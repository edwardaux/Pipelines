package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 *                  ┌─80─────┐   
 *  ──┬─CHOP─────┬──┼────────┼──
 *    └─TRUNCate─┘  └─number─┘
 *
 *    
 *  ──CHOP──┬─────────┬──┬────────────────────────┬──┬─xrange─────────────┬──
 *          └─ANYCASE─┘  │             ┌─BEFORE─┐ │  └─┬─STRing─┬─dstring─┘
 *                       └─┬─────────┬─┼────────┼─┘    └─ANYof──┘
 *                         └─snumber─┘ └─AFTER──┘
 */
public class Chop extends Stage {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		signalOnError();
		try {
			commit(-2);
			
			PipeArgs pa = new PipeArgs(args);
			
			boolean anycase = false;
			int chopOffset = 0;
			boolean before = true;
			boolean not = false;
			int type = PipeUtil.TARGET_TYPE_XRANGE;
			String target = null;
			char[] targetRange = null;
			int column = 80;
			String word = scanWord(pa);
			if (word.equals("")) {
				type = PipeUtil.TARGET_TYPE_COLUMN;
				column = 80;
			}
			else if (Syntax.isNumber(word) && pa.getRemainder().equals("")) {
				type = PipeUtil.TARGET_TYPE_COLUMN;
				column = PipeUtil.makeInt(word);
			}
			else {
				if (Syntax.abbrev("ANYCASE", word, 4) ||       // note special case of 4 for CHOP (because of ANYof)
					Syntax.abbrev("CASEANY", word, 7) ||
					Syntax.abbrev("IGNORECASE", word, 10) ||
					Syntax.abbrev("CASEIGNORE", word, 10)) {
					anycase = true;
					word = scanWord(pa);
				}
				
				if (Syntax.isSignedNumber(word)) {
					chopOffset = PipeUtil.makeInt(word);
					word = scanWord(pa);
				}
				
				if (Syntax.abbrev("BEFORE", word, 6)) {
					before = true;
					word = scanWord(pa);
				}
				else if (Syntax.abbrev("AFTER", word, 5)) {
					before = false;
					word = scanWord(pa);
				}
				
				if (Syntax.abbrev("NOT", word, 3)) {
					not = true;
					word = scanWord(pa);
				}
				
				if (Syntax.abbrev("STRING", word, 3)) {
					type = PipeUtil.TARGET_TYPE_STRING;
					target = scanString(pa, true);
				}
				else if (Syntax.abbrev("ANYOF", word, 3)) {
					type = PipeUtil.TARGET_TYPE_ANYOF;
					target = scanString(pa, true);
				}
				else {
					if (Syntax.isXrange(word)) {
						type = PipeUtil.TARGET_TYPE_XRANGE;
						target = word;
					}
					else {
						return exitCommand(-54, word);
					}
				}
				
				word = scanWord(pa);
				if (!"".equals(word)) {
					// extra parameters
					return exitCommand(-112, word);
				}
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

			if (anycase)
				target = target.toUpperCase();
			if (type == PipeUtil.TARGET_TYPE_XRANGE)
				targetRange = PipeUtil.makeXrange(target);
			
			while (true) {
				String orig = peekto();
				String s = (anycase ? orig.toUpperCase() : orig);
				
				PipeUtil.Location location = PipeUtil.locateTarget(s, 0, type, target, targetRange, not, column);
				
				// if not found, then we don't actually chop anything, otherwise
				// we set the initial index to the start of the target (because
				// the next bit of logic handles moving it along to the end)
				int endColumn = (location.found ? location.startIndex : s.length());

				// now apply the offset
				if (before)
					endColumn -= chopOffset;
				else
					endColumn += chopOffset+target.length();
				
				select(OUTPUT, 0);
				String[] results = PipeUtil.split(orig, Math.max(0, endColumn));
				output(results[0]);
				if (streamState(OUTPUT, 1) == 0) {
					select(OUTPUT, 1);
					output(results[1]);
				}
				readto();
			}
		}
		catch(EOFException e) {
		}
		return 0;
	}
}
