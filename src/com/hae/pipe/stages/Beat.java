package com.hae.pipe.stages;

import com.hae.pipe.*;

// TODO this class doesn't behave the way it should.  It is supposed to output a record every 'number' seconds. 
/**
 *          
 *  ──BEAT──┬───────┬─┬─number────────┬─┬─────────────────┬──
 *          └─ONCE──┘ └─number.number─┘ └─delimitedString─┘
 */
public class Beat extends Stage {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		signalOnError();
		try {
			commit(-2);
			
			boolean finished = false;
			boolean once = false;
			float milliseconds = 0;
			String missedBeatString = "";
			PipeArgs pa = new PipeArgs(args);
			
			String word = scanWord(pa);
			if (Syntax.abbrev("ONCE", word, 4)) {
				once = true;
				word = scanWord(pa);
			}
			
			if (Syntax.isDecimalNumber(word)) {
				milliseconds = PipeUtil.makeFloat(word) * 1000;
				word = scanString(pa, false);
			}
			else
				return exitCommand(-58, word);
			
			if (!"".equals(word)) {
				missedBeatString = word;
				word = scanWord(pa);
			}
			
			if (!"".equals(word)) {
				// extra parameters
				return exitCommand(-112, word);
			}

			// make sure that the primary input stream is connected
			if (streamState(INPUT, 0) != 0)
				return exitCommand(-102, "0");
			// make sure that the primary output stream is connected
			if (streamState(OUTPUT, 0) != 0)
				return exitCommand(-102, "0");
			
			// make sure no other input streams are connected
			int maxStream = maxStream(INPUT);
			for (int i = 1; i <= maxStream; i++) {
				streamState(INPUT, i);
				if (RC >= 0 && RC <= 8)
					return exitCommand(-264, ""+i);
			}
				
			commit(0);
			
			long lastRecordTime = System.currentTimeMillis();

			while (true) {
				String s = peekto();
				long now = System.currentTimeMillis();
				if (now - lastRecordTime > milliseconds) {
					if (!finished) {
						if (once)
							finished = true;
						select(OUTPUT, 1);
						output(missedBeatString);
					}
				}
				lastRecordTime = now;
				select(OUTPUT, 0);
				output(s);
				readto();
			}
		}
		catch(EOFException e) {
		}
		return 0;
	}
}
