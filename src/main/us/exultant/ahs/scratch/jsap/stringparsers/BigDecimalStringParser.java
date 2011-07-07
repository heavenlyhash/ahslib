/*
 * Copyright (c) 2002-2004, Martian Software, Inc.
 * This file is made available under the LGPL as described in the accompanying
 * LICENSE.TXT file.
 */

package us.exultant.ahs.scratch.jsap.stringparsers;

import us.exultant.ahs.scratch.jsap.StringParser;
import us.exultant.ahs.scratch.jsap.ParseException;
import java.math.BigDecimal;

/**
 * A {@link us.exultant.ahs.scratch.jsap.StringParser} for parsing BigDecimals.  The parse() method delegates the
 * actual
 * parsing to BigDecimal's constructor.
 * @author <a href="http://www.martiansoftware.com/contact.html">Marty Lamb</a>
 * @see us.exultant.ahs.scratch.jsap.StringParser
 * @see java.math.BigDecimal
 */
public class BigDecimalStringParser extends StringParser {

	private static final BigDecimalStringParser INSTANCE = new BigDecimalStringParser();

	/** Returns a {@link BigDecimalStringParser}.
	 *
	 * <p>Convenient access to the only instance returned by
	 * this method is available through
	 * {@link us.exultant.ahs.scratch.jsap.JSAP#BIGDECIMAL_PARSER}.
	 *  
	 * @return a {@link BigDecimalStringParser}.
	 */
	
    public static BigDecimalStringParser getParser() {
		return INSTANCE; 
	}

	/**
     * Creates a new BigDecimalStringParser.
     * @deprecated Use {@link #getParser()} or, even better, {@link us.exultant.ahs.scratch.jsap.JSAP#BIGDECIMAL_PARSER}.
     */
    @Deprecated
    public BigDecimalStringParser() {
        super();
    }

    /**
     * Parses the specified argument into a BigDecimal.  This method simply
     * delegates
     * the parsing to <code>new BigDecimal(String)</code>.  If BigDecimal
     * throws a
     * NumberFormatException, it is encapsulated into a ParseException and
     * re-thrown.
     *
     * @param arg the argument to parse
     * @return a BigDecimal object with the value contained in the specified
     * argument.
     * @throws ParseException if <code>new BigDecimal(arg)</code> throws a
     * NumberFormatException.
     * @see BigDecimal
     * @see us.exultant.ahs.scratch.jsap.StringParser#parse(String)
     */
    public Object parse(String arg) throws ParseException {
        BigDecimal result = null;
        try {
            result = new BigDecimal(arg);
        } catch (NumberFormatException e) {
            throw (
                new ParseException(
                    "Unable to convert '" + arg + "' to a BigDecimal.",
                    e));
        }
        return (result);
    }
}