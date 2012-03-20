package com.hae.pipe;

import java.util.*;

public class RangeSingle implements Range {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	/**
	 * Ternary attribute indicating chars/words/fields/
	 */
	private char _type = 'C';

	/**
	 * What character to use to separate the words
	 */
	private char _wordSep = ' ';

	/**
	 * What character to use to separate the fields
	 */
	private char _fieldSep = '\t';
	
	/**
	 * Inclusive starting index for the range (1-based)
	 */
	private int _start = 1;
	
	/**
	 * Inclusive ending index for the range (1-based).  If
	 * infinity, this value is Integer.MAX_VALUE.
	 */
	private int _end = Integer.MAX_VALUE;

	/**
	 * If this range is to be further divided, this is the subrange
	 * eg. SUBSTR 1-10 of 5-15 would result in start=5/end=15 
	 * for this Range, and start=1/end=10 for the subrange
	 */
	private RangeSingle _subRange = null;
	
	/**
	 * Takes a string in one of the following forms:
	 *   number
	 *   snumber
	 *   number-number
	 *   number.number
	 *   numorstar-numorstar
	 *   numorstar.numorstar
	 *   snumber;snumber
	 * where the numbers are 1-based offsets for strings.
	 */
	public RangeSingle(PipeArgs args) throws PipeException {
		boolean done = false;
		String word = args.nextWord();
		
		if (word.equals(""))
			throw new PipeException(-54, word);
		
		boolean inSubstring = false;
		while (!done && !word.equals("")) {
			if (Syntax.abbrev("FIELDSEPARATOR", word, 8) || Syntax.abbrev("FS", word, 2)) {
				String xorcWord = args.nextWord();
				if (Syntax.isXorC(xorcWord))
					_fieldSep = PipeUtil.makeChar(xorcWord);
				else
					throw new PipeException(50, xorcWord);
			}
			else if (Syntax.abbrev("WORDSEPARATOR", word, 7) || Syntax.abbrev("WS", word, 2)) {
				String xorcWord = args.nextWord();
				if (Syntax.isXorC(xorcWord))
					_wordSep = PipeUtil.makeChar(xorcWord);
				else
					throw new PipeException(50, xorcWord);
			}
			else if (Syntax.abbrev("WORDS", word, 1)) {
				_type = 'W';
			}
			else if (Syntax.abbrev("FIELDS", word, 1)) {
				_type = 'F';
			}
			else if (Syntax.abbrev("SUBSTRING", word, 6)) {
				inSubstring = true;
			}
			else if (Syntax.abbrev("OF", word, 2)) {
				_subRange = new RangeSingle(args);
				done = true;
			}
			else {
				// there are a few cases where the "word" contains two
				// tokens (eg. w1), so we try to split them out now
				if (word.toUpperCase().startsWith("W") && word.length() >= 2 && Syntax.isRange(word.substring(1))) {
					_type = 'W';
					word = word.substring(1); 
				}
				else if (word.toUpperCase().startsWith("F") && word.length() >= 2 && Syntax.isRange(word.substring(1))) {
					_type = 'F';
					word = word.substring(1); 
				}

				// at this point, the word *should* just be the numeric range portion
				if (Syntax.isSignedRange(word)) {
					String[] words = word.split(";");
					
					_start = Integer.parseInt(words[0]);
					_end = Integer.parseInt(words[words.length-1]);
				}
				else if (Syntax.isRange(word)) {
					String[] words = word.split("[\\.\\-]");
					boolean dot = word.indexOf('.') == -1 ? false : true;
					if (words[0].equals("*"))
						_start = 1;
					else
						_start = Integer.parseInt(words[0]);
					
					if (words.length == 1)
						_end = _start+1;
					else {
						if (words[1].equals("*"))
							_end = Integer.MAX_VALUE;
						else if (dot)
							_end = _start + Integer.parseInt(words[1]) - 1;
						else
							_end = Integer.parseInt(words[1]);
					}
				}
				else {
					// we should only get to here we are looking an
					// optional, but not present, range
					String input = args.getRemainder();
					args.undo();
					throw new PipeException(-54, input);
				}
				
				if (!inSubstring) {
					done = true;
					inSubstring = false;
				}
			}
			if (!done)
				word = args.nextWord();
		}
	}

	/**
	 * Returns whether the passed string is contained within the range
	 */
	public boolean contains(String input, String target, boolean anycase, boolean anyof) throws PipeException {
		if (anycase) {
			input = input.toLowerCase();
			target = target.toLowerCase();
		}
		int[] boundingIndexes = getBoundingIndexes(input);
		if (boundingIndexes[0] == boundingIndexes[1])
			return false;
		else {
			if (target.equals(""))
				return true;
			
			if (anyof) {
				// iterate over all the target chars and see if any of
				// them are in the input record
				for (int i = 0; i < target.length(); i++) {
					char c = target.charAt(i);
					if (input.indexOf(c) != -1)
						return true;
				}
				// nope, none found.
				return false;
			}
			else {
				return extractRange(input).contains(target);
			}
		}
	}

	public String extractRange(String input) throws PipeException {
		int[] boundingIndexes = getBoundingIndexes(input);
		return input.substring(boundingIndexes[0], boundingIndexes[1]);
	}

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
	public ReplaceResult replace(String input, String from, String to, int numberOfOccurrences, boolean anycase) throws PipeException {
		int[] boundingIndexes = getBoundingIndexes(input);
		
		// we accept -1 as a synonym for unlimited changes
		if (numberOfOccurrences == -1)
			numberOfOccurrences = Integer.MAX_VALUE;
		
		// if the from and to are the same, then no changes will occur
		if (from.equals(to))
			return new ReplaceResult(input, 0);
		
		// if we are trying to replace an empty string, the behaviour is
		// to pre-pend the 'to' string to the first range 
		if (from.equals(""))
			return new ReplaceResult(input.substring(0, boundingIndexes[0])+to+input.substring(boundingIndexes[0]), 1);
		
		// if we have asked to ignore case, then we have to do some
		// special case handling (but only if the from string is actually
		// longer than two chars - in which case, we just let the original 
		// strings alone)
		boolean specialCaseHandling = false;
		if (anycase && from.length() >= 2) {
			boolean containsUpper = false;
			for (int i = 0; i < from.length(); i++) {
				if (Character.isUpperCase(from.charAt(i))) {
					containsUpper = true;
					break;
				}
			}
			boolean noLetters = true;
			for (int i = 0; i < from.length(); i++) {
				if (Character.isLetter(from.charAt(i))) {
					noLetters = false;
					break;
				}
			}
			// if the from string has upper case chars, or no
			// letters, then we leave the strings alone. Otherwise
			// we try to preserve the case...
			if (!containsUpper && !noLetters)
				specialCaseHandling = true;
		}
		
		String pre = input.substring(0, boundingIndexes[0]);
		String middle = input.substring(boundingIndexes[0], boundingIndexes[1]);
		String post = input.substring(boundingIndexes[1]);
		
		String result = middle;
		int len = from.length();
		int pos = pos(result, from, 0, anycase);
		int count = 0;
		
		while ((pos >= 0) && ((numberOfOccurrences <= 0) || (count < numberOfOccurrences))) {
			String replacement = to;
			if (specialCaseHandling) {
				String replaced = result.substring(pos, pos + len);
				if (Character.isUpperCase(replaced.charAt(0)))
					if (Character.isUpperCase(replaced.charAt(1)))           
						replacement = replacement.toUpperCase();  // both uppercase, so uppercase the whole replacement
					else
						replacement = replacement.substring(0, 1).toUpperCase()+replacement.substring(1);
			}
			result = result.substring(0, pos) + replacement + result.substring(pos + len, result.length());
			count++;
			pos = pos(result, from, pos + replacement.length(), anycase); 
		}
		
		return new ReplaceResult(pre+result+post, count);
	}
	private int pos(String input, String target, int start, boolean anycase) {
		// probably a bit inefficient to keep uppercasing, but
		// I will deal with that a bit later (if required)
		return (anycase ? input.toUpperCase().indexOf(target.toUpperCase(), start) : input.indexOf(target, start));
	}

	/**
	 * Given an input string, recurse down through any child ranges
	 * and return absolute indexes into the input string that represent
	 * the range.
	 */ 
	private int[] getBoundingIndexes(String input) throws PipeException {
		int[] subBoundingIndexes = null;
		if (_subRange != null) {
			subBoundingIndexes = _subRange.getBoundingIndexes(input);
			input = _subRange.extractRange(input);
		}
		
		int[] boundingIndexes;
		if (_type == 'W')
			boundingIndexes = getBoundingIndexWords(input);
		else if (_type == 'F')
			boundingIndexes = getBoundingIndexFields(input);
		else
			boundingIndexes = getBoundingIndexChars(input);
		
		if (boundingIndexes[0] > boundingIndexes[1])
			throw new PipeException(-54, toString());

		if (subBoundingIndexes == null)
			return boundingIndexes;
		else
			return new int[] { boundingIndexes[0]+subBoundingIndexes[0], boundingIndexes[1]+subBoundingIndexes[0] };
	}
	
	/**
	 * Assuming that the range settings are for chars,
	 * work out what the bounding indexes are.
	 */
	private int[] getBoundingIndexChars(String input) {
		// these two are still 1-based
		int length = input.length();
		int startingIndex = (_start > 0 ? _start : Math.max(0, length + _start + 1));
		int endingIndex = (_end > 0 ? Math.min(_end, length+1) : Math.max(0, length + _end + 1));

		if (startingIndex-1 >= length)
			return new int[] { length, length };
		else
			return new int[] { startingIndex-1, Math.min(length, endingIndex) };
	}
	
	/**
	 * Assuming that the range settings are for words,
	 * work out what the bounding indexes are.
	 */
	private int[] getBoundingIndexWords(String input) {
		ArrayList<int[]> wordOffsets = new ArrayList<int[]>();

		// are we in the middle of processing a word?
		boolean inWord = false;

		int wordStart = 0;
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (c != _wordSep) {
				if (!inWord)
					wordStart = i;
				inWord = true;
			}
			else {
				if (inWord)
					wordOffsets.add(new int[] { wordStart, i });
				inWord = false;
			}
		}
		if (inWord)
			wordOffsets.add(new int[] { wordStart, input.length() });

		int startingWordIndex = (_start > 0 ? _start : Math.max(1, wordOffsets.size() + _start + 1));
		int endingWordIndex = (_end > 0 ? Math.min(_end-1, wordOffsets.size()-1) : Math.max(0, wordOffsets.size() + _end));

		if (startingWordIndex-1 >= wordOffsets.size())
			return new int[] { 0, 0 };
		else {
			int[] startOffsets = wordOffsets.get(startingWordIndex-1);
			int[] endOffsets = wordOffsets.get(Math.min(wordOffsets.size(), endingWordIndex));
			return new int[] { startOffsets[0], endOffsets[1] };
		}
	}
	
	/**
	 * Assuming that the range settings are for fields,
	 * work out what the bounding indexes are.
	 */
	private int[] getBoundingIndexFields(String input) {
		ArrayList<int[]> wordOffsets = new ArrayList<int[]>();

		int wordStart = 0;
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (c == _fieldSep) {
				wordOffsets.add(new int[] { wordStart, i });
				wordStart = i+1;
			}
		}
		wordOffsets.add(new int[] { wordStart, input.length() });

		int startingWordIndex = (_start > 0 ? _start : Math.max(0, wordOffsets.size() + _start + 1));
		int endingWordIndex = (_end > 0 ? Math.min(_end-1, wordOffsets.size()-1) : Math.max(0, wordOffsets.size() + _end));

		if (startingWordIndex-1 >= wordOffsets.size())
			return new int[] { 0, 0};
		else {
			int[] startOffsets = wordOffsets.get(startingWordIndex-1);
			int[] endOffsets = wordOffsets.get(Math.min(wordOffsets.size(), endingWordIndex));
			return new int[] { startOffsets[0], endOffsets[1] };
		}
	}

	/**
	 * Returns whether this range represents a simple "range" (true), or
	 * a more complex "inputRange" (false)
	 */
	public boolean isSimple() {
		if (_subRange != null || _type != 'C' || _wordSep != ' ' || _fieldSep != '\t')
			return false;
		else
			return true;
	}
	
	public int[] getStartAndEnd() {
		return new int[] { _start, _end }; 
	}
	
	/**
	 * Reverse the range details back to a normal range spec.
	 * Note that it may not be exactly the same as originally 
	 * specified (eg. 2.3 will be returned as 2-4, and *-* will
	 * be returned as 1-*)
	 */
	public String toString() {
		String wordsep = (_wordSep == ' ' ? "" : "WS "+_wordSep+" ");
		String fieldsep = (_fieldSep == '\t' ? "" : "FS "+_fieldSep+" ");
		String type = (_type == 'C' ? "" : ""+_type+" ");
		String sep = ((_start < 0 || _end < 0) ? ";" : "-");
		String range = _start+sep+(_end == Integer.MAX_VALUE ? "*" : ""+_end);
		String subrange = (_subRange == null ? range : "SUBSTR "+range+" OF "+_subRange.toString());
		return wordsep+fieldsep+type+subrange;
	}
}
