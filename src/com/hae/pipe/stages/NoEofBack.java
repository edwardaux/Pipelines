package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 *  ──NOEOFBACK──
 */
public class NoEofBack extends Stage {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		signalOnError();
		try {
			commit(0);
			
			while (true) {
				String s = peekto();
				try {
					output(s);
				}
				catch(EOFException e) {
					// uh oh... eof while trying to write, so
					// now we consume all input...
					while (true) {
						readto();
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
