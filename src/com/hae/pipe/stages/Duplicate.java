package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 *               ┌─1───────┐   
 *  ──DUPlicate──┼─────────┼──
 *               ├─snumber─┤  
 *               └─*───────┘
 */
public class Duplicate extends Stage {
	public int execute(String args) throws PipeException {
		signalOnError();
		try {
			commit(-2);
			
			int count = 1;
			PipeArgs pa = new PipeArgs(args);
			
			String word = scanWord(pa);
			if (!"".equals(word)) {
				if (Syntax.isNumberOrStar(word, true)) {
					if (Syntax.isSignedNumber(word))
						count = PipeUtil.makeInt(word);
					else
						count = Integer.MAX_VALUE;

					// only negative number allowed is -1
					if (count < -1)
						return exitCommand(-66, word);
					word = scanWord(pa);
				}

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

			while (true) {
				String s = peekto();
				if (count != -1) {
					// output this record
					output(s);
					for (int i = 0; i < count; i++) {
						// and now output the copies
						output(s);
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
