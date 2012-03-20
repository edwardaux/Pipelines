package com.hae.pipe.stages;

import java.util.*;

import com.hae.pipe.*;

/**
 *           ┌──────────────┐                     
 *  ──COUNT──┴┬─CHARACTErs─┬┴──
 *            ├─WORDS──────┤  
 *            ├─LINES──────┤  
 *            ├─MINline────┤  
 *            └─MAXline────┘  
 *                                           
 */
public class Count extends Stage {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public int execute(String args) throws PipeException {
		signalOnError();
		
		commit(-2);
		
		long chars = -1;
		long words = -1;
		long lines = -1;
		long min = -1;
		long max = -1;
		
		PipeArgs pa = new PipeArgs(args);
		String word = scanWord(pa);
		if ("".equals(word))
			throw new PipeException(-11);
		
		while (!"".equals(word)) {
			if (Syntax.abbrev("CHARACTERS", word, 8) || Syntax.abbrev("BYTES", word, 5) || Syntax.abbrev("CHARS", word, 5))
				chars = 0;
			else if (Syntax.abbrev("WORDS", word, 5))
				words = 0;
			else if (Syntax.abbrev("LINES", word, 5) || Syntax.abbrev("RECORDS", word, 7))
				lines = 0;
			else if (Syntax.abbrev("MINLINE", word, 3))
				min = 0;
			else if (Syntax.abbrev("MAXLINE", word, 3))
				max = 0;
			else 
				throw new PipeException(-111, word);
			
			word = scanWord(pa);
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
		
		// if the secondary stream is defined, we write records
		// to primary output and count to secondary output
		boolean countToSecondary = (streamState(OUTPUT, 1) == 0);
		
		try {
			while (true) {
				String s = peekto();
				// if we are sending the count to the secondary output
				// stream, then we need to write the record to the primary
				if (countToSecondary)
					output(s);
				
				// now lets get some metrics
				if (chars != -1)
					chars += s.length(); 
				if (words != -1)
					words += new StringTokenizer(s).countTokens(); 
				if (lines != -1)
					lines += 1; 
				if (min != -1)
					min = Math.min(min, s.length()); 
				if (max != -1)
					max = Math.max(max, s.length()); 

				readto();
			}
		}
		catch(EOFException e) {
			try {
				// eof in the input stream, so we write the counts
				if (countToSecondary)
					select(OUTPUT, 1);
				output(makeOutput(chars, words, lines, min, max));
			}
			catch(EOFException e2) {
				// I don't think this should ever happen - famous last words, huh?!
			}
		}
		return 0;
	}
	
	private String makeOutput(long chars, long words, long lines, long min, long max) {
		String s = "";
		if (chars != -1)
			s += " "+chars; 
		if (words != -1)
			s += " "+words; 
		if (lines != -1)
			s += " "+lines; 
		if (min != -1)
			s += " "+min; 
		if (max != -1)
			s += " "+max;
		return s.trim();
	}
}
