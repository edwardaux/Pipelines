package com.hae.pipe;

import java.nio.charset.Charset;

public class PipeArgs {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	/**
	 * The original input string
	 */
	private String _input;
	
	/**
	 * The current offset into the string
	 */
	private int _offset;
	
	/**
	 * The offset that we will reset to if
	 * the undo() function is called
	 */
	private int _undoOffset;
	
	/**
	 * A single mark that can be set to mark the current
	 * location, and return to it a later state (useful
	 * for undoing multiple reads
	 */
	private int _mark;
	
	public PipeArgs(String input) {
		_input = input;
		_offset = 0;
		_undoOffset = 0;
	}
	
	/**
	 * Returns the original input
	 */
	public String getInput() {
		return _input;
	}
	
	/**
	 * Returns any remaining (unread) characters
	 */
	public String getRemainder() {
		return _offset >= _input.length() ? "" : _input.substring(_offset);
	}
	
	/**
	 * Returns the next space-delimited word.  Returns an empty
	 * string if there are no more words.
	 */
	public String nextWord() {
		// save the offset
		_undoOffset = _offset;
		
		// skip to first non-blank
		int x = skipBlanks(_offset);

		// empty string
		if (x >= _input.length())
			return "";

		// now read to the next blank
		int nextBlank = nextBlank(x);
		_offset = nextBlank+1;
		return _input.substring(x, nextBlank);
	}
	
	/**
	 * Peeks as to what the next word is
	 */
	public String peekWord() {
		String s = nextWord();
		undo();
		return s;
	}
	/**
	 * Returns the next delimited string of the following types:
	 *   x0A0B2C09     the delim is a 'x' or 'X' and the digits are 0-9A-F
	 *   b01010011     the delim is a 'b' or 'B' and the digits are 0 or 1
	 *   /abc/         the delim can be any char
	 * Throws an exception if mandatory, but not found.
	 */
	public String nextDelimString(boolean mandatory) throws PipeException {
		// save the offset
		_undoOffset = _offset;
		
		// skip to first non-blank
		int x = skipBlanks(_offset);

		// empty string
		if (x >= _input.length()) {
			if (mandatory)
				throw new PipeException(-11);
			else
				return "";
		}
		
		char c = _input.charAt(x);
		if (c == 'b' || c == 'B') {
			int end = nextBlank(x+1);
			// contains the leading 'b'
			String data = _input.substring(x, end);
			if (data.length()-1 == 0)       // ignore leading 'b'
				throw new PipeException(-337, c);
			else if ((data.length()-1) % 8 != 0)
				throw new PipeException(-336, data);
			else if (!Syntax.isBinString(data))
				throw new PipeException(-338, data);
			else {
				data = data.substring(1);        // ditch the leading 'b'
				byte[] bytes = new byte[data.length() / 8];
				for (int i = 0; i < bytes.length; i++) {
					bytes[i] = (byte)Integer.parseInt(_input.substring(x+1, x+9), 2);
					x += 8;
				}
				_offset = end;
				return new String(bytes, Charset.forName("ISO-8859-1"));
			}
		}
		else if (c == 'x' || c == 'X') {
			int end = nextBlank(x+1);
			// contains the leading 'x'
			String data = _input.substring(x, end);
			if (data.length()-1 == 0)       // ignore leading 'x'
				throw new PipeException(-64, c);
			else if ((data.length()-1) % 2 != 0)
				throw new PipeException(-335, data);
			else if (!Syntax.isHexString(data))
				throw new PipeException(-65, data);
			else {
				data = data.substring(1);        // ditch the leading 'h'
				byte[] bytes = new byte[data.length() / 2];
				for (int i = 0; i < bytes.length; i++) {
					bytes[i] = (byte)Integer.parseInt(_input.substring(x+1, x+3), 16);
					x += 2;
				}
				_offset = end;
				return new String(bytes, Charset.forName("ISO-8859-1"));
			}
		}
		else {
			int nextDelim = _input.indexOf(c, x+1);
			if (nextDelim == -1) {
				// missing end delim
				throw new PipeException(-60, _input);
			}
			else {
				// found a blank
				_offset = nextDelim+1;
				return _input.substring(x+1, nextDelim);
			}
		}
	}

	/**
	 * Returns everything within the next set of parenthesis.  If next
	 * non-blank character is not a ( or the end of the line, then you 
	 * get returned an empty string. Otherwise, you will get everything 
	 * (non-inclusive of the parentheses) between the ( and the ) 
	 */
	public String nextExpression() throws PipeException {
		// save the offset
		_undoOffset = _offset;
		
		// skip to first non-blank
		int x = skipBlanks(_offset);

		// empty string?
		if (x >= _input.length())
			return "";

		if (_input.charAt(x) != '(')
			return "";
		
		// now read to the next blank
		int nextParen = _input.indexOf(')', x);
		if (nextParen == -1)
			throw new PipeException(-200, _input.substring(x));
		_offset = nextParen+1;
		return _input.substring(x+1, nextParen);
	}
	
	/**
	 * Hangs on to the current offset so that you can
	 * optionally return the offset to it at some later 
	 * stage (useful if you want to under multiple reads)
	 */
	public void mark() {
		_mark = _offset;
	}
	
	/**
	 * Resets the offset to the previously saved offset.
	 * Note that we only support one level of undo
	 */
	public void undo() {
		_offset = _undoOffset;
	}
	
	/**
	 * Resets the offset back to 0
	 */
	public void reset() {
		_offset = 0;
		_undoOffset = 0;
	}
	
	/**
	 * Resets the offset back to the mark
	 */
	public void resetMark() {
		_offset = _mark;
		_undoOffset = _mark;
	}
	
	/**
	 * Returns the index of the next non-blank, but
	 * also taking into account the end of string
	 */
	private int skipBlanks(int currentOffset) {
		while (currentOffset < _input.length() && _input.charAt(currentOffset) == ' ')
			currentOffset++;
		return currentOffset;
	}
	
	/**
	 * Returns the index of the next blank, but
	 * also taking into account the end of string
	 */
	private int nextBlank(int currentOffset) {
		int end = _input.indexOf(' ', currentOffset);
		if (end == -1)
			end = _input.length();
		return end;
	}
}
