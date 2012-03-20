package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 *  ──FANINANY──┬────────┬──
 *              └─STRICT─┘
 */
public class Faninany extends Stage {
	public int execute(String args) throws PipeException {
		signalOnError();
		try {
			commit(-2);
			
			// make sure that the primary output stream is connected
			if (streamState(OUTPUT, 0) != 0)
				return exitCommand(-102, "0");
			
			// make sure no other output streams are connected
			int maxStream = maxStream(OUTPUT);
			for (int i = 1; i <= maxStream; i++) {
				streamState(OUTPUT, i);
				if (RC >= 0 && RC <= 8)
					return exitCommand(-264, ""+i);
			}
				
			commit(0);
			
			while (true) {
				select(INPUT, ANYINPUT);
				String s = peekto();
				output(s);
				readto();
			}
		}
		catch(EOFException e) {
		}
		return 0;
	}
}
