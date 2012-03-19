package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 *           ┌─VERSION──┐   
 *  ──Query──┼──────────┼──
 *           ├─MSGLEVEL─┤  
 *           ├─MSGLIST──┤  
 *           └─LEVEL────┘
 */
public class Query extends Stage {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private static int TYPE_VERSION  = 86;
	private static int TYPE_MSGLEVEL = 186;
	private static int TYPE_MSGLIST  = 189;
	private static int TYPE_LEVEL    = 560;
	
	public int execute(String args) throws PipeException {
		signalOnError();
		try {
			commit(-4);

			// are we the first stage in the pipe?
			if (stageNum() != 1)
				return exitCommand(-87);

			PipeArgs pa = new PipeArgs(args);
			String word = pa.nextWord();
			
			int messageNo = TYPE_VERSION;
			String[] parms = new String[] {};
			String output = "";
			if ("".equals(word) || Syntax.abbrev("VERSION", word, 7)) {
				messageNo = TYPE_VERSION;
				parms = new String[] { Pipe.VERSION, Pipe.VERSION_DATE };
				output = Pipe.buildMessage(messageNo, "PIPQRY", parms);
			}
			else if (Syntax.abbrev("MSGLEVEL", word, 8)) {
				messageNo = TYPE_MSGLEVEL;
				parms = new String[] { ""+getOptions().msgLevel };
				output = PipeUtil.makeBinLength4(getOptions().msgLevel);
			}
			else if (Syntax.abbrev("MSGLIST", word, 7)) {
				messageNo = TYPE_MSGLIST;
				parms = new String[] { "" };
				output = "";
			}
			else if (Syntax.abbrev("LEVEL", word, 5)) {
				messageNo = TYPE_LEVEL;
				parms = new String[] { Pipe.VERSION, Pipe.VERSION_DATE };
				int v = Pipe.VERSION_LEVEL << 28;
				v += Pipe.VERSION_RELEASE << 24;
				v += Pipe.VERSION_MOD << 16;
				v += Pipe.VERSION_SERIAL;
				output = PipeUtil.makeBinLength4(v);
			}
			else
				return exitCommand(-111, word);
			
			// extra parameter check
			if (!"".equals(pa.getRemainder()))
				return exitCommand(-112, pa.getRemainder());
			
			commit(0);

			if (streamState(OUTPUT, 0) == 0)
				output(output);
			else
				issueMessage(messageNo, "PIPQRY", parms);
		}
		catch(EOFException e) {
		}
		return 0;
	}

}
