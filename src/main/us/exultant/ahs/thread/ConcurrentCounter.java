package us.exultant.ahs.thread;

import us.exultant.ahs.core.*;
import us.exultant.ahs.util.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * <p>
 * Acts as essentially as a Map&lt;Whatever,AtomicInteger&gt;.
 * </p>
 * 
 * <p>
 * ConcurrentCounter can only be constructed with either a pre-defined list of elements or
 * an actual Enum-based type for efficiency reasons (allowing arbitrarily-timed online
 * additon of new elements would require significantly greater synchronization overhead on
 * all operations).
 * </p>
 * 
 * <p>
 * If highly concerned with efficiency and using the Enum-based constructor is not an
 * option, consider combining this class with the {@link Intern} system to reduce the
 * strain of equality checks.
 * </p>
 * 
 * @author hash
 * 
 */
public abstract class ConcurrentCounter<$T> implements Listener<$T> {
	public static class EnumBased<$E extends Enum<$E>> extends ConcurrentCounter<$E> {
		public EnumBased(Class<$E> $enumType) {
			super(new EnumMap<$E, AtomicInteger>($enumType));
			for ($E $e : $enumType.getEnumConstants())
				$map.put($e, new AtomicInteger());	
		}
	}
	public static class Flexible<$E extends Enum<$E>> extends ConcurrentCounter<$E> {
		public Flexible(Collection<? extends $E> $elements) {
			super(new HashMap<$E, AtomicInteger>());
			for ($E $e : $elements)
				$map.put($e, new AtomicInteger());
		}
	}
	protected ConcurrentCounter(Map<$T,AtomicInteger> $map) {
		this.$map = $map;
	}
	
	protected final Map<$T,AtomicInteger>	$map;	// this would have to be volatile if we didn't have the pre-defined elements criteria to prevent index modification and resizing.
							
	/**
	 * This can be called from any thread, and will internally require no
	 * synchronization on anything but the counter for the given element. By the time
	 * this method returns, the count of the given object will have been incremented.
	 */
	public void hear($T $f) {
		$map.get($f).incrementAndGet();
	}
	
	/**
	 * This can be called from any thread, since the reading of a single value is an
	 * inherently atomic operation.
	 */
	public int getCount($T $f) {
		return $map.get($f).get();
	}
}