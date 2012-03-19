package com.hae.pipe.stages;

import java.io.*;

import com.hae.pipe.*;
import com.hae.pipe.EOFException;

/**
 * ──DISKR──filename──
 */
public class Diskr extends Stage {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";

	public int execute(String args) throws PipeException {
		signalOnError();

		commit(-2000000000);

		// make sure we have a file name
		PipeArgs pa = new PipeArgs(args);
		String fileName = scanWord(pa);
		if ("".equals(fileName))
			return exitCommand(-113);

		// extra parameter check
		if (!"".equals(pa.getRemainder()))
			return exitCommand(-112, pa.getRemainder());
		
		// does the file exist?
		File file = new File(fileName);
		if (!file.exists()) {
			// nope, let's try upper case
			file = new File(fileName.toUpperCase());
			if (!file.exists())
				return exitCommand(-146, fileName);
		}
		
		// are we the first stage in the pipe?
		if (stageNum() != 1)
			return exitCommand(-87);
		
		// everything looks OK at this point, so we can commit
		commit(0);

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();
			while (line != null) {
				output(line);
				line = reader.readLine();
			}
		}
		catch(IOException e) {
			return exitCommand(-5002, e.getMessage());
		}
		catch(EOFException e) {
		}
		finally {
			try {
				reader.close();
			}
			catch(IOException e) {
				return exitCommand(-5002, e.getMessage());
			}
		}
		return 0;
	}

}
