package us.exultant.ahs.io;

public interface Translator<$FROM, $TO> {
	public $TO translate($FROM $x) throws TranslationException;
}