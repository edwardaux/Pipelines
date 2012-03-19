package com.hae.pipe.stages;

/**
 *          
 *  ──NLOCATE──┬─────────┬──┬───────┬──┬─────────────┬──┬───────┬──>
 *             └─ANYcase─┘  ├─MIXED─┤  └─inputRanges─┘  └─ANYof─┘
 *                          ├─ONEs──┤ 
 *                          └─ZEROs─┘ 
 *  >──┬─────────────────┬──
 *     └─delimitedString─┘
 *  
 */
public class NLocate extends Locate {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	/**
	 * Support for LOCATE/NLOCATE.  If LOCATE, then we leave 
	 * the sign alone 
	 */
	protected boolean shouldNegate() {
		return true;
	}
}
