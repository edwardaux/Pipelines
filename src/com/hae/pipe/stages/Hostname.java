package com.hae.pipe.stages;

import java.net.*;

import com.hae.pipe.*;

/**
 * ──HOSTNAME──
 */
public class Hostname extends Stage {
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
				address = InetAddress.getLocalHost().getHostName();
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
