package com.hae.pipe;

import java.io.*;

public class PipeEventStageStart extends PipeEvent {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private Stage _stage;

	public PipeEventStageStart(Stage stage) {
		super(8);
		_stage = stage;
	}

	protected void populateBytes(DataOutputStream byteStream) throws IOException {
		byteStream.writeByte(8);                             // event id
		byteStream.writeByte(0);                             // normal format
		byteStream.writeShort(0);                            // reserved
		byteStream.writeInt(_stage.getRefId());              // stage reference
	}

	protected String asReadableString() {
		return "STAGE START";
	}
	
	public Stage getStage() {
		return _stage;
	}
}
