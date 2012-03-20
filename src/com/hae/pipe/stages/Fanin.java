package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 *  ──FANIN──┬──────────────┬──>
 *           │  ┌────────┐  │
 *           └──┴─stream─┴──┘
 */
public class Fanin extends Stage {
	public int execute(String args) throws PipeException {
		signalOnError();
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
		
		args = args.trim();
		if (args.equals("")) {
			// no arguments, so we iterate over all streams
			maxStream = maxStream(INPUT);
			for (int i = 0; i <= maxStream; i++) {
				try {
					select(INPUT, i);
					String s = peekto();
					while (true) {
						output(s);
						readto();
						s = peekto();
					}
				}
				catch(EOFException e) {
					// eof on this stream, so move to the next one
				}
			}
		}
		else {
			// user passed in a collection of streams
			PipeArgs pa = new PipeArgs(args);
			String stream = pa.nextWord();
			while (!stream.equals("")) {
				try {
					select(INPUT, stream);
					String s = peekto();
					while (true) {
						output(s);
						readto();
						s = peekto();
					}
				}
				catch(EOFException e) {
					// eof on this stream, so move to the next one
				}
				stream = pa.nextWord();
			}
		}
		return 0;
	}
}
