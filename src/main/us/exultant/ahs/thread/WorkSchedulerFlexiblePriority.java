/*
 * Copyright 2010 - 2013 Eric Myhre <http://exultant.us>
 *
 * This file is part of AHSlib.
 *
 * AHSlib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License, or
 * (at the original copyright holder's option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package us.exultant.ahs.thread;

import us.exultant.ahs.core.*;
import us.exultant.ahs.util.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class WorkSchedulerFlexiblePriority implements WorkScheduler {
	public WorkSchedulerFlexiblePriority(int $threadCount) {
		$threads = ThreadUtil.wrapAll(new Runnable() {
			public void run() {
				worker_cycle();
			}
		}, $threadCount);
	}

	public WorkScheduler start() {
		$lock.lock();
		try {
			switch ($requestedStat) {
				case NOT_STARTED:
					$requestedStat = RequestedStatus.RUNNING;
					break;
				case RUNNING:
					return this;
				case STOPPING_HARD:
				case STOPPING:
				case STOPPED:
					throw new IllegalStateException("cannot start a scheduler that is already in \""+$requestedStat+"\" state!");
			}
		} finally {
			$lock.unlock();
		}

		ThreadUtil.startAll($threads);

		return this;
	}

	public void stop(boolean $aggressively) {
		$lock.lock();
		// try to transition
		try {
			switch ($requestedStat) {
				case NOT_STARTED:
					$requestedStat = RequestedStatus.STOPPED;
					return;
				case RUNNING:
					$requestedStat = $aggressively ? RequestedStatus.STOPPING_HARD : RequestedStatus.STOPPING;
					$available.signalAll();
					break;
				case STOPPING_HARD:
					break; /* can't stop any harder, and downgrading is not allowed */
				case STOPPING:
					$requestedStat = $aggressively ? RequestedStatus.STOPPING_HARD : RequestedStatus.STOPPING;
					break;
				case STOPPED:
					return;
			}
		} finally {
			$lock.unlock();
		}

		//TODO:AHS:THREAD: this is a total hackjob for the moment being.  We need to do some snazzy stuff with Futures here.
		ThreadUtil.joinAll($threads);
		$lock.lock();
		try {
			$requestedStat = RequestedStatus.STOPPED;
		} finally {
			$lock.unlock();
		}

		//TODO:AHS:THREAD: and you should also be making sure all tasks still somewhere in the scheduler become cancelled.  especially because external cancels after we close this pipe will be quite unpleasant.
		$completed.sink().close();
	}

	@SuppressWarnings("unchecked")
	public <$V> WorkFuture<$V> schedule(WorkTarget<$V> $work, ScheduleParams $when) {
		WorkFutureImpl<$V> $wf = new WorkFutureImpl<$V>(this, $work, $when);
		$completed.sink().write((WorkFuture<Object>)$wf);
		$lock.lock();
		try {
			if ($wf.$sync.scheduler_shiftToScheduled()) {
				$scheduled.add($wf);
			} else {
				if ($when.isUnclocked()) {
					$unready.add($wf);
//					$log.trace(this, "frosh to unready: "+$wf);
				} else {
					$delayed.add($wf);
				}

				if ($work.isDone()) {
					// this check MUST be done AFTER adding the task to a heap, or otherwise it becomes more than slightly dodgy, for all the usual reasons: you could have failed to shift because you just weren't ready, then the done check happens, then you concurrently "finish" from someone else draining your pipe before this scheduler knows to take update requests about you seriously.
					$wf.$sync.tryFinish(false, null, null);		//FIXME:AHS:THREAD: i don't want to be calling completion listeners from inside the scheduler lock!
					hearTaskDrop($wf);
				}
			}
			return $wf;
		} finally {
			$lock.unlock();
		}
	}

	public <$V> void update(WorkFuture<$V> $fut) {
		/* note that this entire method could be replaced by "update(Arr.asList(new WorkFuture<?>[] {$fut}));".
		 * we're going out of our way to avoid creating those garbage objects. */
		if (update_per($fut));
			update_postSignal();
	}
	public <$V> void update(Collection<WorkFuture<$V>> $futs) {
		boolean $areDeferredUpdates = false;
		for (WorkFuture<$V> $fut : $futs)
			$areDeferredUpdates |= update_per($fut);
		if ($areDeferredUpdates) update_postSignal();
	}
	/** makes an immediate attempt to finish a task, and pushes it into a list of things that need to be touched again the next time a worker thread checks in.
	 * @return true if {@link #$updatereq} has new members, which means that {@link #update_postSignal()} must be called soon. */
	private boolean update_per(WorkFuture<?> $fut) {
		// check if it's even remotely possible that it's one of ours, and cast it into something that exposes us the guts we need to pinch
		if (!($fut instanceof WorkFutureImpl)) return false;
		WorkFutureImpl<?> $fui = (WorkFutureImpl<?>)$fut;

		/* // if it already knows itself as complete, we need pay no attention
		* if ($fui.isDone()) return false;
		* This would be a nice idea, but unfortunately it's invalid because we sometimes have to use the update method for the express purpose of making sure completed tasks get kicked out of the unready pile. */

		// check doneness of the work; try to transition immediately to FINISHED if is done.
		if ($fui.$work.isDone()) {
			$fui.$sync.tryFinish(false, null, null);
			/* This is allowed to fail completely if the work is currently running.
			 * Regardless of whether or not we were the ones to cause a finish, since we're currently outside of the scheduler lock,
			 *  we must call update to make sure tasks don't get stuck in waiting pile. */
		}

		// just push this into the set of requested updates.
		return $updatereq.add($fui);
	}
	/** wake a thread that might be blocking because there's no work, because it needs to drain the updatereq list again before it can be sure it should still be waiting */
	private void update_postSignal() {
		$lock.lock();
		try {
			$available.signal();
		} finally {
			$lock.unlock();
		}
	}

	public ReadHead<WorkFuture<Object>> completed() {
		return $completed.source();
	}



	private final Thread[]			$threads;
	private volatile RequestedStatus	$requestedStat	= RequestedStatus.NOT_STARTED;

	private final PriorityHeap		$delayed	= new PriorityHeap(WorkFutureImpl.DelayComparator.INSTANCE);
	private final PriorityHeap		$scheduled	= new PriorityHeap(WorkFutureImpl.PriorityComparator.INSTANCE);
	private final Set<WorkFutureImpl<?>>	$unready	= new HashSet<WorkFutureImpl<?>>();
	private final Set<WorkFutureImpl<?>>	$updatereq	= Collections.newSetFromMap(new ConcurrentHashMap<WorkFutureImpl<?>,Boolean>());	// as long as we run updates strictly after removing something from this, our synchronization demands are quite low.
	private final Map<Thread,Object> 	$running	= new ConcurrentHashMap<Thread,Object>();	// this is purely for bookkeeping/debugging/statusreporting and serves absolutely zero functional purpose out of that.
	private final FuturePipe<Object>	$completed	= new FuturePipe<Object>();

	private final ReentrantLock		$lock		= new ReentrantLock();
	private Thread				$leader		= null;
	private final Condition			$available	= $lock.newCondition();

//	private static final Logger		$log		= new Logger(Logger.LEVEL_TRACE);

	private static enum RequestedStatus { NOT_STARTED, RUNNING, STOPPING, STOPPING_HARD, STOPPED }



	private void worker_cycle() {
		WorkFutureImpl<?> $chosen = null;
		doWork: for (;;) {
			// lock until we can pull someone out who's state is scheduled.
			//    delegate our attention to processing update requests and waiting for delay expirations as necessary.
			$lock.lock();
			try {
				retry: for (;;) {
					switch ($requestedStat) {
						case RUNNING:
							// kay.  carry on.
							break;
						case STOPPING_HARD:
							// don't care if there's work available.  we're leaving.
							$chosen = null;
							break doWork;
						case STOPPING:
							// carry on.  we'll stop in a little bit if worker_acquireWork can't come up with anything to do.
							break;
						default:
							throw new MajorBug("illegal state transition occured ("+$requestedStat+")");
					}
					WorkFutureImpl<?> $wf = worker_acquireWork();	/* this may block. */
					if ($wf == null) break doWork;	/* we either ran out of work or we were shifted to hard-stopping. */
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
				}
				$running.put(Thread.currentThread(), $chosen);
				$chosen.$sync.scheduler_shiftToRunning();
			} finally {
				$lock.unlock();
			}

			// run the work we pulled out.
			boolean $mayRunAgain = $chosen.$sync.scheduler_power();

			// clear the interrupt status of the current thread.  assuming that the interrupt was intended for the work we were in the middle of moments ago, it is no longer valid now that we've exited that work's execution, and it would in inappropriate for it to disrupt the next work or our scheduling.
			Thread.currentThread().interrupted();

			// requeue the work for future attention if necessary
			if ($mayRunAgain) {
				$lock.lock();
				$running.put(Thread.currentThread(), "planning next move");
				try {
					switch ($chosen.$sync.scheduler_shiftPostRun()) {
						case FINISHED:
						case CANCELLED:
							hearTaskDrop($chosen);
							break;
						case SCHEDULED:
							$scheduled.add($chosen);
							break;
						case WAITING:
							if ($chosen.getScheduleParams().isUnclocked())
								$unready.add($chosen);
							else
								$delayed.add($chosen);
							break;
						default:
							throw new MajorBug("running is not a valid state in this branch");
					}
				} finally {
					$lock.unlock();
				}
			} else {
				// the work is completed (either as FINISHED or CANCELLED); we must now drop it.
				hearTaskDrop($chosen);
			}
			//TODO:AHS:THREAD:EFFIC: in the case that you're just looping around, we should actually just hold on to that lock.  it's fine like it is, but holding it will probably be more efficient.
		}
		$running.put(Thread.currentThread(), "shutting down");
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
	 * The lock must be acquired during this entire function, in order to make
	 * possible the correctly atomic shifts to RUNNING or WAITING that may be carried
	 * out immediately following this function.
	 */
	private WorkFutureImpl<?> worker_acquireWork() {
		assert $lock.isHeldByCurrentThread();
		try { for (;;) {
			// offer to shift any tasks that have had updates requested
			worker_pollUpdates();

			// shift any clock-based tasks that need no further delay into the scheduled heap.  note the time until the next of those clocked tasks will be delay-free.
			long $delay = worker_pollDelayed();

			// get work now, if we have any.
			WorkFutureImpl<?> $first = $scheduled.peek();
			if ($first != null) return $scheduled.poll();

			// if we couldn't get any work immediately and stopping is requested, we'll concede.
			if ($requestedStat == RequestedStatus.STOPPING) /* checking for STOPPING_HARD isn't necessary because we wouldn't get here if it was */
				return null;

			// if we don't have any ready work, wait for signal of new work submission or until what we were told would be the next delay expiry; then we just retry.
			$running.put(Thread.currentThread(), "waiting for availability of new work events");
			if ($leader != null) {
				$available.awaitUninterruptibly();
			} else {
				$leader = Thread.currentThread();
				try {
					$available.awaitNanos($delay);	// note that if we had zero delayed work, this is just an obscenely long timeout and no special logic is needed.
				} catch (InterruptedException $e) {
					/* don't rightly reckon I give a damn. */
				} finally {
					if ($leader == Thread.currentThread()) $leader = null;
				}
			}
			if ($requestedStat == RequestedStatus.STOPPING_HARD)
				// don't care if there's work available.  we're leaving.
				return null;
			$running.put(Thread.currentThread(), "planning next move");
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
		assert $lock.isHeldByCurrentThread();
		final Iterator<WorkFutureImpl<?>> $itr = $updatereq.iterator();
		while ($itr.hasNext()) {
			WorkFutureImpl<?> $wf = $itr.next(); $itr.remove();
			if ($wf.$sync.scheduler_shiftToScheduled()) {
				if ($unready.remove($wf))
					$scheduled.add($wf);
				else throw new MajorBug("if a piece of work is being shifted from unready state to scheduled, it should have already been in the unready set!");
			} else {
				if ($wf.$work.isDone())	{
					boolean $causedFinish = $wf.$sync.tryFinish(false, null, null);
					// note that even if this guy is scheduled, we don't chase that object down.  we could.  we don't need to; he'll bubble out eventually.  if he's in unready... i'm not sure he'll escape.
//					$log.trace(this, "isDone "+$wf+" noticed in worker_pollUpdates(); we caused finish "+$causedFinish);
				}
				switch ($wf.getState()) {
					case FINISHED:
					case CANCELLED:
						$unready.remove($wf);
						hearTaskDrop($wf);
						break;
					case SCHEDULED:
						if ($wf.$heapIndex == -1) throw new MajorBug();	//XXX:AHS:THREAD: this check shall remain for a while for doublechecking, but i'm fairly confident it's not necessary.
						$scheduled.siftDown($wf.$heapIndex, $wf);
						$scheduled.siftUp($wf.$heapIndex, $wf);
						break;
					default:
						/* it's still just waiting, leave it there */
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
	 * Hold the friggin' lock when calling, of course. Since we cause WorkFuture
	 * instances to change their state in here, we must enforce that this aligns with
	 * changing the heap they're in so the rest of the scheduler doesn't go insane.
	 *
	 * @return the delay (in nanosec) until the next known clocked task will be ready
	 *         (or Long.MAX_VALUE if there are no more clocked tasks present).
	 */
	private long worker_pollDelayed() {
		assert $lock.isHeldByCurrentThread();
		WorkFutureImpl<?> $key;
		for (;;) {
			$key = $delayed.peek();
			if ($key == null) return Long.MAX_VALUE;	// the caller should just wait indefinitely for notification of new tasks entering the system, because we're empty.
			if (!$key.$sync.scheduler_shiftToScheduled()) return $key.getScheduleParams().getDelay();
			$delayed.poll();	// get it outta there
			$scheduled.add($key);
		}
	}

	protected void hearTaskDrop(WorkFuture<?> $wf) {
//		X.sayet("task dropped!  " + $wf + "\n\t" + X.toString(new Exception()));
	}

	public String describe() {
		$lock.lock();
		String $moar = null;
		int $runningCount = 0;
		try {
			StringBuilder $sb = new StringBuilder();

			$sb.append("\n\t\tRUNNING:");
			if ($running.size() > 0)
				for (Map.Entry<Thread,Object> $thing : $running.entrySet()) {
					$sb.append("\n\t\t\t"+Strings.padRightToWidth($thing.getKey().toString(),40)+" --->   "+$thing.getValue());
					$runningCount += ($thing.getValue() instanceof WorkFuture) ? 1 : 0;
				}
			else
				$sb.append("\n\t\t\t--- none ---");

			$sb.append("\n\t\tSCHEDULED:");
			if ($scheduled.$size > 0)
				for (int $i = 0; $i < $scheduled.$size; $i++)
					$sb.append("\n\t\t\t"+$scheduled.$queue[$i]);
			else
				$sb.append("\n\t\t\t--- none ---");

			$sb.append("\n\t\tUNREADY:");
			if ($unready.size() > 0)
				for (WorkFuture<?> $thing : $unready)
					$sb.append("\n\t\t\t"+$thing);
			else
				$sb.append("\n\t\t\t--- none ---");

			$sb.append("\n\t\tDELAYED:");
			if ($delayed.$size > 0)
				for (int $i = 0; $i < $delayed.$size; $i++)
					$sb.append("\n\t\t\t"+$delayed.$queue[$i]);
			else
				$sb.append("\n\t\t\t--- none ---");

			$sb.append("\n\t\tUPDATEREQ:");
			if ($updatereq.size() > 0)
				for (WorkFuture<?> $thing : $updatereq)
					$sb.append("\n\t\t\t"+$thing);
			else
				$sb.append("\n\t\t\t--- none ---");

			$moar = $sb.toString();
		} finally {
			$lock.unlock();
		}
		return
		"running: "   + Strings.padLeftToWidth($runningCount+"", 5)     + "    " +
		"scheduled: " + Strings.padLeftToWidth($scheduled.$size+"", 5)  + "    " +
		"unready: "   + Strings.padLeftToWidth($unready.size()+"", 5)   + "    " +
		"delayed: "   + Strings.padLeftToWidth($delayed.$size+"", 5)    + "    " +
		"updatereq: " + Strings.padLeftToWidth($updatereq.size()+"", 5) +
		$moar;
	}



	/**
	 * This is a max-heap; nothing too special. We do also store the index of an entry
	 * in the entry itself, though; this enables us to remove entries or resort that
	 * entry quickly.
	 *
	 * When we use it for actual task priorities, the highest priority is of course on
	 * top; when we use it for delays, we use a comparator that sees the nearest times
	 * as the highest.
	 */
	private class PriorityHeap {
		public PriorityHeap(Comparator<WorkFutureImpl<?>> $comparator) {
			this.$comparator = $comparator;
		}

		private static final int		INITIAL_CAPACITY	= 64;
		private WorkFutureImpl<?>[]		$queue			= new WorkFutureImpl<?>[INITIAL_CAPACITY];
		private int				$size			= 0;
		private final Comparator<WorkFutureImpl<?>>	$comparator;

		public WorkFutureImpl<?> peek() {
			return $queue[0];
		}

		/** Returns the first element, replacing the first element with the last and sifting it down. Call only when holding lock. */
		private WorkFutureImpl<?> poll() {
			assert $lock.isHeldByCurrentThread();
			int $s = --$size;
			WorkFutureImpl<?> f = $queue[0];
			WorkFutureImpl<?> x = $queue[$s];
			$queue[$s] = null;
			if ($s != 0) siftDown(0, x);
			f.$heapIndex = -1;
			return f;
		}

		/** Add a new element and immediately sift it to its heap-ordered spot. Call only when holding lock. */
		public boolean add(WorkFutureImpl<?> $newb) {
			assert $lock.isHeldByCurrentThread();
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
		private void siftUp(int $k, WorkFutureImpl<?> $x) {
			assert $lock.isHeldByCurrentThread();
			while ($k > 0) {
				int $parent = ($k - 1) >>> 1;
				WorkFutureImpl<?> $e = $queue[$parent];
				if ($comparator.compare($x, $e) <= 0) break;
				$queue[$k] = $e;
				$e.$heapIndex = $k;
				$k = $parent;
			}
			$queue[$k] = $x;
			$x.$heapIndex = $k;
		}

		/** Sift element added at top down to its heap-ordered spot. Call only when holding lock. */
		private void siftDown(int $k, WorkFutureImpl<?> $x) {
			assert $lock.isHeldByCurrentThread();
			int $half = $size >>> 1;
			while ($k < $half) {
				int $child = ($k << 1) + 1;
				WorkFutureImpl<?> $c = $queue[$child];
				int right = $child + 1;
				if (right < $size && $comparator.compare($c, $queue[right]) <= 0) $c = $queue[$child = right];
				if ($comparator.compare($x, $c) > 0) break;
				$queue[$k] = $c;
				$c.$heapIndex = $k;
				$k = $child;
			}
			$queue[$k] = $x;
			$x.$heapIndex = $k;
		}

		/** Resize the heap array. Call only when holding lock. */
		private void grow() {
			assert $lock.isHeldByCurrentThread();
			int $oldCapacity = $queue.length;
			int $newCapacity = $oldCapacity + ($oldCapacity >> 1); // grow 50%
			if ($newCapacity < 0) // overflow
			$newCapacity = Integer.MAX_VALUE;
			$queue = Arrays.copyOf($queue, $newCapacity);
		}
	}



	/**
	 * <p>
	 * When run, dumps the entire set of tasks known to this WorkScheduler as
	 * "waiting"/"unready" into a queue that requests rechecking and updating of their
	 * status.
	 * </p>
	 *
	 * <p>
	 * Note: this function must acquire a global lock on the entire Scheduler in order
	 * to perform its function, so calling it wantonly is not advised.
	 * </p>
	 *
	 * <p>
	 * This does not request updating or re-sorting of tasks already in the scheduled
	 * heap.
	 * </p>
	 */
	public void flush() {
		$lock.lock();
		try {
			$updatereq.addAll($unready);
		} finally {
			$lock.unlock();
		}
	}
}
