package com.hae.pipe;

import java.io.*;

public class PipeEventSyntaxExit extends PipeEvent {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private static final byte[] FILLER = new byte[] { 
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
	};

	private Stage _stage;

	public PipeEventSyntaxExit(Stage stage) {
		super(64);
		_stage = stage;
	}

	protected void populateBytes(DataOutputStream byteStream) throws IOException {
		byteStream.writeByte(7);                             // event id
		byteStream.writeByte(0);                             // normal format
		byteStream.writeShort(0);                            // reserved
		byteStream.writeInt(_stage.getRefId());              // stage reference
		byteStream.writeInt(0);                              // address of work area
		byteStream.writeInt(0);                              // register 1
		byteStream.write(FILLER);                            // registers 0-11
	}

	protected String asReadableString() {
		return "SYNTAX EXIT";
	}
	
	public Stage getStage() {
		return _stage;
	}
}
