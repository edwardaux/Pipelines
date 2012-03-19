package com.hae.pipe;

import java.io.*;

public class PipeEventPause extends PipeEvent {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private Stage _stage;
	
	public PipeEventPause(Stage stage) {
		super(8);
		_stage = stage;
	}

	protected void populateBytes(DataOutputStream byteStream) throws IOException {
		byteStream.writeByte(17);                            // event id
		byteStream.writeByte(0);                             // normal format
		byteStream.writeShort(0);                            // reserved
		byteStream.writeInt(_stage.getRefId());              // stage reference
	}

	protected String asReadableString() {
		return "PAUSE";
	}
  	
	public Stage getStage() {
		return _stage;
	}
}
