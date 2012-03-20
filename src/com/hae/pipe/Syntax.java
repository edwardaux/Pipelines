package com.hae.pipe;

public class Syntax implements PipeConstants {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private static final String LETTER      = "[a-zA-Z]";
	private static final String CHAR        = "[^ ]";    // anything, except a blank
	private static final String DIGIT       = "[0-9]";
	private static final String HEX         = "[0-9a-fA-F]";
	private static final String BIN         = "[01]";
	private static final String NUM         = "("+DIGIT+"+)";
	private static final String SNUM        = "([\\-\\+]{0,1}"+NUM+")";
	private static final String DECIMAL     = "("+NUM+"(\\."+NUM+")?)";
	private static final String STRING      = "(.*)";
	private static final String HEXSTRING   = "[xX]("+HEX+"{2})*";
	private static final String BINSTRING   = "[bB]("+BIN+"{8})*";
	private static final String STAR        = "([\\*]{1})";
	private static final String NUMORSTAR   = "("+NUM+"|"+STAR+")";
	private static final String SNUMORSTAR  = "("+SNUM+"|"+STAR+")";
	private static final String DASH_RANGE  = "("+NUMORSTAR+"-"+NUMORSTAR+")";
	private static final String DOT_RANGE   = "("+NUMORSTAR+"\\."+NUMORSTAR+")";
	private static final String SEMI_RANGE  = "("+SNUM+"|("+SNUM+";"+SNUM+"))";
	private static final String RANGE       = "("+NUM+"|"+DASH_RANGE+"|"+DOT_RANGE+")";
	private static final String RANGES      = "(\\("+RANGE+"([ ]+"+RANGE+")*\\))";
	private static final String XORC        = "("+CHAR+"|"+HEX+"{2}|(?i)BLANK|SPACE|TAB[ulate]{0,5})";
	private static final String DASH_XRANGE = "("+XORC+"-"+XORC+")";
	private static final String DOT_XRANGE  = "("+XORC+"\\."+NUM+")";
	private static final String XRANGE      = "("+XORC+"|"+DASH_XRANGE+"|"+DOT_XRANGE+")";
	private static final String STREAM_ID   = "("+LETTER+"{1,4})";
	private static final String STREAM      = "("+NUM+"|"+STREAM_ID+")";
	private static final String QUAD        = "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
	private static final String IPADDRESS   = "(("+QUAD+"\\.){3}"+QUAD+")";
	private static final String LABEL       = "("+STRING+":)";

	public static boolean isString(String input) {
		return input.matches(STRING);
	}
	public static boolean isHexString(String input) {
		return input.matches(HEXSTRING);
	}
	public static boolean isBinString(String input) {
		return input.matches(BINSTRING);
	}
	public static boolean isDelimitedString(String input) {
		if (input.length() == 0)
			return false;
		String delim = input.substring(0, 1);
		return isBinString(input) | isHexString(input) | input.matches(delim+STRING+delim);
	}
	public static boolean isNumber(String input) {
		return input.matches(NUM);
	}
	public static boolean isSignedNumber(String input) {
		return input.matches(SNUM);
	}
	public static boolean isDecimalNumber(String input) {
		return input.matches(DECIMAL);
	}
	public static boolean isNumberOrStar(String input, boolean allowNegative) {
		return input.matches(allowNegative ? SNUMORSTAR : NUMORSTAR);
	}
	public static boolean isRange(String input) {
		return input.matches(RANGE);
	}
	public static boolean isSignedRange(String input) {
		return input.matches(SEMI_RANGE);
	}
	public static boolean isRanges(String input) {
		return input.matches(RANGE+"|"+RANGES);
	}
	public static boolean isXorC(String input) {
		return input.matches(XORC);
	}
	public static boolean isXrange(String input) {
		return input.matches(XRANGE);
	}
	public static boolean isStreamId(String input) {
		return input.matches(STREAM_ID);
	}
	public static boolean isStream(String input) {
		return input.matches(STREAM);
	}
	public static boolean isIPAddress(String input) {
		return input.matches(IPADDRESS);
	}
	public static boolean isLabel(String input) {
		return input.matches(LABEL);
	}

	/**
	 * Takes a parameter, and checks (with a specified minimum number
	 * of chars) whether it matches the specified input
	 */
	public static boolean abbrev(String longString, String shortString, int minChars) {
		return abbrev(longString, shortString, minChars, true);
	}
	
	/**
	 * Takes a parameter, and checks (with a specified minimum number
	 * of chars) whether it matches the specified input
	 */
	public static boolean abbrev(String longString, String shortString, int minChars, boolean anycase) {
		// if the input doesn't meet the minimum number of chars, 
		// then we bail out immediately
		if (shortString.length() < minChars)
			return false;
		if (shortString.length() > longString.length())
			return false;

		int count = Math.min(shortString.length(), longString.length());
		for (int i = 0; i < count; i++) {
			if (!anycase && shortString.charAt(i) != longString.charAt(i))
				return false;
			if (anycase && Character.toUpperCase(shortString.charAt(i)) != Character.toUpperCase(longString.charAt(i)))
				return false;
		}
		return true;
	}

	/**
	 * Takes a source string and checks whether it starts with the target 
	 * (depending on the case sensitivity flag.)
	 */
	public static boolean startsWith(String source, String target, boolean anycase) {
		if (!anycase)
			return source.startsWith(target);
		else
			return source.toUpperCase().startsWith(target.toUpperCase());
	}


	/**
	 * Verifies whether a valid direction (BOTH, INPUT, OUTPUT) is passed
	 */
	public static boolean isValidDirection(int direction) {
		return (direction == BOTH || direction == INPUT || direction == OUTPUT);
	}
	
	/**
	 * Verifies whether a valid stream type (CURRENT, ANY, ALL) is passed
	 */
	public static boolean isValidStreamType(int streamType) {
		return (streamType == CURRENT || streamType == ANY || streamType == ALL);
	}
	
	/**
	 * Converts a string version of a direction to an int
	 */
	public static int asDirection(String direction) throws PipeException {
		if (Syntax.abbrev("INPUT", direction, 2))
			return INPUT;
		else if (Syntax.abbrev("OUTPUT", direction, 3))
			return OUTPUT;
		else
			throw new PipeException(-100, direction);
	}
	
	public static int extractStreamNo(String stream, int direction, Stage stage) throws PipeException {
		if (Syntax.isStream(stream) && Syntax.isNumber(stream))
			return Integer.parseInt(stream);
		else if (Syntax.isStreamId(stream)) {
			int streamNo = (direction == INPUT ? stage.getInputStreamNo(stream) : stage.getOutputStreamNo(stream));
			if (streamNo == -1)
				throw new PipeException(-103, stream);
			return streamNo;
		}
		else if (stream.equals("*")) {
			int streamNo = (direction == INPUT ? stage.getSelectedInputStreamNo() : stage.getSelectedOutputStreamNo());
			return streamNo;
		}
		else {
			throw new PipeException(-165, stream);
		}
	}
	
	public static Range getRange(PipeArgs pa, boolean required) throws PipeException {
		try {
			pa.mark();
			if (pa.getRemainder().trim().startsWith("(")) 
				return new RangeMultiple(pa);
			else {
				return new RangeSingle(pa);
			}
		}
		catch(PipeException e) {
			if (required || e.getMessageNo() == -200 || e.getMessageNo() == -55)
				throw e;
			else {
				pa.resetMark();
				return null;
			}
		}
	}

}
