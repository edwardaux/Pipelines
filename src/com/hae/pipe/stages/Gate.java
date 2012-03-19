package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 *  ──GATE──┬────────┬──
 *          └─STRICT─┘
 */
public class Gate extends Stage {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		signalOnError();
		try {
			commit(-2);
			
			boolean strict = false;
			PipeArgs pa = new PipeArgs(args);
			
			String word = scanWord(pa);
			if (Syntax.abbrev("STRICT", word, 6)) {
				strict = true;
				word = scanWord(pa);
			}
			
			if (!"".equals(word)) {
				// extra parameters
				return exitCommand(-112, word);
			}

			// make sure that the primary input stream is connected
			if (streamState(INPUT, 0) != 0)
				return exitCommand(-102, "0");
			
			commit(0);

			while (true) {
				select(INPUT, ANYINPUT);
				String s = peekto();
				int selectedInputStream = streamNum(INPUT);
				
				// so, if it was a record on the primary stream
				// then lets just short the two primary streams 
				// and bail out...
				if (selectedInputStream == 0) {
					select(BOTH, 0);
					shortStreams();
					break;
				}
				
				if (strict) {
					// TODO for now, we don't support STRICT
					select(OUTPUT, selectedInputStream);
					output(s);
				}
				else {
					select(OUTPUT, selectedInputStream);
					output(s);
				}
				
				// now consume the record
				readto();
			}
		}
		catch(EOFException e) {
		}
		return 0;
	}
}
