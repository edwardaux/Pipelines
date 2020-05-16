package com.hae.pipe.stages;

import java.util.*;

import com.hae.pipe.*;
import static com.hae.pipe.PipeConstants.INPUT;

/**
 * {@literal
 *                                       ┌─Ascending───┐
 *  >>──SORT──┬─────────┬──┤ Padtype ├───┼─────────────┼───────────────────<<
 *            ├─COUNT───┤                ├─Descending──┤
 *            └─UNIQUE──┘                └─┤ Group ├───┘
 *
 *  Padtype:
 *     ┌─NOPAD────────────┐
 *  ├──┼──────────────────┼─────────────────────────────────────────────────┤
 *     └─PAD──┬─char────┬─┘
 *            ├─hexchar─┤
 *            ├─BLANK───┤
 *            └─SPACE───┘
 *
 *  Group:
 *    ┌───────────────────────────────────────────┐
 *    V              ┌─Ascending───┐              |
 *  ├─┴─columnrange──┼─────────────┼──┤ Padtype ├─┴─────────────────────────┤
 *                   └─Descending──┘
 *
 *
 *
 * Reference used: z/VM, CMS Pipelines Reference, Version 3 Release 1.0
 *
 * The sort program used is Java Collections.sort, a stable sort.
 *
 * Please see "? IMPROVEMENT ?" below for possible need of improvement
 * }
 */
public class Sort extends Stage {
        private boolean unique = false;
        private boolean count  = false;

	// Padding instructions when the sort is on the full-record key
	private PadInstructions fullRecPadInstructions;

	// Array of KeyField objects, each corresponding to one columnrange in
	// the "|Group|" part of syntax diagram
	private ArrayList<Comparator<String>> keyFields = new ArrayList<>();

	public int execute(String args) throws PipeException {
		signalOnError();
		ArrayList<String> buffer = new ArrayList<>();
		try {
			commit(-2);

			PadtypeParser padParser = new PadtypeParser();

			PipeArgs pa = new PipeArgs(args);

			String word = scanWord(pa);

			// COUNT/UNIQUE group ?
			if (!"".equals(word)) {
				if (Syntax.abbrev("UNIQUE", word, 4)) {
					unique = true;
					word = scanWord(pa);  // found option, prepare next word
				}
				else if (Syntax.abbrev("COUNT", word, 5)) {
					count = true;
					word = scanWord(pa);  // found option, prepare next word
				}
			}

			// "|Padtype|" group ?
			// Either appears at this spot in args or it doesn't.
			//
			// A PadInstructions object is always created here.
			// It applies only if the sort is on the full-record key.
			//
			// If columnranges are specified further on, they will
			// each get their own PadInstructions object and this full-record
			// PadInstructions object will not be used.
			pa.undo();
			fullRecPadInstructions = padParser.createPadInstructions(pa);
			word = scanWord(pa);

			// key fields?    (ASC/DESC/Group see syntax diagram)
			// When this operand group is not found, default key field will be:
			//   full-record key, ascending, using full-rec pad instructions found above
			RangeSingle defRange = new RangeSingle(new PipeArgs("1-*"));
			keyFields.add(new KeyField(defRange, true, fullRecPadInstructions));

			// Now see if one or more EXPLICIT key fields was given. If found,
			// it/they will replace the default full-rec KeyField just created above
			if (!"".equals(word)) {

				// three EXPLICIT possibilities
				if (Syntax.abbrev("ASCENDING", word, 1)) {
					// The default, explicitly specified. Done already above.
					word = scanWord(pa);
				}
				else if (Syntax.abbrev("DESCENDING", word, 1)) {
					// make a descending, full-rec KeyField and replace ASC default
					keyFields.clear();
					RangeSingle rng = new RangeSingle(new PipeArgs("1-*"));
					keyFields.add(new KeyField(rng, false, fullRecPadInstructions));
					word = scanWord(pa);
				}
				else {
					// "|Group|" in the syntax diagram
					//    m-n [ASC/DESC] [padtype]
					RangeSingle crRange;
					boolean crAscending;
					PadInstructions crPadInst;
					ArrayList<Comparator<String>> newKeys = new ArrayList<>();

					// create a KeyField for each columnrange
					while (!"".equals(word)) {

						// if not a SIMPLE columnrange then stop looking
						if (! Syntax.isRange(word)) {
							break;
						}

						// Define a RangeSingle for the SIMPLE range found
						crRange = new RangeSingle(new PipeArgs(word));

						// get optional ASC/DESC, default is ASC if not specified
						crAscending = true;
						word = scanWord(pa);
						if (Syntax.abbrev("ASCENDING", word, 1)) {
							crAscending = true;
							word = scanWord(pa);
						}
						else if (Syntax.abbrev("DESCENDING", word, 1)) {
							crAscending = false;
							word = scanWord(pa);
						}

						// PAD instructions for this key field
						pa.undo();
						crPadInst = padParser.createPadInstructions(pa);
						word = scanWord(pa);

						// create KeyField object for this columnrange
						newKeys.add(new KeyField(crRange, crAscending, crPadInst));
					}

					// Did we make any columnrange SortKeys?
					// Then replace the default "full-record" KeyField with these
					if (! newKeys.isEmpty()) {
						if (newKeys.size() > 10) { // z/VM manual says limit is 10
							throw new PipeException(-56);
						}
						keyFields = newKeys;
					}
				}
			}

			// No more operands expected on cmdline. 
			if (!"".equals(word)) {
				return exitCommand(-112, word); // excessive operands
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

			String x;
			// SORT delays the records. Read them all into buffer.
			// Just keep on consuming them until EOF, and rely
			// on the EOFException getting thrown...
			while (true) {
				String s = peekto();
				// a normal record. add it to the buffer
				buffer.add(s);
				readto();
			}
		}
		catch(EOFException e) {
			// EOF received, time to sort the buffered data and send to output
			try {
				Collections.sort(buffer, new MultiComparator<String>(keyFields));

				if (unique)
					uniqueOutput(buffer);
				else if (count)
					countOutput(buffer);
				else
					allOutput(buffer);

				buffer.clear();
			}
			catch(EOFException e2) {
				// uh, oh... output is not connected.  That's OK though
			}
		}
		return 0;
	}

	/**
	 * KeyField object contains instructions on how to compare 2 records.
	 * <p>
 	 * A KeyField contains:
	 * <ul>
 	 * <li>the RangeSingle object (column location of the key field)
	 * <li>sort direction for this key (boolean: true ASC, false DESC)
	 * <li>padding instructions when comparing data of different length
	 * </ul>
	 */
	public class KeyField implements Comparator<String> {
		// inner class member variables "_xxx"
		private RangeSingle _keyRange;
		private boolean _sortAscending;
		private PadInstructions _padInstructions;
		private char _padChar;

		// reusable tool
		private StringBuilder _strBuilder = new StringBuilder();

		public KeyField(RangeSingle r, boolean asc, PadInstructions pad) {
			_keyRange = r;
			_sortAscending = asc;
			_padInstructions = pad;
			if (_padInstructions.isPadRequested()) {
				_padChar = _padInstructions.getPadChar();
			}
		}

		public void setAscending(boolean TF) {
			_sortAscending = TF;
		}

		private String pad(String s, int len) {

			_strBuilder.setLength(0);
			_strBuilder.append(s);
			
			while(_strBuilder.length() < len) {
				_strBuilder.append(_padChar);
			}
			return _strBuilder.toString();
		}

		/**
		 * Return the String found in this key field's columnrange,
		 * no padding applied.
		 * <p>
		 * The string may be shorter than the nominal columnrange if the input
		 * record is too short. Contrast this with getKeys().
		 * 
		 * @param rec - String input record from which to extract key range,
		 * @return String[] - [0] key from rec1, [1] key from rec2
		 */
		public String getKeyRange(String rec) {
			String key;

			try {
				key = _keyRange.extractRange(rec);
			}
			catch (PipeException pe) {
				// ? IMPROVEMENT ?
				// Forced to throw a RuntimeException. Can't
				// pass PipeException up the call chain because
				// compare() method signature does not allow "throws PipeException".
				throw new RuntimeException(
					"Error extracting key field (range " + 
					_keyRange.toString() + 
					") from input record, PipeException " + 
					pe.getMessageNo()
				);
				// Is this a problem? For a RangeSingle.isSimple() range, 
				// which this is, the PipeException should never occur?
			}
			return key;
		}

		/**
		 * Compare two records using the instructions in this KeyField object.
		 * 
		 * @param rec1 - input record to compare
		 * @param rec2 - input record to compare
		 * @return int - -1, 0, 1
		 */
		public int compare(String rec1, String rec2) {
			String k1, k2;
			String[] keys;
			keys = getKeys(rec1, rec2);
			k1 = keys[0];
			k2 = keys[1];
			int result = (_sortAscending) ? k1.compareTo(k2) : k2.compareTo(k1);
			if (result != 0) {
				return result;
			}
			return 0;
		}

		/**
		 * Return an array with the "effective" keys from the specified
		 * input records, "effective" meaning after any PadInstructions 
		 * have been applied.
		 * <p>
		 * Contrast this with getKeyRange().
		 * 
		 * @param rec1 - input record to compare
		 * @param rec2 - input record to compare
		 * @return String[] - [key from rec1, key from rec2]
		 */
		public String[] getKeys(String rec1, String rec2) {
			String k1 = getKeyRange(rec1);
			String k2 = getKeyRange(rec2);

			if (_padInstructions.isPadRequested()) {
				if (k1.length() < k2.length()) {
					k1 = pad(k1, k2.length());
				}
				else if (k2.length() < k1.length()) {
					k2 = pad(k2, k1.length());
				}
			}
			return new String[] {k1, k2};
		}
	}

	/**
	 * Comparator to pass to Collections.sort, supports multiple key fields.
	 */
	public class MultiComparator<String> implements Comparator<String> {
		private final ArrayList<Comparator<String>> _keyfields;

		public MultiComparator(ArrayList<Comparator<String>> list) {
			this._keyfields = list;
		}

		/**
		 * Compare two records by all the key fields.
		 * <p>
		 * The key fields are applied, in order, until we get a NOT EQUAL
		 * comparison and return -1 or +1.
		 * <p>
		 * If all key fields are applied, and are all equal, return 0.
		 * 
		 * @param rec1 - String record X
		 * @param rec2 - String record Y
		 * @return int - -1, 0, +1 (see Comparator interface)
		 */
		public int compare(String rec1, String rec2) {

			// Compare key field by key field till find inequality
			for (Comparator<String> kf : _keyfields) {
				int result = kf.compare(rec1, rec2);
				if (result != 0) {
					return result;
				}
			}
			// fall through, all key fields equal
			return 0;
		}
	}

	/**
	 * Padding instructions for a KeyField object, used when comparing
	 * key field contents of different lengths.
	 */
	public class PadInstructions {
		// inner class member variables "_xxx"
		private boolean _padding;
		private char _padChar;

		/**
		 * NOPAD Constructor
		 */
		public PadInstructions() {
			_padding = false;
			_padChar = 0x00; // placeholder, not used for NOPAD
		}

		/**
		 * PAD Constructor
		 * 
		 * @param padchar - padding character 
		 */
		public PadInstructions(char padchar) {
			_padding = true;
			_padChar = padchar;
		}

		public boolean isPadRequested() {
			return _padding;
		}

		public char getPadChar() {
			return _padChar;
		}
	}

	/**
	 * A parser for a "|Padtype|" operand, as shown in syntax diagram.
	 *<p>
	 * Other stages (COLLATE, MERGE, UNIQUE...) also use this exact
	 * "|Padtype|" syntax in spots. Maybe this class or some variation could 
	 * be moved out of SORT stage, for general use?
	 */
	public class PadtypeParser {
		// inner class member variables "_xxx"

		/**
		 * Constructor
		 */
		public PadtypeParser() {
		}

		/**
		 * Parse the cmdline for a "|Padtype|" operand at the point specified 
		 * by the PipeArgs parm, and return a PadInstructions object from 
		 * what is found there.
		 *
		 * If PipeArgs is NOT pointing to an explicit Padtype operand:
		 * - return a "NOPAD" PadInstructions object (default when nothing specified).
		 * - PipeArgs pointer not changed.
		 *
		 * If PipeArgs IS pointing to a Padtype operand group:
		 * - return the PadInstructions object specified (PAD or NOPAD). 
		 * - Padtype operands are consumed in PipeArgs.
		 *
		 * In both scenarios, upon return to the caller, they must 
		 * invoke scanWord(pa) to begin parsing the rest of the cmdline.
		 *
		 * @param pa - PipeArgs object pointing to the cmdline position 
		 *             where a Padtype operand should begin.
		 * @return PadInstructions object
		 */
		public PadInstructions createPadInstructions(PipeArgs pa) throws PipeException {

			char _padChar;
			PadInstructions _padinst;

			// Don't consume words unless they are part of Padtype operand group
			String word = pa.peekWord();

			if (Syntax.abbrev("NOPAD", word, 5)) {
				scanWord(pa); // consume the peeked keyword
				_padinst = new PadInstructions();
			}
			else if (Syntax.abbrev("PAD", word, 3)) {
				scanWord(pa); // consume the peeked keyword

				// next word MUST be a pad char/keyword, consume it
				word = scanWord(pa);
				if (!"".equals(word)) {
					if (Syntax.abbrev("BLANK", word, 5) ||
						Syntax.abbrev("SPACE", word, 5)) {
						_padChar = ' ';
					}
					else if (Syntax.isXorC(word)) {
						_padChar = PipeUtil.makeChar(word);
					}
					else {
						// Invalid PAD character
						throw new PipeException(-49, "PAD");  // correct error message ???
					}
					_padinst = new PadInstructions(_padChar);
				}
				else {
					// end of PipeArgs, missing PAD character
					throw new PipeException(-15, "PAD");  // correct error message ???
				}
			}
			else {
				// "peekword" is not part of a Padtype operand group, we consumed nothing.
				//
				// when no Padtype operands specified, default is "NOPAD" object.
				_padinst = new PadInstructions();
			}

			return _padinst;
			// on return, caller should do a scanWord() 
			// to continue parsing SORT cmdline args
		}
	}

	/**
	 * Output all sorted records.
	 */
	private void allOutput(ArrayList<String> buffer) throws PipeException{

		for (int j = 0; j < buffer.size(); j++) {
			output((String)buffer.get(j));
		}
	}

	/**
	 * Output the first record of every "set of identical records", in other
	 * words, just records that have unique keys.
	 * <p>
	 * A "set of identical records" are consecutive records with identical
	 * key fields.
	 * <p>
	 * Subsequent records with identical key fields are discarded. There is no
	 * secondary output stream.
	 * <p>
	 * The COUNT and UNIQUE operands in SORT (and in other stages like UNIQUE),
	 * specify some action to take on sets of "duplicate records". The z/VM 
	 * manual says:
	 * <p>
	 *     "Records are considered to be identical if the specified 
	 *      keyfields contain the same data."
	 * <p>
	 * Be aware of how padding can alter the output for the COUNT 
	 * and UNIQUE operands, because it can change the "duplicate sets" found.
	 * <p>
	 * Using these input records:
	 *    "adam"
	 *    "adam "
	 * <p>
	 * "SORT UNIQUE 1-5" (no padding) will produce two output recs:
	 *    "adam"
	 *    "adam "
	 * <p>
	 * "SORT UNIQUE 1-5 PAD" will produce one output rec:
	 *    "adam"
	 * 
	 * @param buffer - all records after being sorted
	 * @throws PipeException 
	 */
	private void uniqueOutput(ArrayList<String> buffer) throws PipeException {

		String lastrec;
		String thisrec; 

		// eliminate trivial case
		if (buffer.isEmpty())
			return;

		// Prime the loop:
		// 1st record of buffer is always output, 
		// it's the 1st record in a set of identical records
		lastrec = (String) buffer.get(0);
		output(lastrec);

		// process rest of buffer, [1] through [N]
		for (int j = 1; j < buffer.size(); j++) {
                        thisrec = (String)buffer.get(j);

			if (thisStartsNewSet(lastrec, thisrec)) {
				output(thisrec);
				lastrec = thisrec; // THIS is now LAST 
			}
		}
	}

	/**
	 * Output the first record of every "set of identical records", prepended
	 * with a 10-character, right justified count of how many records are 
	 * in that set of identical records.
	 * <p>
	 * A "set of identical records" are consecutive records with identical
	 * key fields.
	 * <p>
	 * Subsequent records in the set are counted but discarded.
	 * There is no secondary output stream.
	 * <p>
	 * Also see uniqueOutput() for discussion of how padding can influence COUNT results.
	 * 
	 * @param buffer
	 * @throws PipeException 
	 */
	private void countOutput(ArrayList<String> buffer) throws PipeException {

		String thisrec;
		String outrec;
		int set_size;

		// eliminate trivial case
		if (buffer.isEmpty())
			return;

		// Prime the loop:
		// 1st rec in buffer starts a key set, set size is 1 so far
		outrec = (String) buffer.get(0);
		set_size = 1;

		// process rest of buffer, [1] thru [N]
		for (int j = 1; j < buffer.size(); j++) {
                        thisrec = (String)buffer.get(j);

			// If this is NOT the start of a new set of "identical" records,
			// ignore this record, just bump the count.
			if (! thisStartsNewSet(outrec, thisrec)) {
				set_size++;
				continue;
			}

			// New key encountered. Start of a new set.
			// Write out the current saved "1st record" with its final count.
			output(String.format("%10d" , set_size) + outrec);

			// Initialize new key set
			outrec = thisrec; // new "1st in set"
			set_size = 1;  // start count at 1
		}

		// Loop has ended with a set in progress.
		// Output it now, with its count.
		output(String.format("%10d" , set_size) + outrec);
	}

	/**
	 * Does this record start a new "set of identical records" in the sorted BUFFER?
	 *<p>
	 * Compare the last record with this record. If the keys are different, 
	 * then this record is the start of a new set of identical records.
	 * 
	 * @param lastrec
	 * @param thisrec
	 * @return boolean
	 * @throws PipeException 
	 */
	private boolean thisStartsNewSet(String lastrec, String thisrec) throws PipeException{

		final int LAST = 0; // corresponding order to parmlist
		final int THIS = 1;
		String[] keys;

		for (Comparator<String> c: keyFields) {
			KeyField kf = (KeyField) c;
			keys = kf.getKeys(lastrec, thisrec);

			// if THIS key is same as LAST key, have to try next key field
			if (keys[THIS].equals(keys[LAST])) 
				continue;

			// THIS key field is different from LAST, start of new set
			return true;
		}

		// All key fields of each record compared equal, THIS rec is not 
		// the start of a new set
		return false;
	}
}
