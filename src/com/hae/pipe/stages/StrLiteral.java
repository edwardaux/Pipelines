package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 *  ──STRLITERAL──┬─────────────────────────────────────┬──┬──────────────────┬─
 *                │   ┌─PREFACE──┐                      │  └─delimitedString──┘
 *                └─┬─┼──────────┼──┬──────────────┬──┬─┘
 *                  │ └─APPEND───┘  └─CONDitional──┘  │
 *                  └─IFEMPTY─────────────────────────┘
 */
public class StrLiteral extends Stage {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		signalOnError();
		try {
			commit(-2);
			
			boolean preface = true;
			boolean conditional = false;
			boolean ifempty = false;
			String delimitedString = "";
			PipeArgs pa = new PipeArgs(args);
			
			String word = scanWord(pa);
			if (Syntax.abbrev("IFEMPTY", word, 7)) {
				ifempty = true;
				word = scanWord(pa);
			}
			else {
				if (Syntax.abbrev("PREFACE", word, 7)) {
					preface = true;
					word = scanWord(pa);
				}
				else if (Syntax.abbrev("APPEND", word, 6)) {
					preface = false;
					word = scanWord(pa);
				}
				
				if (Syntax.abbrev("CONDITIONAL", word, 4)) {
					conditional = true;
					word = scanWord(pa); 
				}
			}
			
			// so, we've finished looking for keywords, so we need to
			// put back the last word we've read because we need to look
			// for a delimited string.
			pa.undo();
			delimitedString = pa.nextDelimString(false);
			
			commit(0);
			
			if (ifempty) {
				// only write the record if there are no input records
				String s = null;
				try {
					s = peekto();
					// oops... if we got here, then there are input records,
					// (ie. s will be non-null)
				}
				catch(EOFException e) {
					output(delimitedString);
				}

				// so, was there an input record?  
				if (s != null) {
					// we will write the one we just read (but NOT write
					// the delimitedString) and short the rest
					output(s);
					readto();  // consume the one we just read
					shortStreams();
				}
				// no need to short the rest because we don't actually
				//have any input.
			}
			else {
				if (preface) {
					// The most obvious incantation is "STRLITERAL /blah/", in which case,
					// we can write the record and short the rest (just like LITERAL)
					if (!conditional) {
						output(delimitedString);
						shortStreams();
					}
					else {
						// OK, so we have to check for an input record first...
						String s = null;
						try {
							s = peekto();
							// yep, input, so s will be non-null
						}
						catch(EOFException e) {
							// nope, no input
						}
						
						if (s != null) {
							// so, we found an input record, so we need to firstly
							// write our delimitedString, then the record we read,
							// and then we can short the streams.
							output(delimitedString);
							output(s);
							readto();  // consume the one we just read
							shortStreams();
						}
					}
				}
				else {
					// worst case scenario is APPEND because we can't short the streams
					boolean found = false;
					try {
						while (true) {
							String s = peekto();
							found = true;
							output(s);
							readto();
						}
					}
					catch(EOFException e) {
						// alrighty, we have finished copying over all the input 
						// records, now let's dump our input string
						if (!conditional || (conditional && found))
							output(delimitedString);
					}
				}
			}
		}
		catch(EOFException e) {
		}
		return 0;
	}
}
