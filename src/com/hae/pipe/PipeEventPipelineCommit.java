package com.hae.pipe;

import java.io.*;

public class PipeEventPipelineCommit extends PipeEvent {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private Pipeline _pipeline;
	private int _aggregateRC;
	private int _commitLevel;

	public PipeEventPipelineCommit(Pipeline pipeline, int aggregateRC, int commitLevel) {
		super(16);
		_pipeline = pipeline;
		_aggregateRC = aggregateRC;
		_commitLevel = commitLevel;
	}

	protected void populateBytes(DataOutputStream byteStream) throws IOException {
		byteStream.writeByte(14);                            // event id
		byteStream.writeByte(0);                             // normal format
		byteStream.writeShort(0);                            // reserved
		byteStream.writeInt(_pipeline.getRefId());           // pipe reference
		byteStream.writeInt(_aggregateRC);                   // aggregate RC
		byteStream.writeInt(_commitLevel);                   // commit level
	}

	protected String asReadableString() {
		return "COMMIT: aggrc="+_aggregateRC+" commitLevel="+_commitLevel;
	}
	
	public Pipeline getPipeline() {
    	return _pipeline;
    }
	
	public int getAggregateRC() {
		return _aggregateRC;
	}
	
	public int getCommitLevel() {
		return _commitLevel;
	}
}
