package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 *  ──HOLE──
 */
public class Hole extends Stage {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		signalOnError();
		
		commit(0);
		
		int maxStream = maxStream(INPUT);
		for (int i = 0; i <= maxStream; i++) {
			select(INPUT, i);
			try {
				while (true)
					readto();
			}
			catch(EOFException e) {
			}
		}
		return 0;
	}
}
