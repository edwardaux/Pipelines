package com.hae.pipe.stages;

import com.hae.pipe.*;

public class ZZZGenChars extends Stage {
	private String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	public int execute(String args) throws PipeException {
		int count = Integer.parseInt(args.trim());
		try {
			commit(0);
			for (int i = 0; i < count; i++)
				output(""+chars.charAt(i%52));
		}
		catch(EOFException e) {
		}
		return 0;
	}

}
