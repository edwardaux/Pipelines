package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 *  ──REVERSE──
 */
public class Reverse extends Stage {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		signalOnError();
		try {
			commit(-2);
			
			PipeArgs pa = new PipeArgs(args);
			
			String word = pa.nextWord();
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
				output(new StringBuffer(s).reverse().toString());
				readto();
			}
		}
		catch(EOFException e) {
		}
		return 0;
	}
}
