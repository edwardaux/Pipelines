package com.hae.pipe;

import java.io.*;

public class PipeEventConsoleInput extends PipeEvent {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private Stage _stage;
	private String _input;

	public PipeEventConsoleInput(Stage stage, String input) {
		super(20);
		_stage = stage;
		_input = input;
	}

	protected void populateBytes(DataOutputStream byteStream) throws IOException {
		byteStream.writeByte(15);                            // event id
		byteStream.writeByte(1);                             // custom format - we store the input
		byteStream.writeShort(0);                            // reserved
		byteStream.writeInt(_stage.getRefId());              // stage reference
		byteStream.writeInt(0);                              // address of the buffer
		byteStream.writeInt(_input.length());                // length of buffer
		byteStream.writeInt(0);                              // address of feedback word
		byteStream.writeChars(_input);                       // input text
	}

	protected String asReadableString() {
		return "CONS INPUT: "+_input;
	}
	
	public Stage getStage() {
		return _stage;
	}
	
	public String getInput() {
		return _input;
	}
}
