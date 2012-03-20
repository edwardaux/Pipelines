package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 *          
 *  ──BETWEEN──┬─────────┬──delimitedString──┬─number──────────┬──
 *             └─ANYCASE─┘                   └─delimitedString─┘
 *  
 */
public class Between extends Stage {
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
			
			String firstString = scanString(pa, true);
			
			// set aside some variables...
			String secondString = null;
			int desiredCount = 0;
			
			// now check to see if the second parm is a number 
			// or another delimited string
			word = scanWord(pa);
			if (Syntax.isNumber(word)) {
				desiredCount = PipeUtil.makeInt(word);
				if (desiredCount < 2)
					return exitCommand(-66, ""+desiredCount);
			}
			else {
				// not a number, so must be a delimited string
				pa.undo();
				try {
					secondString = scanString(pa, true);
				}
				catch(PipeException e) {
					return exitCommand(-211, word);
				}
			}
			
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
			
			int counted = 0;
			boolean between = false;
			while (true) {
				String s = peekto();
				
				// if we aren't yet between the desired markers
				// then we have to look to see if the current
				// record matches.
				if (!between) {
					if (Syntax.startsWith(s, firstString, anycase)) {
						// yep, now we are in between, so we can start
						// to write to the primary output stream...
						between = true;
						select(OUTPUT, 0);
						output(s);
						counted++;
					}
					else {
						// nope, still not between, so we write to
						// the secondary output (if connected). Otherwise
						// we just ignore it...
						if (streamState(OUTPUT, 1) == 0) {
							select(OUTPUT, 1);
							output(s);
						}
					}
				}
				else {
					select(OUTPUT, 0);
					output(s);

					// so, we know that we are somewhere in between... perhaps we 
					// have hit the delimiting record?
					if (secondString == null) {
						// check  the number of records we have passed so far. If
						// this is the last one, then output it and set to not between
						if (counted == desiredCount-1) { 
							// still under our limit
							between = false;
							counted = 0;
						}
						else
							counted++;
					}
					else {
						if (Syntax.startsWith(s, secondString, anycase)) {
							between = false;
							counted = 0;
						}
					}
				}
				readto();
			}
		}
		catch(EOFException e) {
		}
		return 0;
	}
	
}
