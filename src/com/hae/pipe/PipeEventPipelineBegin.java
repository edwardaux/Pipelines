package com.hae.pipe;

import java.io.*;

public class PipeEventPipelineBegin extends PipeEvent {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private Pipeline _pipeline;
	private String _spec;

	public PipeEventPipelineBegin(Pipeline pipeline) {
		super(16+pipeline.getSpecification().length());
		_pipeline = pipeline;
		_spec = _pipeline.getSpecification();
	}

	protected void populateBytes(DataOutputStream byteStream) throws IOException {
		byteStream.writeByte(1);                             // event id
		byteStream.writeByte(1);                             // custom format - we include the spec
		byteStream.writeShort(0);                            // reserved
		byteStream.writeInt(_pipeline.getRefId());           // pipe reference
		byteStream.writeInt(0);                              // address of spec
		byteStream.writeInt(_spec.length());                 // spec length
		byteStream.writeChars(_spec);                        // spec 
	}

	protected String asReadableString() {
		return "PIPE BEGIN: "+_pipeline.getSpecification();
	}
	
	public Pipeline getPipeline() {
    	return _pipeline;
    }
	
	public String getSpec() {
		return _spec;
	}
}
