/*
 * Copyright (c) 2002-2004, Martian Software, Inc.
 * This file is made available under the LGPL as described in the accompanying
 * LICENSE.TXT file.
 */

package ahs.util.jsap.stringparsers;

import ahs.util.jsap.StringParser;
import ahs.util.jsap.ParseException;

/**
 * A {@link ahs.util.jsap.StringParser} for parsing Floats.  The <code>parse()</code> method delegates
 * the actual
 * parsing to <code>new Float(String)</code>.  If <code>new Float(String)</code>
 * throws a NumberFormatException, it
 * is encapsulated in a ParseException and re-thrown.
 *
 * @author <a href="http://www.martiansoftware.com/contact.html">Marty Lamb</a>
 * @see ahs.util.jsap.StringParser
 * @see java.lang.Float
 */
public class FloatStringParser extends StringParser {

	private static final FloatStringParser INSTANCE = new FloatStringParser();	

	/** Returns a {@link FloatStringParser}.
	 * 
	 * <p>Convenient access to the only instance returned by
	 * this method is available through
	 * {@link ahs.util.jsap.JSAP#FLOAT_PARSER}.
	 *  
	 * @return a {@link FloatStringParser}.
	 */

    public static FloatStringParser getParser() {
		return INSTANCE;
	}

	/**
     * Creates a new FloatStringParser.
     * @deprecated Use {@link #getParser()} or, even better, {@link ahs.util.jsap.JSAP#FLOAT_PARSER}.
     */
    public FloatStringParser() {
        super();
    }

    /**
     * Parses the specified argument into a Float.  This method delegates the
     * actual
     * parsing to new Float(String).  If new Float(String) throws a
     * NumberFormatException, it
     * is encapsulated in a ParseException and re-thrown.
     *
     * @param arg the argument to parse
     * @return a Float object with the value contained in the specified
     * argument.
     * @throws ParseException if <code>new Float(arg)</code> throws a
     * NumberFormatException.
     * @see java.lang.Float
     * @see ahs.util.jsap.StringParser#parse(String)
     */
    public Object parse(String arg) throws ParseException {
        Float result = null;
        try {
            result = new Float(arg);
        } catch (NumberFormatException e) {
            throw (
                new ParseException(
                    "Unable to convert '" + arg + "' to a Float.",
                    e));
        }
        return (result);
    }
}
