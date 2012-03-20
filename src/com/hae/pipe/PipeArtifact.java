package com.hae.pipe;

public class PipeArtifact {
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
