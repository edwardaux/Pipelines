package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 *                                          ┌─AT─────────────────────┐    
 *  ──SPLIT──┬─────────┬─┬────────────────┬─┼────────────────────────┼──>
 *           └─ANYCase─┘ └─MINimum number─┘ └─┬─────────┬─┬─BEFORE─┬─┘  
 *                                            └─snumber─┘ └─AFTER──┘ 
 *             ┌─BLANK────────────────────────────┐    
 *  >─┬─────┬──┼──────────────────────────────────┼──
 *    └─NOT─┘  └┬─xrange─────────────┬─┬────────┬─┘
 *              └─┬─STRing─┬─dstring─┘ └─number─┘
 *                └─ANYof──┘
 */
public class Split extends Stage {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		signalOnError();
		try {
			commit(-2);
			
			boolean anycase = false;
			int minimum = 0;
			boolean consumeTarget = true;
			boolean before = false;
			boolean after = false;
			int relative = 0;
			boolean not = false;
			int type = PipeUtil.TARGET_TYPE_ANYOF;
			String target = " ";
			char[] targetRange = null;
			int maxCount = Integer.MAX_VALUE;
			PipeArgs pa = new PipeArgs(args);
			
			String word = scanWord(pa);
			if (Syntax.abbrev("ANYCASE", word, 4)) {
				anycase = true;
				word = scanWord(pa);
			}
			
			if (Syntax.abbrev("MINIMUM", word, 3)) {
				word = scanWord(pa);
				if (!Syntax.isNumber(word))
					throw new PipeException(-58, word);
				minimum = PipeUtil.makeInt(word);
				if (minimum < 0)
					throw new PipeException(-287, word);
				word = scanWord(pa);
			}

			if (Syntax.abbrev("AT", word, 2)) {
				relative = 0;
				word = scanWord(pa);
			}
			else {
				boolean beforeOrAfterRequired = false;
				if (Syntax.isSignedNumber(word)) {
					beforeOrAfterRequired = true;
					relative = PipeUtil.makeInt(word);
					word = scanWord(pa);
				}
				if (Syntax.abbrev("BEFORE", word, 6)) {
					consumeTarget = false;
					before = true;
					word = scanWord(pa); 
				}
				else if (Syntax.abbrev("AFTER", word, 5)) {
					consumeTarget = false;
					after = true;
					word = scanWord(pa); 
				}
				else if (beforeOrAfterRequired) {
					// we should only get to here if the user entered 
					// an snumber, but didn't follow it with a BEFORE/AFTER
					throw new PipeException(-48, ""+relative, word);
				}
			}
			
			if (Syntax.abbrev("NOT", word, 3)) {
				not = true;
				word = scanWord(pa);
			}
			
			if (Syntax.abbrev("BLANK", word, 5)) {
				type = PipeUtil.TARGET_TYPE_ANYOF;
				target = " ";
				word = scanWord(pa);
			}
			else {
				if (Syntax.abbrev("STRING", word, 3)) {
					type = PipeUtil.TARGET_TYPE_STRING;
					target = scanString(pa, true);
					word = scanWord(pa);
				}
				else if (Syntax.abbrev("ANYOF", word, 3)) {
					type = PipeUtil.TARGET_TYPE_ANYOF;
					target = scanString(pa, true);
					word = scanWord(pa);
				}
				else if (Syntax.isXrange(word)) {
					type = PipeUtil.TARGET_TYPE_XRANGE;
					target = word;
					word = scanWord(pa);
				}
				
				if (Syntax.isNumber(word)) {
					maxCount = PipeUtil.makeInt(word);
					word = scanWord(pa);
				}
				
			}

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

			if (anycase)
				target = target.toUpperCase();
			if (type == PipeUtil.TARGET_TYPE_XRANGE)
				targetRange = PipeUtil.makeXrange(target);
			if (before)
				relative = -relative;
			
			while (true) {
				String orig = peekto();
				
				// split copies null records straight to the output
				if (orig.length() == 0) {
					output(orig);
				}
				else {
					String s = (anycase ? orig.toUpperCase() : orig);
					
					// represents the location that the last split
					// was found.  It doesn't care about BEFORE or AFTER
					// it just slowly advances through the string as the
					// targets are found.  This is where the next search
					// starts from (pending any MINIMUM requirements)
					int lastSplitIndex = 0;
					
					// represents where we should actually start writing 
					// the output string from.  It is aware of whether it
					// needs to deal with BEFORE and AFTER, and so on. 
					int writingFromIndex = 0;
					
					// how many matches have we had so far?
					int count = 0;
					
					while (lastSplitIndex < s.length()) {
						// if we've hit the maxcount, then we need to 
						// dump the rest of the record and get out.
						if (count == maxCount) {
							output(orig.substring(lastSplitIndex));
							break;
						}
						else {
							PipeUtil.Location location = PipeUtil.locateTarget(s, lastSplitIndex+minimum, type, target, targetRange, not, 0);
							if (!location.found) {
								output(orig.substring(writingFromIndex));
								break;
							}
							else {
								// If we are writing AFTER, then we need to advance 
								// past the end of the target string (ie. endIndex),
								// otherwise (BEFORE and AT), we need to deal with
								// the beginning of the target (startIndex)
								int endColumn = Math.min(orig.length(), Math.max(0, (after ? location.endIndex+1 : location.startIndex)+relative));
								
								// but we don't write blank strings...
								if (writingFromIndex != endColumn)
									output(orig.substring(writingFromIndex, endColumn));
							}
							// now move the split location past this target
							lastSplitIndex = location.endIndex+1;
							// and move the start of what we are supposed to 
							// write next time according to the BEFORE/AFTER
							// settings
							writingFromIndex = (consumeTarget ? location.endIndex+1 : (after ? location.endIndex+1 : location.startIndex))+relative;  
						}
						count++;
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
