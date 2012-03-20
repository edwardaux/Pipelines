package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 *  ──┬─CONSole──┬──┬─────────────────────┬──
 *    └─TERMinal─┘  ├─EOF delimitedString─┤  
 *                  └─NOEOF───────────────┘  
 */
public class Console extends Stage {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		// TODO signal CONSOLE input and CONSOLE output events
		signalOnError();
		try {
			commit(-2);
			
			String eof = "";
			boolean noeof = false;
			
			PipeArgs pa = new PipeArgs(args);
			
			if (stageNum() == 1) {
				// reading input...
				String word = scanWord(pa);
				if (Syntax.abbrev("EOF", word, 3)) {
					eof = scanString(pa, true);
					word = scanWord(pa);
				}
				else if (Syntax.abbrev("NOEOF", word, 5)) {
					noeof = true;
					word = scanWord(pa);
				}

				if (!"".equals(word)) {
					// extra parameters
					return exitCommand(-112, word);
				}
				
				commit(0);
				
				java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
				try {
					while (true) {
						String s = reader.readLine();
						if (!noeof && eof.equals(s))
							break;
						else
							output(s);
					}
				}
				catch(java.io.IOException e) {
					throw new PipeException(-5001, e.getMessage());
				}
			}
			else {
				// writing output...
				if (!"".equals(pa.nextWord())) {
					// extra parameters
					return exitCommand(-112, args);
				}
				
				commit(0);

				while (true) {
					String s = peekto();
					System.out.println(s);
					if (streamState(OUTPUT, 0) == 0)
						output(s);
					readto();
				}
			}
		}
		catch(EOFException e) {
		}
		return 0;
	}
}
