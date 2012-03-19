package com.hae.pipe;

public class PipeArtifact {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private static int NEXT_ID = 1;
	
	private int _refId;
	
	public PipeArtifact() {
		synchronized(PipeArtifact.class) {
			_refId = NEXT_ID++;
		}
	}

	public int getRefId() {
		return _refId;
	}
}
