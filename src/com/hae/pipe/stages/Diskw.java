package com.hae.pipe.stages;

import java.io.*;

import com.hae.pipe.*;
import com.hae.pipe.EOFException;

/**
 * ──DISKW──filename──
 */
public class Diskw extends Stage {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
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
		
		// are we the first stage in the pipe?
		if (stageNum() == 1)
			return exitCommand(-127);
		
		// everything looks OK at this point, so we can commit
		commit(0);

		// does the file exist?
		File outputFile = new File(fileName);
		File tmpFile = outputFile;
		if (outputFile.exists() && !append()) {
			try {
				tmpFile = File.createTempFile("PIP", "");
			}
			catch(IOException e) {
				throw new PipeException(-740, "DISKW temporary file");
			}
		}
		
		BufferedWriter writer = null; 
		try {
			writer = new BufferedWriter(new FileWriter(tmpFile, append()));
			while (true) {
				String s = peekto();
				try {
					// try and be polite and check to see if the output 
					// is still connected.
					if (streamState(OUTPUT, 0) == 0)
						output(s);
				}
				catch(EOFException e) {
					// the output stage may have propogated EOF back
					// between when we checked it and when we called output()
					// But that is OK, we just keep on chugging...
				}
				writer.write(s);
				writer.newLine();
				readto();
			}
		}
		catch(IOException e) {
			return exitCommand(-5003, e.getMessage());
		}
		catch(EOFException e) {
		}
		finally {
			try {
				writer.flush();
				writer.close();
				if (outputFile != tmpFile) {
					if (!outputFile.delete())
						return exitCommand(-5003, "Unable to delete file: "+outputFile);
						
					if (!tmpFile.renameTo(outputFile))
						return exitCommand(-5003, "Unable to rename file: "+tmpFile+" to: "+outputFile);
				}
			}
			catch(IOException e) {
				return exitCommand(-5003, e.getMessage());
			}
		}
		
		return 0;
	}
	
	protected boolean append() {
		return false;
	}

}
