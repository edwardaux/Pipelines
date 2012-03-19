package com.hae.pipe;

public interface Range {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	/**
	 * Returns whether the passed string is contained within the range
	 */
	public boolean contains(String input, String target, boolean anycase, boolean anyof) throws PipeException;
	
	/**
	 * Once this range has been constructed, this method will 
	 * return the data represented by the ranges settings.  For
	 * example, if the ranged was constructed with "2-4", and
	 * the string that is passed to this method is "abcdef",
	 * then it would return "bcd"
	 */
	public String extractRange(String input) throws PipeException;
	
	/**
	 * Replace all occurrences in 'input' of the 'from' string with 
	 * the 'to' string (within the bounds of the range settings) up 
	 * to a maximum of 'numberOfOccurrences' times.
	 * 
	 * The following quote from the 'change' stage in the Author's Edition
	 * of the PIPELINEs manual is relevant for case sensitivity:
	 * 
	 * When the keyword ANYCASE is specified, the characters in the first string and the input
	 * record are compared in uppercase to determine whether the specified string is present.
	 * When the first string contains one or more uppercase characters or contains no letters, the
	 * second string is inserted in the output record without change of case; otherwise, an attempt
	 * is made to preserve the case of the string being replaced. When the first string contains no
	 * uppercase letters and begins with one or more (lowercase) letters, the following rules determine
	 * the case of the replacement string:
	 *   - When the first two characters of the replaced string in the input record are both lowercase,
	 *     the replacement string is used without change.
	 *   - When the first character of the replaced string in the input record is uppercase and the
	 *     second one is lowercase (or not a letter or the string is one character), the first letter of
	 *     the replacement string is uppercased.
	 *   - When the first two characters of the replaced string in the input record are uppercase,
	 *     the complete replacement string is uppercased.
	 */
	public ReplaceResult replace(String input, String from, String to, int numberOfOccurrences, boolean anycase) throws PipeException;
	
	public static class ReplaceResult {
		public String changed;
		public int numberOfChanges;
		public ReplaceResult(String c, int n) {
			changed = c;
			numberOfChanges = n;
		}
	}
}
