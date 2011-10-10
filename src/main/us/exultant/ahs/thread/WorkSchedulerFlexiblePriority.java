package us.exultant.ahs.thread;

import us.exultant.ahs.util.*;
import us.exultant.ahs.log.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class WorkSchedulerFlexiblePriority implements WorkScheduler {
	public WorkSchedulerFlexiblePriority(int $threadCount) {
		Thread[] $threads = ThreadUtil.wrapAll(new Runnable() {
			public void run() {
				for (;;) {
					try {
						worker_cycle();
					} catch (InterruptedException $e) { $e.printStackTrace(); }
				}
			}
		}, $threadCount);
		
		ThreadUtil.startAll($threads);
		
		schedule(new RelentlessGC(), ScheduleParams.makeFixedDelay(2));
	}
	
	public <$V> WorkFuture<$V> schedule(WorkTarget<$V> $work, ScheduleParams $when) {
		WorkFuture<$V> $wf = new WorkFuture<$V>($work, $when);
			//FIXME:AHS:THREAD: this next line won't actually notice if the task that was submitted was already finished (it won't schedule it either, which is good, but not finishing it is still a bug (we wouldn't want someone to wait on the future if it'll never transition to finished, right?)).  (the RelentlessGC will deal with it eventually of course, but it's much better if we notice it immediately -- then we don't even have to lock the scheduler to insert the new task.
		$lock.lock();
		try {
			if ($wf.$sync.scheduler_shift())
				$scheduled.add($wf);
			else
				if ($when.isUnclocked()) {
					$unready.add($wf);
					$log.trace(this, "frosh to unready: "+$wf);
				}
				else
					$delayed.add($wf);
			return $wf;
		} finally {
			$lock.unlock();
		}
	}
	
	public <$V> void update(WorkFuture<$V> $fut) {
		// check doneness; try to transition immediate to FINISHED if is done.
		if ($fut.$work.isDone()) {
			$fut.$sync.tryFinish(false, null, null);	// this is allowed to fail completely if the work is currently running.
			$log.trace(this, "FINAL UPDATE REQUESTED for "+$fut);
		}
		
		// just push this into the set of requested updates.
		$updatereq.add($fut);
	}
	
	
	
	private final PriorityHeap		$delayed	= new PriorityHeap(WorkFuture.DelayComparator.INSTANCE);
	private final PriorityHeap		$scheduled	= new PriorityHeap(WorkFuture.PriorityComparator.INSTANCE);
	private final Set<WorkFuture<?>>	$unready	= new HashSet<WorkFuture<?>>();
	private final Set<WorkFuture<?>>	$updatereq	= Collections.newSetFromMap(new ConcurrentHashMap<WorkFuture<?>,Boolean>());	// as long as we run updates strictly after removing something from this, our synchronization demands are quite low.
	
	private final ReentrantLock		$lock		= new ReentrantLock();
	private Thread				$leader		= null;
	private final Condition			$available	= $lock.newCondition();
	
	private static final Logger		$log		= new Logger(Logger.LEVEL_TRACE);
	
	
	private void worker_cycle() throws InterruptedException {
		WorkFuture<?> $chosen;
		doWork: for (;;) {	// we repeat this loop... well, forever, really.
			
			// lock until we can pull someone out who's state is scheduled.
			//    delegate our attention to processing update requests and waiting for delay expirations as necessary.
			$lock.lockInterruptibly();
			try { retry: for (;;) {
				WorkFuture<?> $wf = worker_acquireWork();	// this may block.
				switch ($wf.getState()) {
					case FINISHED:
						hearTaskDrop($wf);
						continue retry;
					case CANCELLED:
						hearTaskDrop($wf);
						continue retry;
					case SCHEDULED:
						$chosen = $wf;
						break retry;
					default:
						throw new MajorBug("work acquisition turned up a target that had been placed in the scheduled heap, but was neither in a scheduled state nor had undergone any of the valid concurrent transitions.");
				}
			}} finally {
				$lock.unlock();
			}
			
			// run the work we pulled out.
			boolean $requiresMoar = $chosen.$sync.scheduler_power();
			
			// requeue the work for future attention if necessary
			if ($requiresMoar) {	// the work finished into a WAITING state; check it for immediate readiness and put it in the appropriate heap.				
				$lock.lockInterruptibly();
				try {
					if ($chosen.$sync.scheduler_shift()) {
						$scheduled.add($chosen);
					} else {
						if ($chosen.$work.isDone()) {
							boolean $causedFinish = $chosen.$sync.tryFinish(false, null, null);
							$log.trace(this, "isDone "+$chosen+" noticed in worker_cycle() after powering it; we caused finish "+$causedFinish);
							hearTaskDrop($chosen);
							continue doWork;
						}
						// bit of a dodgy spot here: tasks are allowed to become done without notification (i.e. a readhead that was closed long ago but only emptied now due to a concurrent read; a task based on eating from that thing becomes done, but doesn't notice it at all).  And they're allowed to do that at any time after they're in the unready pool, too.  :/
						//    There are two ways of dealing with something like that: having every read from a RH check for doneness, and everyone subscribe to the RH so we could notify for them (which is an INSANE degree of complication to add to the API, and completely useless for a lot of other applications of pipes)... Or, do periodic cleanup.
						// oh, and also someone can do a concurrent cancel before that scheduler_shift attempt, which will bring us here and also leave us with an awkward cancelled task stuck in our unready heap (which isn't as bad as a done-but-not-finished task, since no one can get stuck waiting on it, but is still a garbage problem).
						if ($chosen.getScheduleParams().isUnclocked()) {
							$log.trace(this, "added to unready: "+$chosen);
							$unready.add($chosen);
						} else
							$delayed.add($chosen);
					}
				} finally {
					$lock.unlock();
				}
			} else {
				// the work is completed (either as FINISHED or CANCELLED); we must now drop it.
				hearTaskDrop($chosen);
			}
		}
	}
	
	/**
	 * Try to get WorkFuture from the $scheduled heap, and don't give up. Also always
	 * try to pull clocked work from delayed to scheduled every time around.
	 * 
	 * WorkFuture returned from this method may be either SCHEDULED, CANCELLED, or
	 * FINISHED (they may not be WAITING or RUNNING since those mutations are only
	 * carried out under this scheduler's lock). The WorkTarget of the returned
	 * WorkFuture may return either true or false to both {@link WorkTarget#isDone()}
	 * and {@link WorkTarget#isReady()}, regardless of the WorkFuture's state; the
	 * caller of this function should be aware of that, and either finish and drop the
	 * task or immediately return it to waiting as necessary.
	 * 
	 * The lock much be acquired during this entire function, in order to make
	 * possible the correctly atomic shifts to RUNNING or WAITING that may be carried
	 * out immediately following this function.
	 */
	private WorkFuture<?> worker_acquireWork() throws InterruptedException {
		try { for (;;) {
			// offer to shift any unclocked tasks that have had updates requested
			worker_pollUpdates();
			
			// shift any clock-based tasks that need no further delay into the scheduled heap.  note the time until the next of those clocked tasks will be delay-free.
			long $delay = worker_pollDelayed();
			
			// get work now, if we have any.
			WorkFuture<?> $first = $scheduled.peek();
			if ($first != null) return $scheduled.poll();
			
			// if we don't have any ready work, wait for signal of new work submission or until what we were told would be the next delay expiry; then we just retry.
			if ($leader != null) {
				$available.await();
			} else {
				$leader = Thread.currentThread();
				try {
					$available.awaitNanos($delay);	// note that if we had zero delayed work, this is just an obscenely long timeout and no special logic is needed.
				} finally {
					if ($leader == Thread.currentThread()) $leader = null;
				}
			}
		}} finally {
			if ($leader == null && $scheduled.peek() != null) $available.signal();
		}
	}
	
	/**
	 * Drains the {@link #$updatereq} set and tries to shift those work targets from
	 * the {@link #$unready} heap and into the {@link #$scheduled} heap. This must be
	 * called only while holding the lock.
	 */
	private void worker_pollUpdates() {
		final Iterator<WorkFuture<?>> $itr = $updatereq.iterator();
		while ($itr.hasNext()) {
			WorkFuture<?> $wf = $itr.next(); $itr.remove();
			if ($wf.$sync.scheduler_shift()) {
				if ($unready.remove($wf))
					$scheduled.add($wf);
			} else {
				if ($wf.$work.isDone())	{
					boolean $causedFinish = $wf.$sync.tryFinish(false, null, null);
					$log.trace(this, "isDone "+$wf+" noticed in worker_pollUpdates(); we caused finish "+$causedFinish);
				}
				switch ($wf.getState()) {
					case FINISHED:
					case CANCELLED:
						$unready.remove($wf);
						hearTaskDrop($wf);
					default: // still just waiting, leave it there
						continue;
				}
			}
		}
	}
	
	/**
	 * Pulls clocked work that requires no further delay off of the delayed heap,
	 * pushes it into the scheduled heap, and sifts the shifted tasks as necessary by
	 * priority.
	 * 
	 * Hold the friggin' lock when calling, of course. Since cause WorkFuture's to
	 * change their state in here, we must enforce that this aligns with changing the
	 * heap they're in so the rest of the scheduler doesn't go insane.
	 * 
	 * @return the delay (in nanosec) until the next known clocked task will be ready
	 *         (or Long.MAX_VALUE if there are no more clocked tasks present).
	 */
	private long worker_pollDelayed() {
		WorkFuture<?> $key;
		for (;;) {
			$key = $delayed.peek();
			if ($key == null) return Long.MAX_VALUE;	// the caller should just wait indefinitely for notification of new tasks entering the system, because we're empty.
			if (!$key.$sync.scheduler_shift()) return $key.getScheduleParams().getDelay();
			$delayed.poll();	// get it outta there
			$scheduled.add($key);	
		}
	}
	
	protected void hearTaskDrop(WorkFuture<?> $wf) {
//		X.sayet(X.toString(new Exception()));
	}
	
	
	
	
	private class PriorityHeap {
		public PriorityHeap(Comparator<WorkFuture<?>> $comparator) {
			this.$comparator = $comparator;
		}
		
		private static final int		INITIAL_CAPACITY	= 32;
		private WorkFuture<?>[]			$queue			= new WorkFuture<?>[INITIAL_CAPACITY];
		private int				$size			= 0;
		private final Comparator<WorkFuture<?>>	$comparator;
		
		public WorkFuture<?> peek() {
			return $queue[0];
		}
		
		/** Returns the first element, replacing the first element with the last and sifting it down. Call only when holding lock. */
		private WorkFuture<?> poll() {
			int $s = --$size;
			WorkFuture<?> f = $queue[0];
			WorkFuture<?> x = $queue[$s];
			$queue[$s] = null;
			if ($s != 0) siftDown(0, x);
			f.$heapIndex = -1;
			return f;
		}

		/** Add a new element and immediately sift it to its heap-ordered spot. Call only when holding lock. */
		public boolean add(WorkFuture<?> $newb) {
			if ($newb == null) throw new NullPointerException();
			$lock.lock();
			try {
				int $s = $size;
				if ($s >= $queue.length) grow();
				$size = $s + 1;
				if ($s == 0) {
					$queue[0] = $newb;
					$newb.$heapIndex = 0;
				} else {
					siftUp($s, $newb);
				}
				if ($queue[0] == $newb) {
					$leader = null;
					$available.signal();
				}
			} finally {
				$lock.unlock();
			}
			return true;
		}
		
		/** Sift element added at bottom up to its heap-ordered spot. Call only when holding lock. */
		private void siftUp(int $k, WorkFuture<?> $x) {
			while ($k > 0) {
				int $parent = ($k - 1) >>> 1;
				WorkFuture<?> $e = $queue[$parent];
				if ($comparator.compare($x, $e) >= 0) break;
				$queue[$k] = $e;
				$e.$heapIndex = $k;
				$k = $parent;
			}
			$queue[$k] = $x;
			$x.$heapIndex = $k;
		}
		
		/** Sift element added at top down to its heap-ordered spot. Call only when holding lock. */
		private void siftDown(int $k, WorkFuture<?> $x) {
			int $half = $size >>> 1;
			while ($k < $half) {
				int $child = ($k << 1) + 1;
				WorkFuture<?> $c = $queue[$child];
				int right = $child + 1;
				if (right < $size && $comparator.compare($c, $queue[right]) > 0) $c = $queue[$child = right];
				if ($comparator.compare($x, $c) <= 0) break;
				$queue[$k] = $c;
				$c.$heapIndex = $k;
				$k = $child;
			}
			$queue[$k] = $x;
			$x.$heapIndex = $k;
		}
		
		/** Resize the heap array. Call only when holding lock. */
		private void grow() {
			int $oldCapacity = $queue.length;
			int $newCapacity = $oldCapacity + ($oldCapacity >> 1); // grow 50%
			if ($newCapacity < 0) // overflow
			$newCapacity = Integer.MAX_VALUE;
			$queue = Arrays.copyOf($queue, $newCapacity);
		}
	}
	
	private class RelentlessGC implements WorkTarget<Void> {
		public Void call() throws Exception {
			$lock.lockInterruptibly();
			try {
//				$log.trace(this, "tick.  unready.size="+$unready.size());
				$updatereq.addAll($unready);
			} finally {	
//				$log.trace(this, "tick done.  updatereq.size="+$updatereq.size()+"  unready.size="+$unready.size());
				$lock.unlock();
			}
			return null;
		}

		public boolean isDone() {
			return false;
		}
		
		public boolean isReady() {
			return true;
		}

		public int getPriority() {
			return -1000;
		}
	}
}