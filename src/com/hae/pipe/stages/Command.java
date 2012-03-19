package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 *  ──┬─COMMAND─┬──┬────────┬──
 *    └─CMD─────┘  └─string─┘
 */
public class Command extends Stage {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		signalOnError();
		try {
			commit(-2);

			// make sure no other input streams are connected
			int maxStream = maxStream(INPUT);
			for (int i = 1; i <= maxStream; i++) {
				streamState(INPUT, i);
				if (RC >= 0 && RC <= 8)
					return exitCommand(-264, ""+i);
			}
				
			commit(0);

			// first run the command that is passed as an argument
			// (note that the runCommand() method will ignore it
			// if it is empty.
			runCommand(args);
			
			// now lets try to read the input records and run them
			while (true) {
				String s = peekto();
				runCommand(s);
				readto();
			}
		}
		catch(EOFException e) {
		}
		return 0;
	}
	
	private void runCommand(String command) throws PipeException {
		if ("".equals(command))
			return;
		
		try {
			Process process = Runtime.getRuntime().exec(command);
			java.io.BufferedReader inputReader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
		
			select(OUTPUT, 0);
			String line = inputReader.readLine();
			while (line != null) {
				output(line);
				line = inputReader.readLine();
			}
			inputReader.close();
			
			int rc = process.exitValue();
			if (streamState(OUTPUT, 1) == 0) {
				select(OUTPUT, 1);
				output(""+rc);
			}
			else if (rc < 0)
				throw new PipeException(-5001, "Command \""+command+"\" terminated with rc="+rc);
		}
		catch(java.io.IOException e) {
			throw new PipeException(-5001, e.getMessage()); 
		}
	}
}
