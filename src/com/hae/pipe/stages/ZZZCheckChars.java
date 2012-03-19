package com.hae.pipe.stages;

import com.hae.pipe.*;

public class ZZZCheckChars extends Stage {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		signalOnError();
		String s = "?";
		int i = -1;
		String[] tokens = args.trim().substring(1).split("/");
		if (tokens.length == 0) {
			tokens = new String[1];
			tokens[0] = "";
		}
		try {
			commit(0);
			s = peekto();
			i = 0;
			while (true) {
				if (!s.equals(tokens[i]))
					throw new IllegalArgumentException("Looking for \""+tokens[i]+"\", but found \""+s+"\"");
				readto();
				s = peekto();
				i++;
				if (i == tokens.length)
					throw new IllegalArgumentException("Too many records.  Last record read: \""+s+"\"");
			}
		}
		catch(EOFException e) {
			if (i < tokens.length-1) {
				if (i == -1)
					i = 0;
				throw new IllegalArgumentException("Not enough records for ("+args+").  Next expected record: \""+tokens[i]+"\"");
			}
			return 0;
		}
	}

}
