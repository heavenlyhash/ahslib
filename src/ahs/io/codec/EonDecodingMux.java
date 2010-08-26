package ahs.io.codec;

import ahs.io.*;
import ahs.io.codec.Codec.*;
import ahs.io.codec.eon.*;

import java.util.*;

import mcon.msg.*;
import mcon.msg.MconMessage.*;

/**
 * Use this class to implement polymorphism in codecs by enrolling multiple instantiable
 * classes that share a common interface in this "seed", along with their encoders and
 * decoders. The mux shifts the "MAGICWORD_CLASS" field to the "MAGICWORD_HINT" field upon
 * encoding, and places the common interface's class in the "MAGICWORD_CLASS" field; the
 * process is reversed in decoding -- this means that the mux'd instantiable classes
 * enrolled in the mux must not use the "MAGICWORD_HINT" data field themselves, but otherwise any existing encoders and decoders should be usable transparently..
 * 
 * Calling the enroll(*) functions of this class automatically calls the appropriate
 * putHook(*) methods on the parent codec.
 * 
 * Once this mux is configured, all references to it can be safely discarded -- it will be
 * used internally by the parent codec in a transparent fashion. Simply give the parent
 * codec instances for encoding as normal, and when decoding give it the common interface
 * as the decode target.
 * 
 * @author hash
 * 
 */
public class EonDecodingMux<$FACE> {
	public EonDecodingMux(EonCodec $parent, Class<$FACE> $klass) {
		this.$parent = $parent;
		this.$klass = $klass;
		this.$demux = new HashMap<String,Class<? extends $FACE>>();
		initialize();
	}
	
	private final Class<$FACE>				$klass;
	private final EonCodec					$parent;
	private final Map<String,Class<? extends $FACE>>	$demux;
	
	private void initialize() {
		$parent.putHook($klass, new Dencoder<EonCodec,EonObject,$FACE>() {
			public EonObject encode(EonCodec $codec, $FACE $x) throws TranslationException {
				ahs.util.X.saye("using mux enc");
				EonObject $eo = $parent.encode($x.getClass().cast($x));	// dynamically cast it back as precise as it can get so we don't end up hitting the interface's encode hook infinitely
				$eo.put(Eon.MAGICWORD_HINT, $eo.getKlass());
				$eo.putKlass(EonDecodingMux.this.$klass);
				return $eo;
			}
			
			public $FACE decode(EonCodec $codec, EonObject $eo) throws TranslationException {
				$eo.assertKlass(EonDecodingMux.this.$klass);
				String $hint = $eo.getString(Eon.MAGICWORD_HINT);
				Class<? extends $FACE> $t = $demux.get($hint);
				if ($t == null) throw new TranslationException("Decoding dispatch hook not found for hint \"" + $hint + "\""); 
				$eo.putKlass($hint);
				return $parent.decode($eo, $t);
			}
		});
	}
	
	public <$T extends $FACE> void enroll(Class<$T> $klass, final Encoder<EonCodec,EonObject,$T> $encoder, final Decoder<EonCodec,EonObject,$T> $decoder) {
		$parent.putHook($klass, $encoder);
		$parent.putHook($klass, $decoder);
		$demux.put(Eon.getKlass($klass), $klass);
	}

	public <$T extends $FACE> void enroll(Class<$T> $klass, final Dencoder<EonCodec,EonObject,$T> $dencoder) {
		enroll($klass, (Encoder<EonCodec,EonObject,$T>)$dencoder, (Decoder<EonCodec,EonObject,$T>) $dencoder);
	}
}