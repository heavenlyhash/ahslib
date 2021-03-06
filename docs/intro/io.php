<?php require($_SERVER['DOCUMENT_ROOT']."/style/head.php"); ?>
<?php echo head("exultant.us AHSlib intro io"); ?>
<?php echo theme(); ?>
<link rel='stylesheet' href='../style.css' type='text/css'>
<html id='ahslib-dox'><body class='std'>


<p>
	The <span class=package>ahs.io</span> package represents the most significant part of the <span class=ahsname>AHS</span> library.  It regards not only simple filesystem input/output, but also network communication (both blocking and nonblocking), and efficiently synchronized inter-thread communication -- all with <i>the exact same interface</i> that gives you control of the threading model (in collaboration with the <span class=package>ahs.thread</span> package, which in turn gives you both simple one-line drop-in solutions as well as an interface for customization needs).
</p>

<p>
	Furthermore, the <span class=package>ahs.io package</span> (and in particular, the <span class=package>ahs.io.codec</span> package) concerns itself not just with raw bytes scurrying around, but also with <i>semantic</i> data.  Thus, it contains general purpose tools for rapidly constructing entire stacks of multiplexing message protocols, and serializing data to your choice of either human-readable JSON or an efficient binary scheme (EBON).  At the simplest, one can construct entire protocols by just making classes with the fields desired, annotating them with an interface that defines which fields are to be serializable, and then let the reflective interface do magic for you.  For more complex needs or when the efficiency overhead of reflection is a concern, concise interfaces allow extension to arbitrary encoding and decoding schemes.
</p>

<p>
	The entire system is fastidiously conscious of observing and utilizing type safety at every level -- at no point does the developer have to compromise type safety for generality when using this library.
</p>



<ul>
	<li><h6>Introduction</h6>
	<ul>
		<li><h6>The concept of "semantic data".</h6>
			<p>
				The <span class=ahsname>AHS</span> library is designed around the belief that data should retain semantic meaning as much as possible.  In the situation of IO, that just means things should be objects with fields instead of collections of byte arrays as much as possible. A developer should be able to work with a simple API that allows the sending and receiving of entire chunks of data that represent fully meaningful objects in their own right.  This is a fairly major difference in philosophy from the java.io and java.nio packages, which deal with nothing but primitives and always deal with buffering in terms of piling up bytes thoughtlessly.
			</p>
		</li>
		<li><h6>Translation Stacks</h6>
			<p>
				Semantic data should be able to be translated into other formats of semantic data.  This thought isn't new, and it's essentially a restatement of the concept of protocol stacks.  The <span class=ahsname>AHS</span> library expresses this directly with the <a href='../javadoc/ahs/io/Translator.html'>Translator</a> interface and <a href='../javadoc/ahs/io/TranslatorStack.html'>TranslatorStack</a> class.
			</p>
			<p>
				The idea is simple: a developer can write an implementation of Translator that converts from type A to type B, and other that converts from type B to type C.  Then, use both to construct a new instance of TranslatorStack, and you get something that itself implements Translator from type A to type C.
			</p>
			<p>
				This kind of utility can be useful in all sorts of ways, but it's great in particular for setting up clean and clear general purpose serialization, so translation stacks will pop up again when codecs come in to play and everything comes together.
			</p>
		</li>
		<li><h6>ReadHead and WriteHead</h6>
			<p>
				<a href='../javadoc/ahs/io/ReadHead.html'>ReadHead</a> and <a href='../javadoc/ahs/io/WriteHead.html'>WriteHead</a> are expressions of that central theme of "semantic data", and also expressions of the belief that blocking and nonblocking IO should be interchangeable.  Read their javadocs well.
			</p>
			<p>
				Both accept a single generic type.  This is the "message" type that they deal with; a class or an interface representing an indivisible chunk of semantically meaningful information.  ReadHead and WriteHead instances tend to come in pairs: in the case of pipes, one acts as a sink and the other the source; in the case of network sockets, one is for sending messages to the remote machine and the other is for receiving.
			</p>
			<p>
				ReadHead and WriteHead are meant to support effortless multithreaded usage.  Both specify their behavior under threaded conditions precisely, and implementations allow read and writes to proceed in a well-defined, orderly, efficient fashion from any number of threads.  At any time, ReadHead gives the developer the ability ask for whatever data is immediately available, or ask to have the next piece of data available even if it means waiting, or ask to wait until the underlying data stream (whatever it might be) is closed and then take whatever's left -- in other words, it's possible to make software that chooses to interact with any stream in either a blocking or nonblocking fashion on a per-request basis.  This level of flexibility is totally unprecedented in comparison to any of the options in the java standard library.
			</p>
			<p>
				(Oh, and check this out: <a href='../javadoc/ahs/io/ReadHeadSocketChannel.html'>ahs.io.ReadHeadSocketChannel</a> actually makes accepting new network connections obey the same ReadHead interface as binary network and filesystem IO and everything else.  Abstraction for the win!)
			</p>
		</li>
		<li><h6>Pipes</h6>
			<p>
				<a href='../javadoc/ahs/io/Pipe.html'>ahs.io.Pipe</a> is meant to be a powerful, general purpose tool that might well become one of your favorite helpers for writing multithreaded code.
			</p>
			<p>
				If you're already fairly familiar with writing multithreaded java code, a bit of jargon might explain this more concisely; if you're not, skip this paragraph.  ahs.io.Pipe is kinda like the java library's ConcurrentLinkedQueue, but on steroids.  Pipe adds a semaphore for event-based signaling adding and removing of elements from arbitrary threads, adds a concept of a "closed" pipe/queue, and then ties it all up in the ReadHead and WriteHead interfaces so that other code using the Pipe can choose to interact with it in either a blocking or nonblocking fashion.
			</p>
			<p>
				Pipe can be used as a simple buffer or queue, but it can also function to efficiently shuttle data between arbitrarily massive groups of threads or act as a work queue.  Since it can be operated via the ReadHead interface, it lets the developer ask for whatever data is immediately available, or ask to have the next piece of data available even if it means waiting, or ask to wait until the Pipe is closed and then take whatever's left.  (It pops up in tons of places within the background of the ahs.io library: it does everything from making buffering in translator stacks work for sockets and filesystems uniformly to handling the flow control events in and out of the madness of java.nio.channels.Selector.)
			</p>
		</li>
		<li><h6>Codecs</h6>
			<p>
				Repeat after me: building message protocols should not be hard.
			</p>
			<p>
				Repeat after me: building message protocols should not be hard.
			</p>
			<p>
				Repeat after me: building message protocols should not be hard.
			</p>
			<p>
				Seriously, though.  It shouldn't.  Applications need expressive serialization, and often.  The <span class=ahsname>AHS</span> library is committed to providing the most painless, rapid-development-friendly interfaces for general purpose serialization possible -- and doing it with type safety.
			</p>
			<ul>
				<li><h6>The Big Picture</h6>
					<p>
						The most general form of codecs (the <a href='../javadoc/ahs/io/codec/Codec.html'>ahs.io.codec.Codec</a> interface) is so insanely generic it may make your eyes bleed to look at; honestly, I recommend you ignore it.  In practice, <a href='../javadoc/ahs/io/codec/eon/EonCodec.html'>EonCodec</a> is what you should use, and is what this introduction will talk about.
					</p>
					<p>
						The basic design behind EonCodec isn't complicated: suppose everything can be represented as a map of key-value pairs.  Call that map an <a href='../javadoc/ahs/io/codec/eon/EonObject.html'>EonObject</a> and set it up with getters and setters for all of the primitives, and other EonObjects.  Tack on some other convenience things like a method that puts a string in the map representing the name of the class the map resents.  Now create an <a href='../javadoc/ahs/io/codec/Encoder.html'>Encoder</a> -- it translates a instance of some specific class into an instance of EonObject by just putting its fields into the map.  Create a <a href='../javadoc/ahs/io/codec/Decoder.html'>Decoder</a> that does the reverse.  (And yes, there is an <a href='../javadoc/ahs/io/codec/eon/EonArray.html'>EonArray</a> as well; I'm just glossing over that because it works exactly like you would expect it to.)
					</p>
					<p>
						These Encoder and Decoder implementations then get enrolled together in a Codec.  The Codec hands instances to itself to every encode or decode call, which allows the Encoders and Decoders to use the Codec recursively on every field they want to serialize that isn't a primitive.  Thus, a developer can build Encoder and Decoder objects that have knowledge specific to only one class in good observance of object-oriented paradigms, and turn these into universally effective systems.
					</p>
				</li>
				<li><h6>Switching Encodings</h6>
					<p>
						If you're looking at the source while you read this, you may have already noticed that EonObject is actually just an interface.  It's currently implemented in two forms: <a href='../javadoc/ahs/io/codec/json/JsonObject.html'>JsonObject</a>, and <a href='../javadoc/ahs/io/codec/ebon/EbonObject.html'>EbonObject</a>.  These two implementions are essentially the same except for the way they serialize to bytes; JSON is effectively human-readable, while EBON is a binary format and much more efficient in both output size and decoding time.  These cover the most commonly occurring needs, but of course you're free to implement your own formats as well.  Any EonObject can be translated to a ByteBuffer and back by Eon.TranslatorToByteBuffer and Eon.TranslatorFromByteBuffer respectively.
					</p>
					<p>
						It's possible to switch between which specific kind of EonObject an EonCodec uses within a single line of code -- just switch the factories that you hand to the constructor of EonCodec.  For examples of the difference between an EonCodec backed by the JSON format versus the EBON format, just take a quick glance at the source of <a href='../javadoc/ahs/io/codec/json/JsonCodec.html'>JsonCodec</a> and <a href='../javadoc/ahs/io/codec/ebon/EbonCodec.html'>EbonCodec</a>.  (They both extend EonCodec and are both essentially identical except for the what they hand to the superconstructor.)
					</p>
				</li>
				<li><h6>Easy as Pie</h6>
					<p>
						In practice, it's even easier than all that, because you don't really have to build your own Encoder and Decoder implementations!  Check out the classes <a href='../javadoc/ahs/io/codec/eon/EonRAE.html'>EonRAE</a> and <a href='../javadoc/ahs/io/codec/eon/EonRAD.html'>EonRAD</a>.  (RAE and RAD stand for "reflective annotative [en/de]coder".)  They use -- as the name implies -- java's reflection utilities to be able to automatically transform any class to a representative EonObject and back again.  
					</p>
					<p>
						The developer has control over this via two annotations: <a href='../javadoc/ahs/io/codec/Encodable.html'>Encodable</a> and <a href='../javadoc/ahs/io/codec/Enc.html'>Enc</a>.  Tag a class with Encodable to let EonRAE and EonRAD know they're allowed to operate on it, and then tag the fields that you want to be encoded with the Enc annotation.  Now when you're setting up your codec, just enroll an EonRAE instance and an EonRAD instance for the class tagged with Encodable, just like you would have with any other Encoder and Decoder.  That's it.  (There's also a couple of cool forms of shorthand you can use within those annotations and also some more options for polymorphism, but that's advanced stuff; read the javadoc and the source for more on that.)
					</p>
					<p>
						So all in all, this is actually <i>easier</i> than making a pie -- have you ever tried to get a good homemade meringue that doesn't collapse?
					</p>
				</li>
				<li><h6>Multiplexing and Demultiplexing</h6>
					<p>
						See <a href='../javadoc/ahs/io/codec/eon/EonDecodingMux.html'>EonDecodingMux</a>.  When constructing the mux, specify an interface or superclass that all of the specific message types can be cast to (this is the mux's "face" type).  After that, it's pretty much just like having a codec within a codec: all you have to do is enroll Encoders and Decoders for each class that will be within the mux (and again, it can just be EonRAE and EonRAD across the board), and then enroll the mux in the codec.  There's a little extra magic that goes on in the background, but it's pretty much totally concealed.  You can now just tell the codec to encode any of the muxed objects to the "face" type, and later decode any of them to the same "face" type, and you get polymorphic behavior.  To get the actual types back out, use the standard java instanceof operator or the getClass() method.  Check out <a href='../../src_test/ahs/io/codec/CodecJsonTest.java'>ahs.io.codec.CodecJsonTest#testMuxing()</a>.
					</p>
				</li>
			</ul></li>
		</li>
		<li><h6>Putting it all Together</h6>
			<p>
				EonCodec gives you a tool to translate any java object to an EonObject and back again.  There are Translator classes to convert any EonObject to a ByteBuffer and back again.  The final piece of the puzzle is this: some Translator implementations actually 'translate' ByteBuffer instances onto a ReadableByteChannel, and other 'translate' ByteBuffer instances off of a WritableByteChannel.  That brings us all the way to where java.nio can pick up and run with it down to the OS, and then the wire.
			</p>
			<p>
				Here's an example of this setup in action taken from the MCON project (mcon.net.TransportTcp):
			</p>
<pre>
ReadHead&lt;MconMessage&gt; $src = ReadHeadAdapter.make(
		$socketChannel,
		$pumperSelector,
		TranslatorStack.make(
			new ReadHeadAdapter.Channelwise.BabbleTranslator(),
			new Eon.TranslatorFromByteBuffer(MCON.CODEC),
			MconMessage.TFE
		)
	);
WriteHead&lt;MconMessage&gt;$sink = WriteHeadAdapter.make(
		$socketChannel,
		TranslatorStack.make(
			MconMessage.TTE,
			new Eon.TranslatorToByteBuffer(MCON.CODEC),
			new WriteHeadAdapter.ChannelwiseUnbuffered.BabbleTranslator()
		)
	);
</pre>
			<p>
				$socketChannel is just a java.nio.channels.SocketChannel.  $pumperSelector is an instance of <a href='../javadoc/ahs/util/thread/PumperSelector.html'>ahs.util.thread.PumperSelector</a> (which is essentially just a wrapper (or possibly asylum) around java.nio.channels.Selector; you can make one with the default constructor, start it, and forget about it).
			</p>
			<p>
				What is that TTE and TFE crap, you say?  Glad you asked.  It's just a little shorthand for some translators that the MCON project code shares between all of its connections, since the translators happen to be fully reentrant.  Here they are:
			</p>
<pre>
public static final TranslatorFromEonObject	TFE	= new TranslatorFromEonObject();
public static final TranslatorToEonObject	TTE	= new TranslatorToEonObject();
public static class TranslatorFromEonObject implements Translator&lt;EonObject,MconMessage&gt; {
	public TranslatorFromEonObject() {}
	
	public MconMessage translate(EonObject $eo) throws TranslationException {
		return MCON.CODEC.decode($eo, MconMessage.class);
	}
}
public static class TranslatorToEonObject implements Translator&lt;MconMessage,EonObject&gt; {
	public TranslatorToEonObject() {}
	
	public EonObject translate(MconMessage $eo) throws TranslationException {
		return MCON.CODEC.encode($eo, MconMessage.class);
	}
}
</pre>
			<p>
				And that's it.  As you might have guessed, the MconMessage class is the "face" class of a mux that's enrolled in the EonCodec instance MCON.CODEC, so there's a ton more functionality hidden there in the polymorphic behavior that the mux is providing.  So, if we look at how MCON.CODEC was made...
			</p>
<pre>
public class MconCodec extends ahs.io.codec.eon.EonCodec {
	public static MconCodec makeJsonBasedCodec() {
		return new MconCodec(JsonCodec.OBJPROVIDER,JsonCodec.ARRPROVIDER);
	}
	
	public static MconCodec makeEbonBasedCodec() {
		return new MconCodec(EbonCodec.OBJPROVIDER,EbonCodec.ARRPROVIDER);
	}
	
	protected MconCodec(Factory&lt;? extends EonObject&gt; $objProvider, Factory&lt;? extends EonArray&gt; $arrProvider) {
		super($objProvider, $arrProvider);
		
		try {
			// enroll non-muxed classes in the codec
			// these get used recursively when the muxed Message types are serialized
			putHook(Pseudonym.class,	new EonRAE&lt;Pseudonym&gt;(Pseudonym.class));
			putHook(Pseudonym.class,	new EonRAD&lt;Pseudonym&gt;(Pseudonym.class));
			putHook(ByteVector.class,	ByteVector.ENCODER);
			putHook(ByteVector.class,	ByteVector.DECODER);
			//...
			
			// enroll the muxed message types that are the root of our protocol
			// all of these classes implement the MconMessage interface
			EonDecodingMux&lt;MconMessage&gt; $mux = new EonDecodingMux&lt;MconMessage&gt;(this, MconMessage.class);
			muxDefaults($mux, MessageAnnounce.class);
			muxDefaults($mux, MessageDiscoStart.class);
			muxDefaults($mux, MessageFlood.class);
			muxDefaults($mux, MessageHit.class);
			muxDefaults($mux, MessageInvite.class);
			muxDefaults($mux, MessageKeyGrant.class);
			muxDefaults($mux, MessageLogical.class);
			//...
			
		} catch (UnencodableException $e) {
			throw new MajorBug();
			// this means you screwed up your annotations somewhere.
			// it's a condition that can't be detected at compile time, but 
			//  this will show up the very first time you try to instantiate the codec.
		}
	}
	
	private static &lt;$T extends MconMessage&gt; void muxDefaults(EonDecodingMux&lt;MconMessage&gt; $mux, Class&lt;$T&gt; $klass) throws UnencodableException {
			$mux.enroll($klass, new EonRAE&lt;$T&gt;($klass), new EonRAD&lt;$T&gt;($klass));
	}
	
	//...
</pre>
			<p>
				You can see in those static factory methods at the top of the class how easy it is to switch between the human-readable JSON and the efficient binary EBON formats: really bloody easy.  The rest of the setup is pretty simple too: it's just listing all of the classes that you intend to be able to serialize, and declaring which kind of Encoder and Decoder you want to use for them (note that the ByteVector class has its own Encoder and Decoder -- that's the <a href='../javadoc/ahs/util/ByteVector.html'>ahs.util.ByteVector</a> class if you want to examine that example).
			</p>
			<p>
				To wrap up the examples, let's look at the message classes themselves:
			</p>
<pre>
@Encodable(all_fields=true)
public class MessageFlood extends MconMessage {
	private MessageFlood(Encodable $x) {}
	public MessageFlood() {}
	
	public KeyComp		$hhid;
	public DiscoFloodMess	$blob;
	public int		$scop;
	public KeyComp		$tid;
}
</pre>
			<p>
				...Yep, that's it.  Pretty short and sweet, eh?  You can also have private fields and do the whole getter/setter pattern thing; it makes no difference to the workings of the reflection used in EonRAE and EonRAD.  Here's another equivalent way to express the same thing:
			</p>
<pre>
@Encodable()
public class MessageFlood extends MconMessage {
	private MessageFlood(Encodable $x) {}
	public MessageFlood() {}
	
	@Enc public KeyComp		$hhid;
	@Enc public DiscoFloodMess	$blob;
	@Enc public int			$scop;
	@Enc public KeyComp		$tid;
	public String			$dontSerializeMe;
}
</pre>
			<p>
				This example just skips the "all_fields" shorthand in the first example, and uses the Enc annotation to declare individual fields for serialization -- notice how you can use this to serialize only some of the fields in a class.
			</p>
			<p>
				In summary... after making a simple TranslatorStack and a Codec with a muxing component with the tool in this package, you can build entire protocols effortlessly by just laying out the fields in your messages.  Blar!  At the same time, all of the interfaces used for encoding and decoding can be extended in any arbitrary way you can imagine, and still be compatible with the easy reflective tools and all of the muxing tools.
			</p>
		</li>
	</ul></li>
	<li><h6>Caveats</h6>
	<ul>
		<li><p>
			A codec instance is essentially thread-safe for encoding and decoding.  It is -not- thread safe when it comes to adding Encoders and Decoders; do that setup ahead of time.  Also, since a codec keeps only a single instance of the enrolled Encoder or Decoder, it is possible for a non-thread-safe instance to be enrolled, which in effect may make the entire codec non-thread-safe if those Encoders or Decoders are used... so don't do that.  Encoders and Decoders should be re-entrant (or at least VERY noisily documented if they are not).
		</p></li>
		<li><p>
			I haven't bothered to detect or deal with cyclic datastructures when it comes to codecs.  It's coming someday, but it's an extremely low priority.  In the meantime, if you have a structure that contains cyclic reference when in memory, you can easily jump around this by just not tagging one of the fields in the pair that's creating the cyclic reference with the ENC interface, and any of the reflective-annotative encoding tools will skip it; either that or write your own Encoder implementor.
		</p></li>
	</ul></li>
</ul>


</body></html>
<?php echo foot(); ?>
