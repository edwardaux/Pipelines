package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 * ──LITERAL──┬────────┬──
 *            └─string─┘
 */
public class Literal extends Stage {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		signalOnError();
		try {
			commit(0);
			output(args);
			shortStreams();
		}
		catch(EOFException e) {
		}
		return 0;
	}
}
