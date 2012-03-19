package com.hae.pipe.stages;

import java.util.*;

import com.hae.pipe.*;

/**
 * ──STEM──word──┬──────────────┬──
 *               ├─APPEND───────┤
 *               └─FROM──number─┘
 */
public class Stem extends Stage {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		signalOnError();
		try {
			commit(-1);

			boolean firstStage = (stageNum() == 1);
			boolean append = false;
			// note that the public API for the offset is 1-based, but
			// internally, we use 0-based offsets
			int     startOffset = 0;
			
			// make sure we have a stem name
			PipeArgs pa = new PipeArgs(args);
			String stemName = scanWord(pa);
			if ("".equals(stemName))
				return exitCommand(-231);
			
			String arg = scanWord(pa);
			if (Syntax.abbrev("APPEND", arg, 6)) {
				append = true;
				arg = scanWord(pa);
			}
			if (Syntax.abbrev("FROM", arg, 4)) {
				arg = scanWord(pa);
				if (Syntax.isNumber(arg)) {
					// convert to 0-based offset
					startOffset = Integer.parseInt(arg)-1;
					arg = scanWord(pa);
				}
				else {
					// invalid number
					return exitCommand(-58, arg); 
				}
			}
			if (!"".equals(arg)) {
				// extra parameters
				return exitCommand(-112, arg);
			}

			// make sure APPEND and FROM aren't both passed
			if (append && startOffset != 0)
				return exitCommand(-14, "APPEND and FROM");
				
			commit(0);

			// if first in the pipe, then we read, otherwise
			// we are writing to the collection
			if (firstStage) {
				// first stage, so we output records
				Object collection = getParameter(stemName);
				if (collection == null) {
					// null input, so we don't need to output anything
					return 0;
				}
				else if (collection instanceof List) {
					// handle a java.io.List
					List list = (List)collection;
					for (int i = startOffset; i < list.size(); i++) {
						Object o = list.get(i);
						output(o == null ? "" : o.toString());
					}
				}
				else if (collection instanceof Object[]) {
					// handle an array
					Object[] list = (Object[])collection;
					for (int i = startOffset; i < list.length; i++) {
						Object o = list[i];
						output(o == null ? "" : o.toString());
					}
				}
				else {
					// unknown data type
					return exitCommand(-235, stemName+"("+collection.getClass().getName()+")");
				}
			}
			else {
				// we are writing to a stem...
				Object collection = getParameter(stemName);
				if (collection == null)
					collection = new ArrayList();

				// note that although we can read from arrays of
				// objects, we only ever write to a List.
				if (collection instanceof List) {
					putParameter(stemName, collection);
					
					List list = (List)collection;
					
					// now if we are supposed to start writing at an
					// offset that is greater than the number of current
					// records, then we fill the list until it has enough
					while (startOffset > list.size())
						list.add(null);

					// are we connected to an output stream?
					boolean shouldWrite = (streamState(OUTPUT, 0) == 0);
					
					int i = startOffset;
					String s = peekto();
					while (true) {
						// store into the list...
						if (i >= list.size())
							list.add(s);
						else
							list.set(i, s);

						// and write to output stream if possible
						if (shouldWrite)
							output(s);
						
						// consume the record and wait for the next one
						readto();	
						s = peekto();
						i++;
					}
				}
				else {
					// unknown data type
					return exitCommand(-235, stemName+"("+collection.getClass().getName()+")");
				}
			}
		}
		catch(EOFException e) {
		}
		return 0;
	}
}
