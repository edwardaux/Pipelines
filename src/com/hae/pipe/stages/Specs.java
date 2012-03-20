package com.hae.pipe.stages;

import java.util.*;

import com.hae.pipe.*;

/**
 *          
 *  
 */
public class Specs extends Stage {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private static int STOP_ALLEOF = -2;
	private static int STOP_ANYEOF = -1;
	
	private char _pad = ' ';
	private int _recno = 0;
	
	public int execute(String args) throws PipeException {
		signalOnError();
		try {
			commit(-2);
			
			@SuppressWarnings("unused")
			int stop = STOP_ALLEOF;
			PipeArgs pa = new PipeArgs(args);
			if (pa.getRemainder().trim().equals(""))
				throw new PipeException(-11);
			
			String word = scanWord(pa);
			if (Syntax.abbrev("STOP", word, 4)) {
				word = scanWord(pa);
				if (Syntax.abbrev("ALLEOF", word, 6))
					stop = STOP_ALLEOF;
				else if (Syntax.abbrev("ANYEOF", word, 6))
					stop = STOP_ANYEOF;
				else if (Syntax.isNumber(word))
					stop = PipeUtil.makeInt(word);
				else
					return exitCommand(-58, word);
			}
			else
				pa.undo();
			
			ArrayList<PlainItem> itemGroups = new ArrayList<PlainItem>();
			while (!pa.getRemainder().trim().equals("")) {
				word = pa.peekWord();
				if (Syntax.abbrev("PAD", word, 3))
					itemGroups.add(new Pad(pa));
				else if (Syntax.abbrev("SET", word, 3))
					itemGroups.add(new Set(pa));
				else if (Syntax.abbrev("SELECT", word, 6) ||
					     Syntax.abbrev("READ", word, 4) ||
					     Syntax.abbrev("READTSOP", word, 8) ||
					     Syntax.abbrev("OUTSTREAM", word, 9) ||
					     Syntax.abbrev("WRITE", word, 5) ||
					     Syntax.abbrev("NOWRITE", word, 7))
					itemGroups.add(new StreamControl(pa));
				else if (Syntax.abbrev("BREAK", word, 5) ||
					     Syntax.abbrev("EOF", word, 3))
					itemGroups.add(new BreakControl(pa));
				else
					itemGroups.add(new DataField(pa));
			}
			
			// make sure that the primary input stream is connected
			if (streamState(INPUT, 0) != 0)
				return exitCommand(-102, "0");
			
			// make sure no other input streams are connected
			int maxStream = maxStream(INPUT);
			for (int i = 1; i <= maxStream; i++) {
				streamState(INPUT, i);
				if (RC >= 0 && RC <= 8)
					return exitCommand(-264, ""+i);
			}
				
			commit(0);
		
			while (true) {
				String s = peekto();
				String output = "";
				for (int i = 0; i < itemGroups.size(); i++) {
					ItemGroup itemGroup = (ItemGroup)itemGroups.get(i);
					output = itemGroup.evaluate(s, output);
				}
				output(output);
				_recno++;
				readto();
			}
		}
		catch(EOFException e) {
		}
		return 0;
	}
	
	public abstract class ItemGroup {
		public abstract String evaluate(String originalInput, String outputSoFar) throws PipeException;
	}
	public class Expression {
	}
	public class IfGroup extends ItemGroup {
//		private ItemGroup _true;
//		private ItemGroup _false;
//		private Expression _condition;

		public String evaluate(String originalInput, String outputSoFar) {
			return outputSoFar;
		}
	}
	public abstract class PlainItem extends ItemGroup {
	}
	public class Pad extends PlainItem {
		private char _newPad = ' ';
		public Pad(PipeArgs pa) throws PipeException {
			// consume the PAD keyword
			pa.nextWord();
			String word = pa.nextWord();
			if (Syntax.isXorC(word))
				_newPad = PipeUtil.makeChar(word);
			else
				throw new PipeException(-50, word); 
		}
		public String evaluate(String originalInput, String outputSoFar) {
			_pad = _newPad;
			return outputSoFar;
		}
	}
	public class Set extends PlainItem {
		public Set(PipeArgs pa) throws PipeException {
		}
		public String evaluate(String originalInput, String outputSoFar) {
			return outputSoFar;
		}
	}
	public class StreamControl extends PlainItem {
		public StreamControl(PipeArgs pa) throws PipeException {
		}
		public String evaluate(String originalInput, String outputSoFar) {
			return outputSoFar;
		}
	}
	public class BreakControl extends PlainItem {
		public BreakControl(PipeArgs pa) throws PipeException {
		}
		public String evaluate(String originalInput, String outputSoFar) {
			return outputSoFar;
		}
	}
	public class DataField extends PlainItem {
		private boolean _strip = false;
		private Conversion _conversion = null;
		private InputSource _inputSource;
		private OutputPlacement _outputPlacement;
		
		public DataField(PipeArgs pa) throws PipeException {
			String word = pa.nextWord();
			pa.undo();
			if (Syntax.abbrev("NUMBER", word, 6) || Syntax.abbrev("RECNO", word, 5))
				_inputSource = new InputSourceNumber(pa);
			else if (Syntax.abbrev("TODCLOCK", word, 3))
				_inputSource = new InputSourceTOD(pa);
			else if (Syntax.abbrev("ID", word, 2))
				_inputSource = new InputSourceId(pa);
			else if (Syntax.abbrev("PRINT", word, 5))
				_inputSource = new InputSourcePrint(pa);
			else {
				Range range = scanRange(pa, false);  // TODO deal with a leading letter:
				if (range != null) {
					_inputSource = new InputSourceRange(range);
				}
				else {
					String string = pa.nextDelimString(true);
					_inputSource = new InputSourceString(string);
				}
			}
			
			// now check for strip and conversion keywords
			word = pa.nextWord();
			if (Syntax.abbrev("STRIP", word, 5)) {
				_strip = true;
				word = pa.nextWord();
			}
			if (word.length() == 3 && word.charAt(1) == '2') {
				_conversion = Conversion.create(word);
				word = pa.nextWord();
			}

			// now we are up to output placements
			int defaultAlignment = (_inputSource instanceof InputSourceNumber ? RIGHT : LEFT);
			int defaultLength = (_inputSource instanceof InputSourceNumber ? 10 : -1);
			pa.undo();
			if (Syntax.isNumber(word))
				_outputPlacement = new OutputPlacementNumber(pa, defaultAlignment, defaultLength);
			else if (Syntax.isRange(word))
				_outputPlacement = new OutputPlacementRange(pa, (RangeSingle)scanRange(pa, true), defaultAlignment);
			else if (word.toUpperCase().startsWith("N"))
				_outputPlacement = new OutputPlacementNext(pa, defaultAlignment, defaultLength);
			else
				throw new PipeException(-63, word);
			
			// TODO Expression
			// TODO Can also be a . - See page 624 of spec
		}
		
		public String evaluate(String originalInput, String outputSoFar) throws PipeException {
			String newField = _inputSource.extractInput(originalInput);
			if (_strip)
				newField = newField.trim();
			if (_conversion != null)
				newField = _conversion.convert(newField, _recno);
			return _outputPlacement.place(outputSoFar+_outputPlacement.getPrefix(outputSoFar), newField);
		}

		public abstract class InputSource {
			public abstract String extractInput(String input) throws PipeException;
		}
		public class InputSourceRange extends InputSource {
			private Range _range;
			public InputSourceRange(Range range) {
				_range = range;
			}
			public String extractInput(String input) throws PipeException {
				return _range.extractRange(input);
			}
		}
		public class InputSourceNumber extends InputSource {
			private int _from = 1;
			private int _by = 1;
			public InputSourceNumber(PipeArgs pa) throws PipeException {
				// consume the NUMBER/RECNO keyword
				pa.nextWord();
				
				String word = pa.nextWord();
				if (Syntax.abbrev("FROM", word, 4)) {
					word = pa.nextWord();
					if (Syntax.isSignedNumber(word)) {
						_from = PipeUtil.makeInt(word);
						word = pa.nextWord();
					}
					else
						throw new PipeException(-58, word);
				}
				if (Syntax.abbrev("BY", word, 2)) {
					word = pa.nextWord();
					if (Syntax.isSignedNumber(word)) {
						_by = PipeUtil.makeInt(word);
						word = pa.nextWord();
					}
					else
						throw new PipeException(-58, word);
				}
				pa.undo();
			}
			public String extractInput(String input) {
				int recno = (_from + _recno) + (_recno * (_by-1));
				return ""+recno;
			}
		}
		public class InputSourceTOD extends InputSource {
			public InputSourceTOD(PipeArgs pa) {
				// consume the TODCLOCK keyword
				pa.nextWord();
			}
			public String extractInput(String input) {
				return PipeUtil.makeBinLength8(System.currentTimeMillis());
			}
		}
		public class InputSourceId extends InputSource {
			@SuppressWarnings("unused")
			private String _letter;
			public InputSourceId(PipeArgs pa) throws PipeException {
				// consume the ID keyword
				pa.nextWord();

				String word = pa.nextWord();
				if (word.equals(""))
					throw new PipeException(-1032, "");
				_letter = word;
			}
			public String extractInput(String input) {
				return input;
			}
		}
		public class InputSourcePrint extends InputSource {
			public InputSourcePrint(PipeArgs pa) throws PipeException {
				// consume the PRINT keyword
				pa.nextWord();
			}
			public String extractInput(String input) {
				return input;
			}
		}
		public class InputSourceString extends InputSource {
			private String _string;
			public InputSourceString(String string) {
				_string = string;
			}
			public String extractInput(String input) {
				return _string;
			}
		}
		public abstract class OutputPlacement {
			protected int _alignment;
			protected String _prefix = "";
			public abstract int[] getBoundingIndexes(String outputSoFar, String newField);
			
			public String place(String outputSoFar, String newField) {
				int[] boundingIndexes = getBoundingIndexes(outputSoFar, newField);
				int start = boundingIndexes[0];
				int length = boundingIndexes[1];
				return PipeUtil.insert(outputSoFar, newField, start, length, _alignment, _pad);
			}
			protected String getPrefix(String outputSoFar) {
				return outputSoFar.length() == 0 ? "" : _prefix;
			}
			protected void setAlignment(PipeArgs pa) {
				setAlignment(pa, LEFT);
			}
			protected void setAlignment(PipeArgs pa, int defaultAlignment) {
				String word = pa.nextWord();
				if (Syntax.abbrev("LEFT", word, 1))
					_alignment = LEFT;
				else if (Syntax.abbrev("CENTER", word, 1) || Syntax.abbrev("CENTRE", word, 1))
					_alignment = CENTER;
				else if (Syntax.abbrev("RIGHT", word, 1))
					_alignment = RIGHT;
				else {
					pa.undo();
					_alignment = defaultAlignment;
				}
			}
		}
		public class OutputPlacementNext extends OutputPlacement {
			private int _length = -1;
			public OutputPlacementNext(PipeArgs pa, int defaultAligment, int defaultLength) throws PipeException {
				_length = defaultLength;
				String word = pa.nextWord();
				int dotIndex = word.indexOf('.');
				if (dotIndex != -1) {
					String length = word.substring(dotIndex+1);
					if (Syntax.isNumber(length))
						_length = PipeUtil.makeInt(length);
					else
						throw new PipeException(-58, length);
					word = word.substring(0, dotIndex);
				}
				if (Syntax.abbrev("NEXT", word, 1))
					_prefix = "";
				else if (Syntax.abbrev("NEXTWORD", word, 5) || Syntax.abbrev("NW", word, 2))
					_prefix = " ";
				else if (Syntax.abbrev("NEXTFIELD", word, 5) || Syntax.abbrev("NF", word, 2))
					_prefix = "\t";
				else 
					throw new PipeException(-63, word);
				
				setAlignment(pa, defaultAligment);
			}
			public int[] getBoundingIndexes(String outputSoFar, String newField) {
				return new int[] { outputSoFar.length()+1, _length == -1 ? newField.length() : _length };
			}
		}
		public class OutputPlacementNumber extends OutputPlacement {
			private int _position;
			private int _length;
			public OutputPlacementNumber(PipeArgs pa, int defaultAligment, int defaultLength) throws PipeException {
				_length = defaultLength;
				String word = pa.nextWord();
				if (Syntax.isNumber(word))
					_position = PipeUtil.makeInt(word);
				else
					throw new PipeException(-58, word);
				
				setAlignment(pa, defaultAligment);
			}
			public int[] getBoundingIndexes(String outputSoFar, String newField) {
				return new int[] { _position, (_length == -1 ? newField.length() : _length) };
			}
		}
		public class OutputPlacementRange extends OutputPlacement {
			private int[] _boundingIndexes;
			public OutputPlacementRange(PipeArgs pa, RangeSingle range, int defaultAligment) throws PipeException {
				if (!range.isSimple())
					throw new PipeException(-63, range.toString());
				_boundingIndexes = range.getStartAndEnd();
				if (_boundingIndexes[1] == Integer.MAX_VALUE)
					throw new PipeException(-556);
				_boundingIndexes[1] = _boundingIndexes[1] - _boundingIndexes[0] + 1;
				
				setAlignment(pa, defaultAligment);
			}
			public int[] getBoundingIndexes(String outputSoFar, String newField) {
				return _boundingIndexes;
			}
		}
		public class OutputPlacementExpr extends OutputPlacement {
			public OutputPlacementExpr(PipeArgs pa, int defaultAlignment) {
				setAlignment(pa, defaultAlignment);
			}
			public int[] getBoundingIndexes(String outputSoFar, String newField) {
				return new int[] { 1, newField.length() };  // TODO expression
			}
		}
	}

}
