package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 *          ┌─FIRST──┐  ┌─1──────┐   
 *  ──TAKE──┼────────┼──┼────────┼──┬───────┬──
 *          └─LAST───┘  ├─number─┤  └─BYTES─┘
 *                      └─*──────┘
 */
public class Take extends Stage {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		signalOnError();
		try {
			commit(-2);
			
			boolean first = true;
			int takeCount = 1;
			boolean bytes = false;
			PipeArgs pa = new PipeArgs(args);
			
			String word = scanWord(pa);
			if (Syntax.abbrev("LAST", word, 4)) {
				first = false;
				word = scanWord(pa);
			}
			
			if (Syntax.abbrev("FIRST", word, 4)) {
				first = true;
				word = scanWord(pa);
			}
			
			if (Syntax.isNumberOrStar(word, true)) {
				if (Syntax.isSignedNumber(word))
					takeCount = PipeUtil.makeInt(word);
				else
					takeCount = Integer.MAX_VALUE;
				// can't be negative
				if (takeCount < 0)
					return exitCommand(-287, word);
				word = scanWord(pa);
			}
			
			if (Syntax.abbrev("BYTES", word, 5)) {
				bytes = true;
				word = scanWord(pa);
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

			int taken = 0;
			String spare = "";
			if (first) {
				while (taken < takeCount) {
					String s = peekto();
					if (bytes) {
						// will this string throw us over the edge?
						if (taken + s.length() > takeCount) {
							// yup, so only output the bit that gets us to the limit
							output(s.substring(0, takeCount - taken));
							// and hang on to the rest to be written to the output stream
							spare = s.substring(takeCount - taken);  // hang on to the rest
						}
						else {
							// nope... still under the limit. output the whole record
							output(s);
						}
						taken += s.length();
					}
					else {
						// if we're counting records we just output the 
						// whole record (we couldn't be in this clause if 
						// we have already written takeCount records)
						output(s);
						taken += 1;
					}
					
					// now consume it...
					readto();
					
					// only go again if we haven't hit the limit
					if (taken < takeCount)
						s = peekto();
				}
				
				// now lets send the rest to the secondary output stream
				select(OUTPUT, 1);
				// first, though we need to check to see if it is connected
				if (RC == 0) {
					// is there anything left over if we were processing bytes?
					if (spare.length() != 0)
						output(spare);
					shortStreams();
				}
			}
			else {
				// TODO LAST
			}
		}
		catch(EOFException e) {
		}
		return 0;
	}
}
