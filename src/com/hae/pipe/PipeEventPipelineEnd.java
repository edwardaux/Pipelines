package com.hae.pipe;

import java.io.*;

public class PipeEventPipelineEnd extends PipeEvent {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private Pipeline _pipeline;
	private int _aggregateRC; 

	public PipeEventPipelineEnd(Pipeline pipeline) {
		super(12);
		_pipeline = pipeline;
		_aggregateRC = pipeline.getAggregateRC();
	}

	protected void populateBytes(DataOutputStream byteStream) throws IOException {
		byteStream.writeByte(2);                             // event id
		byteStream.writeByte(0);                             // normal format
		byteStream.writeShort(0);                            // reserved
		byteStream.writeInt(_pipeline.getRefId());           // pipe reference
		byteStream.writeInt(_aggregateRC);                   // pipe rc
	}

	protected String asReadableString() {
		return "PIPE END: "+_pipeline.getAggregateRC();
	}
	
	public Pipeline getPipeline() {
    	return _pipeline;
    }
	
	public int getAggregateRC() {
		return _aggregateRC;
	}
}
