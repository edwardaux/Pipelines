package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 *          
 *  ──LOCATE──┬─────────┬──┬───────┬──┬─────────────┬──┬───────┬──>
 *            └─ANYcase─┘  ├─MIXED─┤  └─inputRanges─┘  └─ANYof─┘
 *                         ├─ONEs──┤ 
 *                         └─ZEROs─┘ 
 *  >──┬─────────────────┬──
 *     └─delimitedString─┘
 *  
 */
public class Locate extends Stage {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		signalOnError();
		try {
			commit(-2);
			
			boolean anycase = false;
			boolean anyof = false;
			String delimitedString = "";
			PipeArgs pa = new PipeArgs(args);
			
			String word = scanWord(pa);
			if (Syntax.abbrev("ANYCASE", word, 3) ||
				Syntax.abbrev("CASEANY", word, 7) ||
				Syntax.abbrev("IGNORECASE", word, 10) ||
				Syntax.abbrev("CASEIGNORE", word, 10)) {
				anycase = true;
				word = scanWord(pa);
			}
			
			if (Syntax.abbrev("MIXED", word, 5)) {
				// ignored for now
			}
			else if (Syntax.abbrev("ONES", word, 3)) {
				// ignored for now
			}
			else if (Syntax.abbrev("ZEROS", word, 4)) {
				// ignored for now
			}
			else {
				// no case sensitivity or mask specified, so we push back
				// the word so that we can read the delimitedString
				pa.undo();
			}
			
			Range range = scanRange(pa, false);
			if (range == null)
				range = new RangeSingle(new PipeArgs("1-*"));
			
			word = scanWord(pa);
			if (Syntax.abbrev("ANYOF", word, 3))
				anyof = true;
			else {
				// nope, must be the delimited string
				pa.undo();
			}
			
			delimitedString = scanString(pa, false);
			
			word = scanWord(pa);
			if (!"".equals(word)) {
				// extra parameters
				return exitCommand(-112, word);
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
				boolean match = range.contains(s, delimitedString, anycase, anyof); 
				if (shouldNegate())
					match = !match;
				
				if (match) {
					select(OUTPUT, 0);
					output(s);
				}
				else {
					if (streamState(OUTPUT, 1) == 0) {
						select(OUTPUT, 1);
						output(s);
					}
				}
				readto();
			}
		}
		catch(EOFException e) {
		}
		return 0;
	}
	
	/**
	 * Support for LOCATE/NLOCATE.  If LOCATE, then we leave 
	 * the sign alone 
	 */
	protected boolean shouldNegate() {
		return false;
	}
}
