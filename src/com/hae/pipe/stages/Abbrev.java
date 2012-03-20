package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 * ──ABBREV──┬─────────────────────────────────┬──
 *           └─target──┬─────────────────────┬─┘
 *                     └─number──┬─────────┬─┘
 *                               └─ANYCASE─┘ 
 */
public class Abbrev extends Stage {
	public int execute(String args) throws PipeException {
		signalOnError();
		try {
			commit(-2);
			
			String target = "";
			int number = 0;
			boolean anycase = false;
			PipeArgs pa = new PipeArgs(args);
			
			String word = scanWord(pa);
			
			// have we got any input...
			if (!"".equals(word)) {
				
				// first word is always the target
				target = word;

				// have they passed a number?
				word = scanWord(pa);
				if (Syntax.isNumber(word)) {
					number = PipeUtil.makeInt(word);
					word = scanWord(pa);
				}
				
				// do we care about case?
				if (Syntax.abbrev("ANYCASE", word, 3) ||
					Syntax.abbrev("CASEANY", word, 7) ||
					Syntax.abbrev("IGNORECASE", word, 10) ||
					Syntax.abbrev("CASEIGNORE", word, 10)) {
					anycase = true;
					word = scanWord(pa);
				}

				if (!"".equals(word)) {
					// extra parameters
					return exitCommand(-112, word);
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
				
			// ok, let's roll...
			commit(0);

			while (true) {
				String s = peekto();
				if (Syntax.abbrev(target, s, number, anycase)) {
					// yes, it is a match, so write to primary output
					select(OUTPUT, 0);
					output(s);
				}
				else {
					// uh oh, no match.  Write to secondary if present  
					if (streamState(OUTPUT, 1) == 0) {
						select(OUTPUT, 1);
						output(s);
					}
				}
				// consume record
				readto();	
			}
		}
		catch(EOFException e) {
		}
		return 0;
	}
}
