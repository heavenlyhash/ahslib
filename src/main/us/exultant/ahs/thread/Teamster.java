package us.exultant.ahs.thread;

import us.exultant.ahs.core.*;
import java.io.*;

/**
 * <p>
 * Ferries data from a {@link ReadHead} to a {@link WriteHead}, using a thread to perform
 * some translation during the journey.
 * </p>
 * 
 * <p>
 * An example use of this would be to move data from the {@link ReadHead} of a Pipe (in
 * the us.exultant.ahs.thread package) that buffers incomming data from the network into a
 * {@link WriteHead} from a Pipe that buffers incoming messages ready for the application
 * logic to read, while using a {@link Translator} to convert the raw data into messages
 * of an application protocol.
 * </p>
 * 
 * @author hash
 * 
 * @param <$FROM>
 * @param <$TO>
 */
public class Teamster<$FROM, $TO> implements WorkTarget {
	public Teamster(ReadHead<$FROM> $src, Translator<$FROM, $TO> $trans, WriteHead<$TO> $sink, int $priority) {
		this.$trans = $trans;
		this.$src = $src;
		this.$sink = $sink;
		this.$prio = $priority;
	}
	public Teamster(ReadHead<$FROM> $src, Translator<$FROM, $TO> $trans, WriteHead<$TO> $sink) {
		this($src, $trans, $sink, 0);
	}
	
	private final ReadHead<$FROM>		$src;
	private final WriteHead<$TO>		$sink;
	private final Translator<$FROM,$TO>	$trans;
	private ExceptionHandler<IOException>	$eh;
	private final int			$prio;
	
	public boolean isDone() {
		return $src.isClosed() && !$src.hasNext();
	}
	
	public boolean isReady() {
		return $src.hasNext();
	}
	
	/** Priority in Teamster is a constant specified when the Teamster was constructed. */
	public int getPriority() {
		return $prio;
	}
	
	public synchronized void run() {
		if (isDone()) return;
		
		$FROM $a = $src.readNow();
		if ($a == null) return;
		
		$TO $b;
		try {
			$b = $trans.translate($a);
			
			if ($b == null) return;
			
			$sink.write($b);
		} catch (IOException $e) {
			// this error handling is the SAME for both errors in the translator and errors from writing the the sink.
			ExceptionHandler<IOException> $dated_eh = $eh;
			if ($dated_eh != null) $dated_eh.hear($e);
			return;
		}
	}
	
	/**
	 * <p>
	 * In the case of exceptions that occur in the course of a Teamster's operation of
	 * its {@link Translator}, those exceptions are sent to the handler specified by
	 * this method (or otherwise they may be discarded silently if no handler has been
	 * set).
	 * </p>
	 * 
	 * <p>
	 * Exceptions caught by the Teamster that are not IOException are still pushed
	 * through this interface by listing them as the cause of a new IOException that
	 * is then rethrown. Exceptions not caught by the Pump can still bubble out of the
	 * Pump without being pushed through this interface, but no exception should do
	 * both.
	 * </p>
	 * 
	 * <p>
	 * The handler's <code>hear(*)</code> method is invoked by the pumping thread, and
	 * will be executed before the Pump takes any other actions such as attempting to
	 * continue reading.
	 * </p>
	 * 
	 * <p>
	 * If changed while the pump is in motion, no concurrency errors will occur;
	 * however, be advised that the pump may take notice of the change in an extremely
	 * lazy fashion, and indeed if more than one thread uses that pump, different
	 * threads may notice the update at markedly and unpredictably different points in
	 * their relative execution.
	 * </p>
	 * 
	 * @param $eh
	 */
	public void setExceptionHandler(ExceptionHandler<IOException> $eh) {
		this.$eh = $eh;
	}
}
