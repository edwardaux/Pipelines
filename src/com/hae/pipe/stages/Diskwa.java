package com.hae.pipe.stages;

/**
 *                    ┌─Variable─────────┐
 * ──DISKW──filename──┼──────────────────┼──
 *                    └─Fixed─┬────────┬─┘
 *                            └─number─┘
 */
public class Diskwa extends Diskw {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	// appends records
	protected boolean append() {
		return true;
	}

}
