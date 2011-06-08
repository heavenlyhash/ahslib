package ahs.applet;

import ahs.applet.command.*;

public class DomContact {
	public DomContact(Exposure $exposure) {
		$power = $exposure;
	}
	
	/**
	 * <p>
	 * Prepares the JsCommand for execution by providing it with direct exposure to
	 * the javascript evaluator, then runs it.
	 * </p>
	 * 
	 * <p>
	 * Typical usage of DomContact might look something like this:
	 * 
	 * <pre>
	 * DomContact $domContact = new DomContact(new DomContactJso($applet));
	 * ... 
	 * 
	 * JsCommand&lt;Bees&gt; $jsc = new FuckWitThisPropCmd(...);
	 * $domContact.execute($jsc);
	 * Bees $answer = $jsc.get();
	 * </pre>
	 * 
	 * </p>
	 * 
	 * <p>
	 * Be aware that this method is NOT synchronized. Individual DOM accesses via an
	 * Exposure should always be atomic, but when two JsCommand are run simultaneously
	 * it is not guaranteed which order they will recieve answers from the Exposure,
	 * and if they make multiple calls to the Exposure or chain invocations to other
	 * JsCommand, there is no guarantee of order or atomicity until they enforce it by
	 * explicitly synchronizing on the Exposure object themselves.
	 * </p>
	 */
	public void execute(JsCommand<?> $jsc) {
		$jsc.run($power);
	}
	
	
	
	private Exposure $power;
	
	/**
	 * Every DomContact implementor should also have a nested class that implements
	 * Exposure. It is recommended that a singleton instance of Exposure be
	 * maintained, and be private to the DomContact. The DomContact can then give a
	 * pointer to the Exposure to JsCommands during their execution; this prevents
	 * leakage of references to the Exposure instance except when a JsCommand chains
	 * invocations to other JsCommands (which is fine) or when a JsCommand keeps a
	 * pointer to the Exposure and returns it later (which is... inadvisable and ought
	 * not be done).
	 */// i might be doing a noob here.  Exposure might be the only thing that needs to be an interface; after that DomContact can pretty much be a final class.  we'll see.
	public static interface Exposure {
		/** This method should perform its operation atomically. */
		public Object eval(String... $strs);	// i still don't really know what's going on with this Object return type, to be honest, but it's what JSO does so i'm going with it until it bites me.
	}
}