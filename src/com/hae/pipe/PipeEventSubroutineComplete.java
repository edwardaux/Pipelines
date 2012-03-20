package com.hae.pipe;

import java.io.*;

public class PipeEventSubroutineComplete extends PipeEvent {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private Pipeline _pipeline;
	private int _rc;

	public PipeEventSubroutineComplete(Pipeline pipeline, int rc) {
		super(12);
		_pipeline = pipeline;
		_rc = rc;
	}

	protected void populateBytes(DataOutputStream byteStream) throws IOException {
		byteStream.writeByte(18);                            // event id
		byteStream.writeByte(0);                             // normal format
		byteStream.writeShort(0);                            // reserved
		byteStream.writeInt(_pipeline.getRefId());           // pipe reference
		byteStream.writeInt(_rc);                            // return code
	}

	protected String asReadableString() {
		return "SUBROUTINE COMPLETE: rc"+_rc;
	}
	
	public Pipeline getPipeline() {
    	return _pipeline;
    }
	
	public int getRC() {
		return _rc;
	}
}
