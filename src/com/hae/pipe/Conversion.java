package com.hae.pipe;

import java.util.*;

/**
 * Convert from one data type to another.  Types are:
 *   D - Signed (negative sign optional) decimal. eg "-123"
 *   X - Hexadecimal number (even number of digits). eg. "0AF1"
 *   B - Bit String. eg "00110010"
 *   F - Signed floating point number.  eg. 1.234
 *   V - Prefixed field length (2 bytes length prefix). eg. "\u0000\u0004abcd"
 *   P - Packed decimal. eg -1.234
 *   I - ISO Date format. eg [yy]yymmdd[hh[mm[ss]]]
 *   T - ISO Timestamp format. eg YYYY[-MM[-DD[Thh[:mm[:ss[.s]TZD]]]]]]] 
 *   C - Internal binary representation
 */
public abstract class Conversion implements PipeConstants {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public static Conversion create(String conversion) throws PipeException {
		conversion = conversion.toUpperCase();
		// DIRECT CONVERSIONS
		if      ("C2D".equals(conversion))
			return new C2D();
		else if ("C2X".equals(conversion))
			return new C2X();
		else if ("C2B".equals(conversion))
			return new C2B();
		else if ("C2F".equals(conversion))
			return new C2F();
		else if ("C2V".equals(conversion))
			return new C2V();
		else if ("C2P".equals(conversion))
			return new C2P();
		else if ("C2I".equals(conversion))
			return new C2I();
		else if ("C2T".equals(conversion))
			return new C2T();
		else if ("D2C".equals(conversion))
			return new D2C();
		else if ("X2C".equals(conversion))
			return new X2C();
		else if ("B2C".equals(conversion))
			return new B2C();
		else if ("F2C".equals(conversion))
			return new F2C();
		else if ("V2C".equals(conversion))
			return new V2C();
		else if ("P2C".equals(conversion))
			return new P2C();
		else if ("I2C".equals(conversion))
			return new I2C();
		else if ("T2C".equals(conversion))
			return new T2C();
		// COMPOSITE CONVERSIONS
		else if ("D2X".equals(conversion))
			return new D2X();
		else if ("D2B".equals(conversion))
			return new D2B();
		else if ("X2D".equals(conversion))
			return new X2D();
		else if ("X2B".equals(conversion))
			return new X2B();
		else if ("X2F".equals(conversion))
			return new X2F();
		else if ("X2V".equals(conversion))
			return new X2V();
		else if ("X2P".equals(conversion))
			return new X2P();
		else if ("X2I".equals(conversion))
			return new X2I();
		else if ("X2T".equals(conversion))
			return new X2T();
		else if ("B2D".equals(conversion))
			return new B2D();
		else if ("B2X".equals(conversion))
			return new B2X();
		else if ("B2F".equals(conversion))
			return new B2F();
		else if ("B2V".equals(conversion))
			return new B2V();
		else if ("B2P".equals(conversion))
			return new B2P();
		else if ("B2I".equals(conversion))
			return new B2I();
		else if ("B2T".equals(conversion))
			return new B2T();
		else if ("F2X".equals(conversion))
			return new F2X();
		else if ("F2B".equals(conversion))
			return new F2B();
		else if ("V2X".equals(conversion))
			return new V2X();
		else if ("V2B".equals(conversion))
			return new V2B();
		else if ("P2X".equals(conversion))
			return new P2X();
		else if ("P2B".equals(conversion))
			return new P2B();
		else if ("I2X".equals(conversion))
			return new I2X();
		else if ("I2B".equals(conversion))
			return new I2B();
		else if ("T2X".equals(conversion))
			return new T2X();
		else if ("T2B".equals(conversion))
			return new T2B();
		else
			throw new PipeException(-391, conversion);
	}
	
	public abstract String convert(String input, int recno) throws PipeException;

	private static class C2D extends Conversion { public String convert(String input, int recno) throws PipeException { return C2D(input, recno); } }
	private static class C2B extends Conversion { public String convert(String input, int recno) throws PipeException { return C2B(input, recno); } }
	private static class C2F extends Conversion { public String convert(String input, int recno) throws PipeException { return C2F(input, recno); } }
	private static class C2V extends Conversion { public String convert(String input, int recno) throws PipeException { return C2V(input, recno); } }
	private static class C2P extends Conversion { public String convert(String input, int recno) throws PipeException { return C2P(input, recno); } }
	private static class C2I extends Conversion { public String convert(String input, int recno) throws PipeException { return C2I(input, recno); } }
	private static class C2T extends Conversion { public String convert(String input, int recno) throws PipeException { return C2T(input, recno); } }
	private static class C2X extends Conversion { public String convert(String input, int recno) throws PipeException { return C2X(input, recno); } }
	private static class D2C extends Conversion { public String convert(String input, int recno) throws PipeException { return D2C(input, recno); } }
	private static class B2C extends Conversion { public String convert(String input, int recno) throws PipeException { return B2C(input, recno); } }
	private static class F2C extends Conversion { public String convert(String input, int recno) throws PipeException { return F2C(input, recno); } }
	private static class V2C extends Conversion { public String convert(String input, int recno) throws PipeException { return V2C(input, recno); } }
	private static class P2C extends Conversion { public String convert(String input, int recno) throws PipeException { return P2C(input, recno); } }
	private static class I2C extends Conversion { public String convert(String input, int recno) throws PipeException { return I2C(input, recno); } }
	private static class T2C extends Conversion { public String convert(String input, int recno) throws PipeException { return T2C(input, recno); } }
	private static class X2C extends Conversion { public String convert(String input, int recno) throws PipeException { return X2C(input, recno); } }
	
	private static class D2X extends Conversion { public String convert(String input, int recno) throws PipeException { return D2C(C2X(input, recno), recno); } }
	private static class D2B extends Conversion { public String convert(String input, int recno) throws PipeException { return D2C(C2B(input, recno), recno); } }
	private static class X2D extends Conversion { public String convert(String input, int recno) throws PipeException { return X2C(C2D(input, recno), recno); } }
	private static class X2B extends Conversion { public String convert(String input, int recno) throws PipeException { return X2C(C2B(input, recno), recno); } }
	private static class X2F extends Conversion { public String convert(String input, int recno) throws PipeException { return X2C(C2F(input, recno), recno); } }
	private static class X2V extends Conversion { public String convert(String input, int recno) throws PipeException { return X2C(C2V(input, recno), recno); } }
	private static class X2P extends Conversion { public String convert(String input, int recno) throws PipeException { return X2C(C2P(input, recno), recno); } }
	private static class X2I extends Conversion { public String convert(String input, int recno) throws PipeException { return X2C(C2I(input, recno), recno); } }
	private static class X2T extends Conversion { public String convert(String input, int recno) throws PipeException { return X2C(C2T(input, recno), recno); } }
	private static class B2D extends Conversion { public String convert(String input, int recno) throws PipeException { return B2C(C2D(input, recno), recno); } }
	private static class B2X extends Conversion { public String convert(String input, int recno) throws PipeException { return B2C(C2X(input, recno), recno); } }
	private static class B2F extends Conversion { public String convert(String input, int recno) throws PipeException { return B2C(C2F(input, recno), recno); } }
	private static class B2V extends Conversion { public String convert(String input, int recno) throws PipeException { return B2C(C2V(input, recno), recno); } }
	private static class B2P extends Conversion { public String convert(String input, int recno) throws PipeException { return B2C(C2P(input, recno), recno); } }
	private static class B2I extends Conversion { public String convert(String input, int recno) throws PipeException { return B2C(C2I(input, recno), recno); } }
	private static class B2T extends Conversion { public String convert(String input, int recno) throws PipeException { return B2C(C2T(input, recno), recno); } }
	private static class F2X extends Conversion { public String convert(String input, int recno) throws PipeException { return F2C(C2X(input, recno), recno); } }
	private static class F2B extends Conversion { public String convert(String input, int recno) throws PipeException { return F2C(C2B(input, recno), recno); } }
	private static class V2X extends Conversion { public String convert(String input, int recno) throws PipeException { return V2C(C2X(input, recno), recno); } }
	private static class V2B extends Conversion { public String convert(String input, int recno) throws PipeException { return V2C(C2B(input, recno), recno); } }
	private static class P2X extends Conversion { public String convert(String input, int recno) throws PipeException { return P2C(C2X(input, recno), recno); } }
	private static class P2B extends Conversion { public String convert(String input, int recno) throws PipeException { return P2C(C2B(input, recno), recno); } }
	private static class I2X extends Conversion { public String convert(String input, int recno) throws PipeException { return I2C(C2X(input, recno), recno); } }
	private static class I2B extends Conversion { public String convert(String input, int recno) throws PipeException { return I2C(C2B(input, recno), recno); } }
	private static class T2X extends Conversion { public String convert(String input, int recno) throws PipeException { return T2C(C2X(input, recno), recno); } }
	private static class T2B extends Conversion { public String convert(String input, int recno) throws PipeException { return T2C(C2B(input, recno), recno); } }

	/**
	 * Converts a string containing a number into an internal value
	 *   Input:  "123".    Must fit into a 4-byte signed integer.
	 *   Output: "\u0000\u0000\u0000\u007B"  4 byte string
	 */ 
	private static String D2C(String input, int recno) throws PipeException {
		try {
			int number = Integer.parseInt(input);
			return PipeUtil.makeBinLength4(number);
		}
		catch(NumberFormatException e) {
			throw new PipeException(-392, "D2C", String.valueOf(recno), "16", input);
		}
	}
	/**
	 * Converts a hex string into a normal string.
	 *   Input:  "6162".  Unlimited in length.
	 *   Output: "ab"
	 */
	private static String X2C(String input, int recno) throws PipeException {
		// remove all blanks
		input = input.replace(" ", "");
		
		if (input.length() % 2 != 0)
			throw new PipeException(-392, "X2C", String.valueOf(recno), "28", input);
		
		try {
			StringBuffer sb = new StringBuffer(input.length() / 2);
			for (int i = 0; i < input.length(); i += 2) {
				char c = (char)Integer.parseInt(input.substring(i, i+2), 16);
				sb.append(c);
			}
			return sb.toString();
		}
		catch(NumberFormatException e) {
			throw new PipeException(-392, "X2C", String.valueOf(recno), "32", input);
		}
	}
	/**
	 * Converts a binary digit string into a normal string.
	 *   Input:  "0110000101100010".  Unlimited in length.
	 *   Output: "ab"
	 */
	private static String B2C(String input, int recno) throws PipeException {
		if (input.length() % 8 != 0)
			throw new PipeException(-392, "B2C", String.valueOf(recno), "36", input);
		
		try {
			StringBuffer sb = new StringBuffer(input.length() / 8);
			for (int i = 0; i < input.length(); i += 8) {
				char c = (char)Integer.parseInt(input.substring(i, i+8), 2);
				sb.append(c);
			}
			return sb.toString();
		}
		catch(NumberFormatException e) {
			throw new PipeException(-392, "B2C", String.valueOf(recno), "40", input);
		}
	}
	/**
	 * Converts a string containing a floating point number into an internal representation
	 *   Input:  "123.45".    Must fit into a 8-byte double
	 *   Output: "\u0040\u005e\u00dc\u00cc\u00cc\u00cc\u00cc\u00cd"  8 byte string
	 */ 
	private static String F2C(String input, int recno) throws PipeException {
		try {
			double d = Double.parseDouble(input);
			if (Double.isInfinite(d) || Double.isNaN(d))
				throw new PipeException(-392, "F2C", String.valueOf(recno), "12", input);
			
			long number = Double.doubleToLongBits(d);
			return PipeUtil.makeBinLength8(number);
		}
		catch(NumberFormatException e) {
			throw new PipeException(-392, "F2C", String.valueOf(recno), "8", input);
		}
	}
	/**
	 * Converts a string into a length-prefixed string
	 *   Input:  "abcdefg"
	 *   Output: "\u0000\u0007abcdefg"
	 */ 
	private static String V2C(String input, int recno) throws PipeException {
		if (input.length() > 65535)
			throw new PipeException(-392, "V2C", String.valueOf(recno), "48", input);
		
		return PipeUtil.makeBinLength2(input.length())+input;
	}
	private static String P2C(String input, int recno) throws PipeException {
		// TODO P2C
		throw new PipeException(-391, "P2C");
	}
	private static String I2C(String input, int recno) throws PipeException {
		long time = PipeUtil.makeDateFromISO(input).getTime();
		return PipeUtil.makeBinLength8(time);
	}
	private static String T2C(String input, int recno) throws PipeException {
		long time = PipeUtil.makeTimestampFromISO(input).getTime();
		return PipeUtil.makeBinLength8(time);
	}
	/**
	 * Converts a 4-byte string into a signed integer
	 *   Input:  "\u0000\u0000\u0000\u007B"
	 *   Output: "123"
	 */ 
	private static String C2D(String input, int recno) throws PipeException {
		if (input.length() != 4)
			throw new PipeException(-392, "C2D", String.valueOf(recno), "20", input);

		return PipeUtil.align(String.valueOf(PipeUtil.makeStrLength4(input)), 11, RIGHT);
	}
	/**
	 * Converts a normal string into a hex string.
	 *   Input:  "ab"
	 *   Output: "6162"
	 */
	private static String C2X(String input, int recno) {
		StringBuffer sb = new StringBuffer(input.length()*2);
		for (int i = 0; i < input.length(); i++) {
			String binary = Integer.toHexString(input.charAt(i)); 
			sb.append("00".substring(0, 2-binary.length()));
			sb.append(binary);
		}
		return sb.toString();
	}
	/**
	 * Converts a normal string into a binary digit string.
	 *   Input:  "ab"
	 *   Output: "0110000101100010"
	 */
	private static String C2B(String input, int recno) {
		StringBuffer sb = new StringBuffer(input.length()*8);
		for (int i = 0; i < input.length(); i++) {
			String binary = Integer.toBinaryString(input.charAt(i)); 
			sb.append("00000000".substring(0, 8-binary.length()));
			sb.append(binary);
		}
		return sb.toString();
	}
	/**
	 * Converts a 8-byte string into a signed double
	 *   Input:  "\u0040\u005e\u00dc\u00cc\u00cc\u00cc\u00cc\u00cd"
	 *   Output: "123.45"
	 */ 
	private static String C2F(String input, int recno) throws PipeException {
		if (input.length() != 8)
			throw new PipeException(-392, "C2F", String.valueOf(recno), "44", input);

		return String.valueOf(Double.longBitsToDouble(PipeUtil.makeStrLength8(input)));
	}
	/**
	 * Converts a length-prefixed string into a normal string
	 *   Input:  "\u0000\u0007abcdefg"
	 *   Output: "abcdefg"
	 */ 
	private static String C2V(String input, int recno) throws PipeException {
		if (input.length() < 2)
			throw new PipeException(-392, "C2V", String.valueOf(recno), "52", input);
		int length = (input.charAt(0) << 8) + input.charAt(1);
		if (length+2 != input.length())
			throw new PipeException(-392, "C2V", String.valueOf(recno), "52", input);
		return input.substring(2);
	}
	private static String C2P(String input, int recno) throws PipeException {
		// TODO C2P
		throw new PipeException(-391, "C2P");
	}
	private static String C2I(String input, int recno) throws PipeException {
		Date date = new Date(PipeUtil.makeStrLength8(input));
		return PipeUtil.makeISOStringFromDate(date); 
	}
	private static String C2T(String input, int recno) throws PipeException {
		Date date = new Date(PipeUtil.makeStrLength8(input));
		return PipeUtil.makeISOTimestampFromDate(date); 
	}

}
