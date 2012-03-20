package com.hae.pipe.stages;

/**
 *                    ┌─Variable─────────┐
 * ──DISKW──filename──┼──────────────────┼──
 *                    └─Fixed─┬────────┬─┘
 *                            └─number─┘
 */
public class Diskwa extends Diskw {
	// appends records
	protected boolean append() {
		return true;
	}

}
