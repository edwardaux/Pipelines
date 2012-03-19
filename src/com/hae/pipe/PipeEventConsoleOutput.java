package com.hae.pipe;

import java.io.*;

public class PipeEventConsoleOutput extends PipeEvent {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private Stage _stage;
	private String _output;

	public PipeEventConsoleOutput(Stage stage, String output) {
		super(16);
		_stage = stage;
		_output = output;
	}

	protected void populateBytes(DataOutputStream byteStream) throws IOException {
		byteStream.writeByte(16);                            // event id
		byteStream.writeByte(1);                             // custom format - we store the output
		byteStream.writeShort(0);                            // reserved
		byteStream.writeInt(_stage.getRefId());              // stage reference
		byteStream.writeInt(0);                              // address of the buffer
		byteStream.writeInt(_output.length());               // length of buffer
		byteStream.writeChars(_output);                      // output text
	}

	protected String asReadableString() {
		return "CONS OUTPUT: "+_output;
	}
	
	public Stage getStage() {
		return _stage;
	}
	
	public String getOutput() {
		return _output;
	}
}
