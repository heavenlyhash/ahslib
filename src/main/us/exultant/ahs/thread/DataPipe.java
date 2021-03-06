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

/**
 * <p>
 * This class behaves almost exactly as per ConcurrentLinkedQueue (and is implemented
 * using it), but also provides additional semantics that allow one to determine at least
 * how many entries are in the queue as a constant time operation, and also provides a
 * blocking get functionality. This allows easy use as a work distributor, since one can
 * make get calls that will not return null simply because work is not instantaneously
 * available. Additionally, this class provides the ability to notify a {@link Listener}
 * whenever the pipe has a change in state nonblocking readers might wish to know of (i.e.
 * when it recieves new information, is closed, or becomes empty).
 * </p>
 *
 * <p>
 * Requests that are blocking in nature are given fair ordering; nonblocking requests
 * disregard this entirely.
 * </p>
 *
 * <p>
 * This Pipe will not accept nulls, and its WriteHead will throw a NullPointerException in
 * response to any attempt to write nulls.
 * </p>
 *
 * @author Eric Myhre <tt>hash@exultant.us</tt>
 *
 */
public final class DataPipe<$T> implements Pipe<$T> {
	/**
	 * Constructs a new, open, active, empty, usable Pipe.
	 */
	public DataPipe() {
		$queue = new ConcurrentLinkedQueue<$T>();
		$gate = new ClosableSemaphore(true);
		SRC = new Source();
		SINK = new Sink();
		$lock = new ReentrantLock();
	}

	/**
	 * <p>
	 * The source from which one reads data from the pipe.
	 * </p>
	 */
	public final Source			SRC;

	/**
	 * <p>
	 * The sink to which one writes data into the pipe.
	 * </p>
	 */
	public final Sink			SINK;

	/**
	 * This Listener is triggered for every completed write operation and for close
	 * operations.
	 */
	private volatile Listener<ReadHead<$T>>	$el;

	/**
	 * This is the data-containing buffer itself.
	 */
	private final ConcurrentLinkedQueue<$T>	$queue;

	/**
	 * The write lock.
	 */
	public final Lock			$lock;

	/**
	 * <p>
	 * We always update this semaphore so that it's a <i>minimal</i> value &mdash;
	 * that is, permits are not released until <i>after</i> an insertion into the
	 * queue completes, and permits are acquired <i>before</i> any read from the queue
	 * is attempted. Because of this policy, the semaphore may sometimes severely
	 * underestimate the available work in order to provide this service with
	 * guaranteeable correctness, since reading the amount of available work from this
	 * semaphore is an operation that is never blocked. (Operations that simply effect
	 * the head or tail of the queue are not likely to lead to drastic varation in the
	 * number of permits available to the semphore).
	 * </p>
	 *
	 * <p>
	 * When write is locked, the number of permits in this semaphore cannot grow (but
	 * it can still shrink, because the semaphore is used to synchronize and order
	 * read requests and there is no actual direct read lock). Thus, if write is
	 * locked and all permits are then drained, an effcetive read locked is attained.
	 * </p>
	 */
	private final ClosableSemaphore	$gate;

	/**
	 * @return {@link #SRC}.
	 */
	public Source source() {
		return SRC;
	}

	/**
	 * @return {@link #SINK}.
	 */
	public Sink sink() {
		return SINK;
	}

	/**
	 * The minimal amount of work immediately available (see the documentation of {@link #$gate} for more details).
	 *
	 * @return the minimal amount of work immediately available.
	 */
	public int size() {
		return $gate.availablePermits();
	}



	/**
	 * {@link DataPipe}'s internal implementation of ReadHead.
	 *
	 * @author Eric Myhre <tt>hash@exultant.us</tt>
	 *
	 */
	public final class Source implements ReadHead<$T> {
		private Source() {} /* this should be a singleton per instance of the enclosing class */

		/**
		 * <p>
		 * Sets the Listener that will be triggered for completed write operations
		 * on the matching {@link Sink} and upon close and exhaustion.
		 * </p>
		 *
		 * <p>
		 * Note that this Listener MAY NOT under any circumstances throw an
		 * exception. If it does so, it will NOT be propagated outside of the
		 * Pipe, since the concurrent nature of this interface makes it
		 * unreasonable to try to choose a relevant stack to propagate such an
		 * exception up through.
		 * </p>
		 */
		public void setListener(Listener<ReadHead<$T>> $el) {
			DataPipe.this.$el = $el;

			// it's possible that there wasn't a listener before this, and we need to make sure we fire an event now in case there aren't any more writes forthcoming for a while (if indeed ever).
			// this can be "spurious", since it doesn't actually come as news of a write, but it's terribly important not to ignore this.
			boolean $mustSpur = false;
			lockWrite();
			if (SRC.hasNext()) $mustSpur = true;
			unlockWrite();	/* we prefer to release locks before we let the listener go on a tear, just as a matter of best/simplest practice. */
			if ($mustSpur) invokeListener(DataPipe.this.$el);
		}

		public $T read() {
			if (isClosed()) {
				return readNow();
			} else {
				try {
					if (!$gate.acquire()) return null;
				} catch (InterruptedException $e) {
					return null;
				}
				$T $v = $queue.remove();
				checkForFinale();
				return $v;
			}
		}

		public $T readNow() {
			boolean $one = $gate.tryAcquire();
			checkForFinale();
			if (!$one) return null;
			return $queue.poll();
		}

		public $T readSoon(long $timeout, TimeUnit $unit) {
			boolean $one;
			try {
				$one = $gate.tryAcquire($timeout, $unit);
			} catch (InterruptedException $e) {
				return null;
			}
			checkForFinale();
			if (!$one) return null;
			return $queue.poll();
		}

		public boolean hasNext() {
			return $gate.availablePermits() > 0;
		}

		public List<$T> readAll() throws InterruptedException {
			waitForClose();
			return readAllNow();
		}

		/**
		 * {@inheritDoc}
		 */
		public List<$T> readAllNow() {
			lockWrite();
			try {
				int $p = $gate.drainPermits();
				if ($p == 0) {
					return Collections.emptyList();
				} else {
					List<$T> $v = new ArrayList<$T>($p);
					for (int $i = 0; $i < $p; $i++)
						$v.add($queue.poll());
					return $v;
				}
			} finally {
				unlockWrite();
				checkForFinale();
			}
		}

		public boolean isClosed() {
			return $gate.isClosed();
		}

		/**
		 * Closes the pipe. No more data will be allowed to be written to this
		 * Pipe's WriteHead. Any blocked {@link DataPipe.Source#readAll()} will return
		 * whatever data they can, and other blocked {@link DataPipe.Source#read()}
		 * will immediately return null following this call even if they cannot
		 * get data (these two processes still happen in fair order). All
		 * subsequent reads (blocking or nonblocking) will either return data
		 * immediately as long as any is still buffered in the pipe, and then
		 * forevermore immediately return null once all buffered data is depleted.
		 */
		public void close() {
			lockWrite();
			try {
				$gate.close();
			} finally {
				unlockWrite();
			}
			X.notifyAll($gate); // trigger the return of any final readAll calls

			// give our listener a chance to notice our closure.
			invokeListener($el);
		}

		private void waitForClose() throws InterruptedException {
			synchronized ($gate) {
				while (!isClosed())
					$gate.wait();
			}
		}

		public boolean isExhausted() {
			return $gate.isPermanentlyEmpty();
		}
	}

	public final class Sink implements WriteHead<$T> {
		private Sink() {} // this should be a singleton per instance of the enclosing class

		/**
		 * Writes a single chunk of data to this Sink. After the data is commited,
		 * a permit is released and the data becomes available for reading from
		 * the {@link Source}, and the pipe's listener is then notified of the
		 * event (note: the listener is called using the writing thread).
		 *
		 * @throws IllegalStateException
		 *                 if this Pipe is closed.
		 * @throws NullPointerException
		 *                 if the chunk is null
		 */
		public void write($T $chunk) throws IllegalStateException {
			/* it's not actually necessary to check for NullPointerException ourselves;
			 * it'll be thrown by $queue.add anyway, and we don't care to waste time checking that twice just to avoid a lock, because if it explodes, then who cares if locking was a waste that time? */
			boolean $mustSpur = false;
			lockWrite();
			try {
				if (isClosed()) throw new IllegalStateException("Pipe has been closed.");
				$queue.add($chunk);
				$gate.release();	/* this can't return false because gate closure involves locking, and we're locked right now.  and that's essentially why the write-lock exists (otherwise we'd have to go dredge back through the queue if this did return false, and that would be... mucky). */
				$mustSpur = true;	/* just by virtue of we didn't throw exception before now */
			} finally {
				unlockWrite();
				if ($mustSpur) invokeListener($el);
			}
		}

		/**
		 * <p>
		 * Writes every element of the given collection to this Sink. All elements
		 * added as a group in this way are guaranteed to come out of the Pipe in
		 * the same order as the original ordering of the collection, and shall
		 * not be intermingled with other objects. Some elements added as a group
		 * in this way may be read and cause notifications to the pipe's listener
		 * before the entire group is added (so it may be possible for a thread
		 * reading from a pipe in a non-blocking fashion to read half of the group
		 * of elements, then get nulls, and then later return to see the other
		 * half of the group).
		 * </p>
		 *
		 * <p>
		 * Even if a {@link NullPointerException} is thrown, partial progress may
		 * have been made &mdash; chunks preceeding the cause of the exception
		 * have already been written to the Pipe, and permits for those chunks are
		 * released before the exception bubbles out.
		 * </p>
		 *
		 * @throws IllegalStateException
		 *                 if this Pipe is closed.
		 * @throws NullPointerException
		 *                 if any chunk in the collection is null
		 */
		public void writeAll(Collection<? extends $T> $chunks) {
			int $writes = 0;
			lockWrite();
			try {
				for ($T $chunk : $chunks) {
					$queue.add($chunk);	/* can't use addAll method because we have to count what happens. */
					$writes++;
				}
			} finally {
				if ($writes > 0) $gate.release($writes);
				unlockWrite();
				if ($writes > 0) invokeListener($el);
			}
		}

		/**
		 * Returns true. Pipe doesn't implement any capacity restrictions, so this
		 * isn't really ever in question. isClosed is technically an unrelated
		 * question.
		 *
		 * @return true
		 */
		public boolean hasRoom() {
			return true;
		}

		public boolean isClosed() {
			return $gate.isClosed();
		}

		/**
		 * Closes the pipe. No more data will be allowed to be written to this
		 * Pipe's WriteHead. Any blocked {@link DataPipe.Source#readAll()} will return
		 * whatever data they can, and other blocked {@link DataPipe.Source#read()}
		 * will immediately return null following this call even if they cannot
		 * get data (these two processes still happen in fair order). All
		 * subsequent reads (blocking or nonblocking) will either return data
		 * immediately as long as any is still buffered in the pipe, and then
		 * forevermore immediately return null once all buffered data is depleted.
		 */
		public void close() {
			SRC.close();
		}
	}

	public void close() {
		SRC.close();
	}

	private final void lockWrite() {
		$lock.lock();
	}

	private final void unlockWrite() {
		$lock.unlock();
	}

	private final void invokeListener(Listener<ReadHead<$T>> $dated_el) {
		if ($dated_el != null)
			try {
				$dated_el.hear(SRC);
			} catch (Throwable $t) {
				/* listeners aren't allowed to throw these! */
				throw new MajorBug("utterly unreasonable exception occurred!",$t);
			}
	}



	private void checkForFinale() {
		if ($gate.isPermanentlyEmpty()) {
			// give our listener a chance to notice our final drain.
			// set our listener to null at the same time.  this prevents possibly awkward loops if one tries to readAllNow() in a listener before checking isExhausted().
			Listener<ReadHead<$T>> $del = $el;
			$el = null;	/* we can't do this set after invoking the listener because it could potentially cause the whole loop we're trying to block here. */
			invokeListener($del);
		}
	}
}
