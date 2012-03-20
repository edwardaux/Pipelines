package com.hae.pipe;

import java.io.*;

public class PipeEventStageState extends PipeEvent {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private Stage _stage;
	private int _encoded;
	private int _decoded;

	public PipeEventStageState(Stage stage, int encoded, int decoded) {
		super(16);
		_stage = stage;
		_encoded = encoded;
		_decoded = decoded;
	}

	protected void populateBytes(DataOutputStream byteStream) throws IOException {
		byteStream.writeByte(13);                            // event id
		byteStream.writeByte(0);                             // normal format
		byteStream.writeShort(0);                            // reserved
		byteStream.writeInt(_stage.getRefId());              // stage reference
		byteStream.writeInt(_encoded);                       // encoded state
		byteStream.writeInt(_decoded);                       // decoded state
	}

	protected String asReadableString() {
		return "STAGE STATE";
	}
	
	public Stage getStage() {
		return _stage;
	}
	
	public int getEncoded() {
		return _encoded;
	}

	public int getDecoded() {
		return _decoded;
	}
}
