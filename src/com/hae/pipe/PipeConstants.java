package com.hae.pipe;

public interface PipeConstants {
	// constants for select
	public static final int ANYINPUT = -1;
	
	// constants for stream directions
	public static final int INPUT   = 1;
	public static final int OUTPUT  = 2;
	public static final int BOTH    = INPUT | OUTPUT; 
	public static final int SUMMARY = INPUT | OUTPUT;  // used for streamState

	// constants for eofreport
	public static final int CURRENT = 4;
	public static final int ANY     = 8;
	public static final int ALL     = 16;
	
	// constants for alignment
	public static final int LEFT    = 0;
	public static final int RIGHT   = 1;
	public static final int CENTRE  = 2;
	public static final int CENTER  = CENTRE;
}
