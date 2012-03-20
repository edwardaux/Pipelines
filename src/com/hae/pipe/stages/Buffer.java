package com.hae.pipe.stages;

import java.util.*;

import com.hae.pipe.*;

/**
 *          
 *  ──BUFFER──┬───────────────────────────────┬──
 *            └──number─┬───────────────────┬─┘
 *                      └──delimitedString──┘
 */
public class Buffer extends Stage {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		signalOnError();
		ArrayList<String> buffer = new ArrayList<String>();
		int number = 1;
		String delimitedString = "";
		try {
			commit(-2);
			
			boolean hasParms = false;
			PipeArgs pa = new PipeArgs(args);
			
			String word = scanWord(pa);
			if (!"".equals(word)) {
				// check and see if it is a number
				if (Syntax.isNumber(word)) {
					// yep, so therefore we have params
					hasParms = true;
					number = PipeUtil.makeInt(word);
					if (number <= 0)
						return exitCommand(-66, word);
					
					// and now look to see if there is delimited string
					word = scanString(pa, false);
					if (!"".equals(word)) {
						delimitedString = word;

						word = scanWord(pa);
						if (!"".equals(word)) {
							// extra parameters
							return exitCommand(-112, word);
						}
					}
				}
				else {
					// not a number
					return exitCommand(-58, word);
				}
			}

			// make sure that the primary input stream is connected
			if (streamState(INPUT, 0) != 0)
				return exitCommand(-102, "0");
			
			// make sure no other input streams are connected
			int maxStream = maxStream(INPUT);
			for (int i = 1; i <= maxStream; i++) {
				streamState(INPUT, i);
				if (RC >= 0 && RC <= 8)
					return exitCommand(-264, ""+i);
			}
				
			commit(0);
			
			if (hasParms) {
				while (true) {
					// just keep on consuming them until EOF, and rely
					// on the EOFException getting thrown...
					buffer.add(readto());
				}
			}
			else {
				while (true) {
					String s = peekto();
					if ("".equals(s)) {
						// null record, so lets firstly consume it and
						// then write some records...
						readto();
						// dump the current contents of the buffer
						dumpBuffer(buffer, number, delimitedString);
						// and lastly, lets write out the null record
						output(s);
					}
					else {
						// a normal record.  Let's add it to the buffer
						buffer.add(s);					
						readto();
					}
				}
			}
		}
		catch(EOFException e) {
			// EOF received, so let's try to write the buffered records
			try {
				dumpBuffer(buffer, number, delimitedString);
			}
			catch(EOFException e2) {
				// uh, oh... output is not connected.  That's OK though
			}
		}
		return 0;
	}
	
	private void dumpBuffer(ArrayList<String> buffer, int number, String delimitedString) throws PipeException{
		for (int i = 0; i < number; i++) {
			// if it is the second (and subsequent) group
			// of records, we write a leading delimitedString
			if (i != 0)
				output(delimitedString);
			// now, let's dump the group
			for (int j = 0; j < buffer.size(); j++)
				output((String)buffer.get(j));
		}
		buffer.clear();
	}
	
}
