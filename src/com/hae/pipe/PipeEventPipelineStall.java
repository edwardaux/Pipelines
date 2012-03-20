package com.hae.pipe;

import java.io.*;

public class PipeEventPipelineStall extends PipeEvent {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private Pipeline _pipeline;

	public PipeEventPipelineStall(Pipeline pipeline) {
		super(8);
		_pipeline = pipeline;
	}

	protected void populateBytes(DataOutputStream byteStream) throws IOException {
		byteStream.writeByte(12);                            // event id
		byteStream.writeByte(0);                             // normal format
		byteStream.writeShort(0);                            // reserved
		byteStream.writeInt(_pipeline.getRefId());           // pipe reference
	}

	protected String asReadableString() {
		return "STALL";
	}
	
	public Pipeline getPipeline() {
    	return _pipeline;
    }
}
