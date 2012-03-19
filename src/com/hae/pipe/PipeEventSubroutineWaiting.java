package com.hae.pipe;

import java.io.*;

public class PipeEventSubroutineWaiting extends PipeEvent {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private Pipeline _pipeline;
	private Stage _stage;

	public PipeEventSubroutineWaiting(Pipeline pipeline, Stage stage) {
		super(12);
		_pipeline = pipeline;
		_stage = stage;
	}

	protected void populateBytes(DataOutputStream byteStream) throws IOException {
		byteStream.writeByte(19);                            // event id
		byteStream.writeByte(0);                             // normal format
		byteStream.writeShort(0);                            // reserved
		byteStream.writeInt(_stage.getRefId());              // stage reference
		byteStream.writeInt(_pipeline.getRefId());           // pipe reference
	}

	protected String asReadableString() {
		return "SUBROUTINE WAIT";
	}
	
	public Pipeline getPipeline() {
    	return _pipeline;
    }
	
	public Stage getStage() {
		return _stage;
	}
}
