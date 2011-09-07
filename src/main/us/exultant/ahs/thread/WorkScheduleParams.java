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
 * {@link ScheduledThreadPoolExecutor#scheduleWithFixedDelay(Runnable,long,long,TimeUnit)}
 * .)
 * <li>Fixed-delay tasks are the same as fixed-rate tasks, but when they are set to a new
 * delay after execution it is related only to their original scheduling time, and is not
 * impacted by how much time was spent in execution. (This is similar to
 * {@link ScheduledThreadPoolExecutor#scheduleAtFixedRate(Runnable,long,long,TimeUnit)}.)
 * </ul>
 * </p>
 * 
 * <p>
 * Delays are always computed when the WorkScheduleParams object is created (and
 * thereafter for recurring tasks, when an execution finishes).
 * </p>
 * 
 * @author hash
 * 
 */
public final class WorkScheduleParams {
	/** This is a singleton that represents any schedule that is {@code unclocked}. */
	public static final WorkScheduleParams	NOW	= new WorkScheduleParams(0, 0); // i can't decide whether to call this just "NOW" or "UNCLOCKED".  "ALWAYS" also seems borderline valid (which is what bothers me about "NOW" -- it doesn't capture the essense of recurrability).
											
	public static WorkScheduleParams makeNow() {
		return NOW;
	}
	
	public static WorkScheduleParams makeDelayed(long $ns) {
		return new WorkScheduleParams($ns, 0);
	}
	
	public static WorkScheduleParams makeFixedRate(long $ns, long $period) {
		return new WorkScheduleParams($ns, $period);
	}
	
	public static WorkScheduleParams makeFixedDelay(long $ns, long $period) {
		return new WorkScheduleParams($ns, -$period);
	}
	
	private WorkScheduleParams(long $ns, long $period) {
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
	 * Sets the next time to run for a periodic task. {@link WorkScheduler}
	 * implementations call this after they finish running a periodic task &mdash it
	 * is public so that it can be accessed by WorkScheduler implementations not in
	 * the AHS library package, but it should not generally be used by client code.
	 */
	public void setNextRunTime() {
		if ($time == 0) return; // this is immutable, silly.	// we could also just let this go, or we could have a subclass that stubs the method.  really it makes no difference -- putting this check in trades one JNE instruction in x86 assembly for sanity, whoopdeedoo.
		if ($period > 0) $time += $period;
		else $time = System.nanoTime() + -$period;
		// DL's ScheduledThreadPoolExecutor has some fiddly bits here with a triggerTime and overflowFree function... but I don't really see why.  The problems don't show up unless you're scheduling tasks something like 22,900 TERAYEARS in the future, and at that point I think it's frankly clear that you're asking for trouble. 
	}
}
