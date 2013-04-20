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

import java.util.concurrent.*;

/**
 * <p>
 * Used to specify timing parameters for scheduling tasks that are repeated or delayed
 * based on wall-clock time.
 * </p>
 * 
 * <p>
 * A task can be classed as {@code delayed}, {@code fixed-rate}, {@code fixed-delay}, or
 * {@code unclocked}. The first three categories are all "clock-based"; the last is
 * (obviously) "unclocked".
 * </p>
 * 
 * <p>
 * Unclocked tasks are scheduled by a {@link WorkScheduler} whenever the
 * {@link WorkTarget}'s {@link WorkTarget#isReady()} method allows, and will be repeated
 * until {@link WorkTarget#isDone()} becomes true, or the WorkTarget throws an exception,
 * or the correlated WorkFuture is cancelled. Clocked tasks follow different rules: the
 * WorkScheduleParam partially overrides and supplements those status methods of the
 * WorkTarget.
 * <ul>
 * <li>Delayed tasks will never be scheduled for running before their delay expires. When
 * their delay expires, they will be scheduled exactly once (regardless of their
 * {@link WorkTarget#isReady()} state at that time (this will be checked before
 * {@link WorkFuture.State} is flipped from {@link WorkFuture.State#SCHEDULED} to
 * {@link WorkFuture.State#RUNNING})). After that, they may be treated the same as an
 * unclocked task and scheduled repeatedly until their {@link WorkTarget#isDone()} method
 * returns true, or the WorkTarget throws an exception, or the correlated WorkFuture is
 * cancelled.
 * <li>Fixed-rate tasks may be scheduled with an initial delay (which behaves exactly the
 * same as a delayed task), and after that are set to a new delay that is the current time
 * after execution plus the specified period. The task is rescheduled in this fashion
 * until the {@link WorkTarget#isDone()} method returns true, or the WorkTarget throws an
 * exception, or the correlated WorkFuture is cancelled. (This is similar to the
 * description of
 * {@link ScheduledExecutorService#scheduleWithFixedDelay(Runnable,long,long,TimeUnit)}.)
 * <li>Fixed-delay tasks are the same as fixed-rate tasks, but when they are set to a new
 * delay after execution it is related only to their original scheduling time, and is not
 * impacted by how much time was spent in execution. (This is similar to
 * {@link ScheduledExecutorService#scheduleAtFixedRate(Runnable,long,long,TimeUnit)}.)
 * </ul>
 * </p>
 * 
 * <p>
 * Delays are always computed when the WorkScheduleParams object is created (and
 * thereafter for recurring tasks, when an execution finishes).
 * </p>
 * 
 * @author Eric Myhre <tt>hash@exultant.us</tt>
 * 
 */
public final class ScheduleParams {
	/** This is a singleton that represents any schedule that is {@code unclocked}. */
	public static final ScheduleParams	NOW	= new ScheduleParams(0, 0); // i can't decide whether to call this just "NOW" or "UNCLOCKED".  "ALWAYS" also seems borderline valid (which is what bothers me about "NOW" -- it doesn't capture the essense of recurrability).
											
	public static ScheduleParams makeNow() {
		return NOW;
	}
	
	public static ScheduleParams makeDelayed(long $delayMs) {
		return makeDelayed($delayMs, TimeUnit.MILLISECONDS);
	}
	
	public static ScheduleParams makeFixedRate(long $periodMs) {
		return makeFixedRate(0, $periodMs, TimeUnit.MILLISECONDS);
	}
	
	public static ScheduleParams makeFixedDelay(long $periodMs) {
		return makeFixedDelay(0, $periodMs, TimeUnit.MILLISECONDS);
	}
	
	public static ScheduleParams makeFixedRate(long $initialDelayMs, long $periodMs) {
		return makeFixedRate($initialDelayMs, $periodMs, TimeUnit.MILLISECONDS);
	}
	
	public static ScheduleParams makeFixedDelay(long $initialDelayMs, long $periodMs) {
		return makeFixedDelay($initialDelayMs, $periodMs, TimeUnit.MILLISECONDS);
	}
	
	public static ScheduleParams makeDelayed(long $delay, TimeUnit $unit) {
		return makeDelayed($delay, $unit, false);
	}
	
	public static ScheduleParams makeFixedRate(long $period, TimeUnit $unit) {
		return makeFixedRate(0, $period, $unit, false);
	}
	
	public static ScheduleParams makeFixedDelay(long $period, TimeUnit $unit) {
		return makeFixedDelay(0, $period, $unit, false);
	}
	
	public static ScheduleParams makeFixedRate(long $initialDelay, long $period, TimeUnit $unit) {
		return makeFixedRate($initialDelay, $period, $unit, false);
	}
	
	public static ScheduleParams makeFixedDelay(long $initialDelay, long $period, TimeUnit $unit) {
		return makeFixedDelay($initialDelay, $period, $unit, false);
	}
	
	public static ScheduleParams makeDelayed(long $startTime, TimeUnit $unit, boolean $startTimeIsAbsolute) {
		return new ScheduleParams(
				($startTimeIsAbsolute ? 0 : System.nanoTime()) + $unit.toNanos($startTime),
				0
		);
	}
	
	public static ScheduleParams makeFixedRate(long $startTime, long $period, TimeUnit $unit, boolean $startTimeIsAbsolute) {
		return new ScheduleParams(
				($startTimeIsAbsolute ? 0 : System.nanoTime()) + $unit.toNanos($startTime),
				$unit.toNanos($period)
		);
	}
	
	public static ScheduleParams makeFixedDelay(long $startTime, long $period, TimeUnit $unit, boolean $startTimeIsAbsolute) {
		return new ScheduleParams(
				($startTimeIsAbsolute ? 0 : System.nanoTime()) + $unit.toNanos($startTime),
				-$unit.toNanos($period)
		);
	}
		
	
	private ScheduleParams(long $ns, long $period) {
		this.$time = $ns;
		this.$period = $period;
	}
	
	
	
	/** The time the task is enabled to execute in nanoTime units */
	private long		$time;
	
	/**
	 * Period in nanoseconds for repeating tasks. A positive value indicates
	 * fixed-rate execution; a negative value indicates fixed-delay execution; a value
	 * of 0 indicates a non-repeating task.
	 */
	private final long	$period;
	
	/**
	 * Returns true if this scheduling request is not clock-based.
	 * 
	 * @return true if unclocked
	 */
	public boolean isUnclocked() {
		return $time == 0;
	}
	
	/**
	 * Returns true if this is a periodic (not a one-shot) scheduling request.
	 * 
	 * @return true if periodic
	 */
	public boolean isPeriodic() {
		return $period != 0;
	}
	
	/**
	 * Returns true if this is a fixed-rate action (as opposed to fixed-delay).
	 * 
	 * @return true if fixed-rate
	 */
	public boolean isFixedRate() {
		return $period > 0;
	}
	
	/**
	 * Returns the amount of time in nanoseconds before this action should become schedulable (or
	 * negative if it already is).
	 * 
	 * @return the amount of time before this action should become schedulable.
	 */
	public long getDelay() {
		//if (isUnclocked()) return 0;	// i heard that getting synchronized nanotime can actually be a surprisingly heavy cost for jvms from a talk a jvm engineer for azul systems gave at a google conference.  either way, i like this being something consistent for unclocked tasks instead of being some arbitrary massively negative number.	// but then this is stupid.  there's no place i can imagine calling this without already having branched in the calling function on isClocked.
		return ($time - System.nanoTime());
	}
	
	/**
	 * @return the time (in nanoTime units) when this task is no longer delayed.
	 */
	public long getNextRunTime() {
		return $time;
	}
	
	

	/**
	 * Sets the next time to run for a periodic task. {@link WorkScheduler}
	 * implementations call this after they finish running a periodic task.
	 */
	void setNextRunTime() {
		if ($time == 0) return; // this is immutable, silly.	// we could also just let this go, or we could have a subclass that stubs the method.  really it makes no difference -- putting this check in trades one JNE instruction in x86 assembly for sanity, whoopdeedoo.
		if ($period > 0) $time += $period;
		else $time = System.nanoTime() + -$period;
	}
}
