package com.hae.pipe;

import java.text.*;
import java.util.*;

public class PipeUtil implements PipeConstants {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public static int TARGET_TYPE_COLUMN = 0;
	public static int TARGET_TYPE_XRANGE = 1;
	public static int TARGET_TYPE_STRING = 2;
	public static int TARGET_TYPE_ANYOF  = 3;

	public static class Location {
		public boolean found = false;
		public int startIndex = -1;
		public int endIndex = -1;
		
		public Location() {
		}
		public Location(int s, int e) {
			found = true;
			startIndex = s;
			endIndex = e;
		}
	}
	
	/**
	 * Given a target that looks something like the following, try to work out
	 * the next match.  Returns -1 if unable to find anything.
	 * 
	 *  ──┬─────┬──┬─xrange─────────────┬──
	 *    └─NOT─┘  └─┬─STRing─┬─dstring─┘
	 *               └─ANYof──┘
	 */
	public static Location locateTarget(String s, int startIndex, int targetType, String dString, char[] xrange, boolean not, int defaultColumn) {
		// default location is NOT FOUND
		Location location = new Location();
		
		if (targetType == TARGET_TYPE_XRANGE) {
			for (int i = startIndex; i < s.length(); i++) {
				char c = s.charAt(i);
				if (not) {
					if (c < xrange[0] || c > xrange[1]) {
						location = new Location(i, i);
						break;
					}
				}
				else {
					if (c >= xrange[0] && c <= xrange[1]) {
						location = new Location(i, i);
						break;
					}
				}
			}
		}
		else if (targetType == TARGET_TYPE_STRING) {
			if (not) {
				// loop over the string looking for the first
				// occurrence where it doesn't match.  eg. if
				// s="abababX" and dString="ab", then it would
				// stop when it gets to X.
				for (int i = 0; i < s.length(); i += dString.length()) { 
					if (!s.substring(startIndex).startsWith(dString, i)) {
						location = new Location(startIndex+i, startIndex+i+dString.length()-1);
						break;
					}
				}
			}
			else {
				int index = s.indexOf(dString, startIndex);
				if (index != -1)
					location = new Location(index, index+dString.length()-1);
			}
		}
		else if (targetType == TARGET_TYPE_ANYOF) {
			for (int i = startIndex; i < s.length(); i++) {
				char c = s.charAt(i);
				if (not) {
					if (dString.indexOf(c) == -1) {
						location = new Location(i, i);
						break;
					}
				}
				else {
					if (dString.indexOf(c) != -1) {
						location = new Location(i, i);
						break;
					}
				}
			}
		}
		else if (targetType == TARGET_TYPE_COLUMN) {
			location = new Location(defaultColumn, defaultColumn);
		}
		return location;
	}
	
	/**
	 * Split a space-delimited string into a list of words
	 */
	public static String[] split(String s) {
		if (s == null || s.equals(""))
			return new String[] {};
		else {
			PipeArgs pa = new PipeArgs(s);
			ArrayList<String> list = new ArrayList<String>();
			String word = pa.nextWord();
			while (!word.equals("")) {
				list.add(word);
				word = pa.nextWord();
			}
			String[] words = new String[list.size()];
			for (int i = 0; i < words.length; i++)
				words[i] = (String)list.get(i);
			return words;
			//return s.split(" ");
		}
	}

	/**
	 * Split a string at the specified column and return an
	 * array that contains two elements: the bit before that
	 * column, and the bit after that column. The index that
	 * gets passed is 1-based and can be negative (which mean
	 * that it is resolved backwards from the end of the string)
	 */
	public static String[] split(String s, int column) {
		if (s == null || s.equals(""))
			return new String[] {"", ""};
		else {
			// first thing to do is make sure that max lengths are honoured
			column = (column >= 0 ? Math.min(column, s.length()) : Math.max(column, -s.length()));
			// now lets calculate the actual offset into the string
			int offset = (column >= 0 ? column : s.length()+column);
			return new String[] { s.substring(0, offset), s.substring(offset) };
		}
	}

	/**
	 * Takes a string in one of the following forms, and converts it
	 * to an int.
	 *   char     - single character
	 *   hexchar  - 2 characters with hexadecimal digits (eg. "4F"  NOT "x'4F'" or "0x4F")
	 *   constant - "BLANK", "SPACE", "TAB" 
	 */
	public static char makeChar(String input) {
		if (!Syntax.isXorC(input))
			throw new IllegalArgumentException(input);
		
		if (input.length() == 1)
			return input.charAt(0);
		else if (input.length() == 2) 
			return (char)Integer.parseInt(input, 16);
		else if (Syntax.abbrev("BLANK", input, 5) || Syntax.abbrev("SPACE", input, 5))
			return ' ';
		else if (Syntax.abbrev("TABULATE", input, 3))
			return '\t';
		else
			throw new IllegalArgumentException(input);
	}

	/**
	 * Takes a string in one of the following forms, and converts it
	 * to an int.
	 *   decstring  - decimal number
	 *   Xhexstring - X followed by (up to 8) hexadecimal digits
	 *   Hhexstring - H followed by (up to 8) hexadecimal digits
	 *   Bbinstring - B followed by (up to 32) binary digits
	 */
	public static int makeInt(String s) throws PipeException {
		try {
			s = s.toUpperCase();
			if (s.charAt(0) == 'X' || s.charAt(0) == 'H')
				return Integer.parseInt(s.substring(1), 16);
			else if (s.charAt(0) == 'B')
				return Integer.parseInt(s.substring(1), 2);
			else
				return Integer.parseInt(s.replace('+',' ').trim());
		}
		catch(NumberFormatException e) {
			throw new PipeException(-58, s);
		}
	}

	/**
	 * Takes a string and converts it to a float
	 */
	public static float makeFloat(String s) {
		return Float.parseFloat(s);
	}
	
	/**
	 * Takes a string and returns two bytes that represent the
	 * starting and ending range for that xrange. See Syntax.XRANGE
	 * for a definition of what a valid range is.
	 */
	public static char[] makeXrange(String s) {
		// if we've been passed something with no - or . then 
		// the range is from itself to itself.  The only catch
		// is if the range is meant to be from ". to ." (or "- to -")
		// see below for special handling...
		if (s.indexOf('.') == -1 && s.indexOf('-') == -1)
			s = s+"-"+s;

		// if the passed value is a single dot or dash, then 
		// let just bail with a special case right now...
		if (s.equals(".") || s.equals("-"))
			return new char[] { s.charAt(0), s.charAt(0) };
		
		String[] parts = (s.equals(".") || s.equals("-")) ? new String[] { s, s } : s.split("[\\.\\-]");
		boolean dot = s.indexOf('.') == -1 ? false : true;
		char c1 = makeChar(parts[0]);
		char c2 = makeChar(parts[1]);
		
		if (dot)
			return new char[] { c1, (char)((int)c1+Integer.parseInt(""+c2)-1) };
		else
			return new char[] { c1, c2 };
	}
	
	/**
	 * Takes a string with a calendar date format and returns a Date object. 
	 * Supported input formats are:
	 *   yymmdd
	 *   yyyymmdd
	 *   yyyymmddhh
	 *   yyyymmddhhmm
	 *   yyyymmddhhmmss
	 */
	private static final SimpleDateFormat DT_YYMMDD         = new SimpleDateFormat("yyMMdd");
	private static final SimpleDateFormat DT_YYYYMMDD       = new SimpleDateFormat("yyyyMMdd");
	private static final SimpleDateFormat DT_YYYYMMDDHH     = new SimpleDateFormat("yyyyMMddHH");
	private static final SimpleDateFormat DT_YYYYMMDDHHMM   = new SimpleDateFormat("yyyyMMddHHmm");
	private static final SimpleDateFormat DT_YYYYMMDDHHMMSS = new SimpleDateFormat("yyyyMMddHHmmss");
	static {
		DT_YYMMDD.setLenient(false);
		DT_YYYYMMDD.setLenient(false);
		DT_YYYYMMDDHH.setLenient(false);
		DT_YYYYMMDDHHMM.setLenient(false);
		DT_YYYYMMDDHHMMSS.setLenient(false);
	}
	public static Date makeDateFromISO(String input) throws PipeException {
		try {
			switch(input.length()) {
			case 6:  return DT_YYMMDD.parse(input);
			case 8:  return DT_YYYYMMDD.parse(input);
			case 10: return DT_YYYYMMDDHH.parse(input);
			case 12: return DT_YYYYMMDDHHMM.parse(input);
			case 14: return DT_YYYYMMDDHHMMSS.parse(input);
			default: throw new PipeException(-1183, input); 
			}
		}
		catch(ParseException e) {
			throw new PipeException(-1183, input); 
		}
	}

	/**
	 * Takes a string with a calendar timestamp format and returns a Date object. 
	 * Supported input formats are:
	 *   yyyy-mm-dd
	 *   yyyy-mm-dd hh
	 *   yyyy-mm-dd hh:mm
	 *   yyyy-mm-dd hh:mm:ss
	 *   yyyy-mm-dd hh:mm:ss.SSS
	 */
	private static final SimpleDateFormat TS_YYYYMMDD             = new SimpleDateFormat("yyyy-MM-dd");
	private static final SimpleDateFormat TS_YYYYMMDDHH           = new SimpleDateFormat("yyyy-MM-dd HH");
	private static final SimpleDateFormat TS_YYYYMMDDHHMM         = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	private static final SimpleDateFormat TS_YYYYMMDDHHMMSS       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static final SimpleDateFormat TS_YYYYMMDDHHMMSSSSS    = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	static {
		TS_YYYYMMDD.setLenient(false);
		TS_YYYYMMDDHH.setLenient(false);
		TS_YYYYMMDDHHMM.setLenient(false);
		TS_YYYYMMDDHHMMSS.setLenient(false);
		TS_YYYYMMDDHHMMSSSSS.setLenient(false);
	}
	public static Date makeTimestampFromISO(String input) throws PipeException {
		try {
			switch(input.length()) {
			case 10: return TS_YYYYMMDD.parse(input);
			case 13: return TS_YYYYMMDDHH.parse(input);
			case 16: return TS_YYYYMMDDHHMM.parse(input);
			case 19: return TS_YYYYMMDDHHMMSS.parse(input);
			case 23: return TS_YYYYMMDDHHMMSSSSS.parse(input);
			default: throw new PipeException(-1183, input); 
			}
		}
		catch(ParseException e) {
			throw new PipeException(-1183, input); 
		}
	}

	/**
	 * Takes a string with a julian date format and returns a Date object.
	 * Supported input formats are:
	 *   yyddds
	 *   ccyyddds
	 *   ccyydddshh
	 *   ccyydddshhmm
	 *   ccyydddshhmmss
	 * Where cc is the number of centuries beyond 1900 (eg. 1=2000, 2=2100)
	 *  and s is the sign (dunno what this is)
	 */ 
	private static final SimpleDateFormat YYDDDS         = new SimpleDateFormat("yyDDD");
	private static final SimpleDateFormat CCYYDDDS       = new SimpleDateFormat("yyyyDDD");
	private static final SimpleDateFormat CCYYDDDSHH     = new SimpleDateFormat("yyyyDDDHH");
	private static final SimpleDateFormat CCYYDDDSHHMM   = new SimpleDateFormat("yyyyDDDHHmm");
	private static final SimpleDateFormat CCYYDDDSHHMMSS = new SimpleDateFormat("yyyyDDDHHmmss");
	static {
		YYDDDS.setLenient(false);
		CCYYDDDS.setLenient(false);
		CCYYDDDSHH.setLenient(false);
		CCYYDDDSHHMM.setLenient(false);
		CCYYDDDSHHMMSS.setLenient(false);
	}
	public static Date makeDateFromJUL(String input) throws PipeException {
		if (input.length() < 6)
			throw new PipeException(-1183, input);
		
		try {
			boolean containsCC = (input.length() != 6);
			int signOffset = containsCC ? 7 : 5;
			String fixedInput = input.substring(0,signOffset)+input.substring(signOffset+1);
			if (containsCC) {
				int cc = Integer.parseInt(fixedInput.substring(0, 2));
				fixedInput = ""+(19+cc)+fixedInput.substring(2);
			}
			switch(input.length()) {
			case 6:  return YYDDDS.parse(fixedInput);
			case 8:  return CCYYDDDS.parse(fixedInput);
			case 10: return CCYYDDDSHH.parse(fixedInput);
			case 12: return CCYYDDDSHHMM.parse(fixedInput);
			case 14: return CCYYDDDSHHMMSS.parse(fixedInput);
			default: throw new PipeException(-1183, input); 
			}
		}
		catch(ParseException e) {
			throw new PipeException(-1183, input); 
		}
	}
	
	/**
	 * Converts a Date object into an ISO formatted string
	 */
	public static String makeISOStringFromDate(Date date) {
		return DT_YYYYMMDDHHMMSS.format(date);
	}
	
	/**
	 * Converts a Date object into an ISO formatted string
	 */
	public static String makeISOTimestampFromDate(Date date) {
		return TS_YYYYMMDDHHMMSSSSS.format(date)+"000";
	}
	
	/**
	 * Converts a Date object into an Julian formatted string
	 */
	public static String makeJULStringFromDate(Date date) {
		return CCYYDDDSHHMMSS.format(date);
	}
	
	/**
	 * Takes a string and a length, and truncates it to that that length.
	 * Returns the original string if the length is greater than the string length;
	 */
	public static String trunc(String s, int length) {
		if (s.length() <= length)
			return s;
		else
			return s.substring(0, length);
	}

	/**
	 * Aligns a string
	 */
	public static String align(String s, int length, int alignment) {
		return align(s, length, alignment, ' ');
	}

	/**
	 * Aligns a string
	 */
	public static String align(String s, int length, int alignment, char pad) {
		// TODO inefficient
		boolean trunc = s.length() > length;
		String padding = "";
		for (int i = 0; i < length-s.length(); i++)
			padding += pad;
		
		if (alignment == RIGHT)
			return trunc ? s.substring(s.length()-length) : padding + s;
		else if (alignment == CENTRE)
			return trunc ? s.substring((s.length()-length)/2, (s.length()-length)/2+length) : (padding.substring(0, (length-s.length())/2) + s + padding).substring(0, length);
		else
			return trunc ? s.substring(0, length) : s + padding;
	}

	/**
	 * Returns a string containing the 8-byte binary representation of the number
	 */
	public static String makeBinLength8(long number) {
		char[] chars = new char[8];
        chars[0] = (char)((number >>> 56) & 0xFF);
        chars[1] = (char)((number >>> 48) & 0xFF);
        chars[2] = (char)((number >>> 40) & 0xFF);
        chars[3] = (char)((number >>> 32) & 0xFF);
        chars[4] = (char)((number >>> 24) & 0xFF);
        chars[5] = (char)((number >>> 16) & 0xFF);
        chars[6] = (char)((number >>>  8) & 0xFF);
        chars[7] = (char)((number >>>  0) & 0xFF);
		return new String(chars);
	}
	
	/**
	 * Returns a string containing the 4-byte binary representation of the number
	 */
	public static String makeBinLength4(int number) {
		char[] chars = new char[4];
        chars[0] = (char)((number >>> 24) & 0xFF);
        chars[1] = (char)((number >>> 16) & 0xFF);
        chars[2] = (char)((number >>>  8) & 0xFF);
        chars[3] = (char)((number >>>  0) & 0xFF);
		return new String(chars);
	}
	
	/**
	 * Returns a string containing the 2-byte binary representation of the number
	 */
	public static String makeBinLength2(int number) {
		char[] chars = new char[2];
        chars[0] = (char)((number >>>  8) & 0xFF);
        chars[1] = (char)((number >>>  0) & 0xFF);
		return new String(chars);
	}

	/**
	 * Returns a number represented by the 8-byte binary string
	 */
	public static long makeStrLength8(String input) throws PipeException {
		if (input.length() != 8)
			throw new PipeException(-352, ""+input.length(), ""+8);
			
		long number = 0;
        number |= ((long)input.charAt(0)) << 56;
        number |= ((long)input.charAt(1)) << 48;
        number |= ((long)input.charAt(2)) << 40;
        number |= ((long)input.charAt(3)) << 32;
        number |= ((long)input.charAt(4)) << 24;
        number |= ((long)input.charAt(5)) << 16;
        number |= ((long)input.charAt(6)) << 8;
        number |= ((long)input.charAt(7));
        return number;
	}

	/**
	 * Returns a number represented by the 4-byte binary string
	 */
	public static int makeStrLength4(String input) throws PipeException {
		if (input.length() != 4)
			throw new PipeException(-352, ""+input.length(), ""+4);
			
		int number = 0;
        number |= input.charAt(0) << 24;
        number |= input.charAt(1) << 16;
        number |= input.charAt(2) << 8;
        number |= input.charAt(3);
		return number;
	}

	/**
	 * Returns a number represented by the 2-byte binary string
	 */
	public static int makeStrLength2(String input) throws PipeException {
		if (input.length() != 2)
			throw new PipeException(-352, ""+input.length(), ""+2);
			
		int number = 0;
        number |= input.charAt(0) << 8;
        number |= input.charAt(1);
		return number;
	}
	
	/**
	 * Inserts a string into another string within a range.
	 */
	public static String insert(String originalString, String insertString, int position, int length, int alignment, char pad) {
		// either pad or truncate the new string to the right length
		insertString = align(insertString, length, alignment, pad);
		String firstBit = align(originalString, position-1, LEFT, pad);
		String secondBit = (position-1+length >= originalString.length()) ? "" : align(originalString, originalString.length()-(position-1+length), RIGHT, pad);
		return firstBit+insertString+secondBit;
	}
}
