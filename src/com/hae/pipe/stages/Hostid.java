package com.hae.pipe.stages;

import java.net.*;

import com.hae.pipe.*;

/**
 * ──HOSTID──
 */
public class Hostid extends Stage {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		signalOnError();
		try {
			commit(-10);

			// are we the first stage in the pipe?
			if (stageNum() != 1)
				return exitCommand(-87);

			PipeArgs pa = new PipeArgs(args);
			// extra parameter check
			if (!"".equals(pa.getRemainder()))
				return exitCommand(-112, pa.getRemainder());
			
			String address;
			try {
				address = InetAddress.getLocalHost().getHostAddress();
			}
			catch(UnknownHostException e) {
				return exitCommand(-1134, "localhost");
			}
			
			// everything looks OK at this point, so we can commit
			commit(0);

			output(address);
		}
		catch(EOFException e) {
		}
		return 0;
	}

}
