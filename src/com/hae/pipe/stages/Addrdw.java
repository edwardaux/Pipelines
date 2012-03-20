package com.hae.pipe.stages;

import com.hae.pipe.*;

/**
 * ──ADDRDW──┬─Variable─┬──
 *           ├─CMS──────┤
 *           ├─SF───────┤
 *           ├─CMS4─────┤
 *           └─SF4──────┘ 
 */
public class Addrdw extends Stage {
	public int execute(String args) throws PipeException {
		signalOnError();
		try {
			commit(-2);

			int type = 0;
			PipeArgs pa = new PipeArgs(args);
			
			String word = scanWord(pa);
			
			if (Syntax.abbrev("VARIABLE", word, 1))
				type = 0;
			else if (Syntax.abbrev("CMS", word, 3))
				type = 1;
			else if (Syntax.abbrev("SF", word, 2))
				type = 2;
			else if (Syntax.abbrev("CMS4", word, 4))
				type = 3;
			else if (Syntax.abbrev("SF4", word, 3))
				type = 4;
			else if ("".equals(word)) {
				// missing parameters
				return exitCommand(-113);
			}
			else {
				// extra parameters
				return exitCommand(-112, word);
			}

			word = scanWord(pa);
			if (!"".equals(word)) {
				// extra parameters
				return exitCommand(-112, word);
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

			while (true) {
				String s = peekto();
				switch(type) {
				case 0:
					// Variable
					output(PipeUtil.makeBinLength2(s.length()+4)+"\u0000\u0000"+s);
					break;
				case 1:
					// CMS
					if (!"".equals(s)) {
						output(PipeUtil.makeBinLength2(s.length())+s);
					}
					break;
				case 2:
					// SF
					output(PipeUtil.makeBinLength2(s.length()+2)+s);
					break;
				case 3:
					// CMS4
					if (!"".equals(s)) {
						output(PipeUtil.makeBinLength4(s.length())+s);
					}
					break;
				case 4:
					// SF4
					output(PipeUtil.makeBinLength4(s.length()+4)+s);
					break;
				}
				readto();	
			}
		}
		catch(EOFException e) {
		}
		return 0;
	}
	
}
