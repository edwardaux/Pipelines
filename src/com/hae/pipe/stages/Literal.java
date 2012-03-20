package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 * ──LITERAL──┬────────┬──
 *            └─string─┘
 */
public class Literal extends Stage {
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
