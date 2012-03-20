package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 *          
 *  ──CHANGE──┬─────────┬──┬───────────────┬──>
 *            └─ANYCASE─┘  ├───inputRange──┤
 *                         │   ┌───────┐   │
 *                         └─(─┴─range─┴─)─┘
 *  
 *  >──delimitedString─delimitedString──┬───────────┬──
 *                                      └─numorstar─┘
 *  
 */
public class Change extends Stage {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		signalOnError();
		try {
			commit(-2);
			
			boolean anycase = false;
			PipeArgs pa = new PipeArgs(args);
			
			String word = scanWord(pa);
			if (Syntax.abbrev("ANYCASE", word, 3) ||
				Syntax.abbrev("CASEANY", word, 7) ||
				Syntax.abbrev("IGNORECASE", word, 10) ||
				Syntax.abbrev("CASEIGNORE", word, 10)) {
				anycase = true;
			}
			else {
				// no case sensitivity specified, so we push back
				// the word so that we can read the delimitedString
				pa.undo();
			}
			
			Range range = scanRange(pa, false);
			if (range == null)
				range = new RangeSingle(new PipeArgs("1-*"));
			
			
			String from;
			String to;
			try {
				from = scanString(pa, true);
				to = scanString(pa, true);
			}
			catch(PipeException e) {
				throw new PipeException(-113);
			}
			int count;
			
			// now lets validate the count...
			word = scanWord(pa);
			if (Syntax.isNumberOrStar(word, false)) {
				if (word.equals("*"))
					count = -1;
				else
					count = PipeUtil.makeInt(word);
			}
			else if (word.equals("")) {
				count = -1;
			}
			else {
				return exitCommand(-66, word);
			}

			if (from.equals("") && count != 1 && count != -1)
				return exitCommand(-198, ""+count);
			
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
				String orig = peekto();
				Range.ReplaceResult result = range.replace(orig, from, to, count, anycase);
				
				// if secondary output is connected, then we write
				//    stream 0: changed
				//    stream 1: unchanged
				// else
				//    stream 0: both
				if (streamState(OUTPUT, 1) == 0) {
					if (result.numberOfChanges != 0)
						select(OUTPUT, 0);
					else
						select(OUTPUT, 1);
					output(result.changed);
				}
				else {
					select(OUTPUT, 0);
					output(result.changed);
				}
				readto();
			}
		}
		catch(EOFException e) {
		}
		return 0;
	}
}
