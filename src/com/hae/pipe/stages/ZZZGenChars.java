package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 * This is a test stage that is used to reliably generate test data.  It takes a number
 * of records to product, and outputs "number" records with a one-char string.  The first
 * string is "a", the second is "b", and so on 
 * 
 *  ──ZZZGEN──number──
 */
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
