package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 *            ┌─STOP──ALLEOF───┐     
 *  ──FANOUT──┼────────────────┼──
 *            └─STOP─┬─ANYEOF──┤  
 *                   └─number──┘
 */
public class Fanout extends Stage {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		signalOnError();
		try {
			commit(-2);

			int streamCount = maxStream(OUTPUT)+1;
			int stopCount = streamCount;
			PipeArgs pa = new PipeArgs(args);
			
			String word = scanWord(pa);
			if (Syntax.abbrev("STOP", word, 4)) {
				word = scanWord(pa);
				if (Syntax.abbrev("ALLEOF", word, 6))
					stopCount = maxStream(OUTPUT);
				else if (Syntax.abbrev("ANYEOF", word, 6))
					stopCount = 1;
				else if (Syntax.isNumber(word))
					stopCount = PipeUtil.makeInt(word);
				else
					return exitCommand(-58, word);
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

			while (true) {
				String s = peekto();
				int disconnectedCount = 0;
				for (int i = 0; i < streamCount; i++) {
					try {
						select(OUTPUT, i);
						output(s);
					}
					catch(EOFException e) {
						disconnectedCount++;
					}
				}
				if (disconnectedCount >= stopCount) {
					break;
				}
				else {
					readto();
				}
			}
		}
		catch(EOFException e) {
		}
		return 0;
	}
}
