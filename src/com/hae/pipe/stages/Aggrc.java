package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 * ──AGGRC──
 */
public class Aggrc extends Stage {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		signalOnError();
		int aggrc = 0;
		try {
			commit(-2);
			
			PipeArgs pa = new PipeArgs(args);
			String word = scanWord(pa);
			
			if (!"".equals(word)) {
				// extra parameters
				return exitCommand(-112, word);
			}

			// if we don't have any input records, then we quit without writing anything
			if (streamState(INPUT, 0) != 0)
				return exitCommand(0);
			
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
				// check to see if it is a number
				if (!Syntax.isSignedNumber(s.trim()))
					return exitCommand(-58, s);
				
				int rec = PipeUtil.makeInt(s);
				if (rec < 0 || aggrc < 0)
					aggrc = Math.min(rec, aggrc);
				else
					aggrc = Math.max(rec, aggrc);
				readto();	
			}
		}
		catch(EOFException e) {
			// if our output stream is connected, then write the result...
			if (streamState(OUTPUT, 0) == 0) {
				output(""+aggrc);
				return 0;
			}
			else
				return exitCommand(aggrc);
		}
	}
}
