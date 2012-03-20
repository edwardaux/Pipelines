package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 * ──NOT──word──┬────────┬──
 *              └─string─┘
 */
public class Not extends Stage {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		signalOnError();

		commit(-2);

		if (args.trim().equals(""))
			return exitCommand(-17);
		
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
			
		if (maxStream(OUTPUT) == 0)
			return callpipe("(end ?) *: | n: "+args+" | hole ? n: | *.OUTPUT.0:");
		else
			return callpipe("(end ?) *: | n: "+args+" | *.OUTPUT.1: ? n: | *.OUTPUT.0:");
	}
}
