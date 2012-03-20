package com.hae.pipe;

import java.io.*;

public class PipeEventScannerEnter extends PipeEvent {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private Pipeline _pipeline;

	public PipeEventScannerEnter(Pipeline pipeline) {
		super(8);
		_pipeline = pipeline;
	}

	protected void populateBytes(DataOutputStream byteStream) throws IOException {
		byteStream.writeByte(3);                             // event id
		byteStream.writeByte(0);                             // normal format
		byteStream.writeShort(0);                            // reserved
		byteStream.writeInt(_pipeline.getRefId());           // pipe reference
	}

	protected String asReadableString() {
		return "SCANNER ENTER";
	}
	
	public Pipeline getPipeline() {
    	return _pipeline;
    }
}
