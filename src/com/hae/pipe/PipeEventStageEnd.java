package com.hae.pipe;

import java.io.*;

public class PipeEventStageEnd extends PipeEvent {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private Stage _stage;
	private int _rc;

	public PipeEventStageEnd(Stage stage, int rc) {
		super(20);
		_stage = stage;
		_rc = rc;
	}

	protected void populateBytes(DataOutputStream byteStream) throws IOException {
		byteStream.writeByte(9);                             // event id
		byteStream.writeByte(0);                             // normal format
		byteStream.writeShort(0);                            // reserved
		byteStream.writeInt(_stage.getRefId());              // stage reference
		byteStream.writeInt(_rc);                            // return code
		byteStream.writeInt(0);                              // size of work area allocated
		byteStream.writeInt(0);                              // highwater mark
	}

	protected String asReadableString() {
		return "STAGE END: rc="+_rc;
	}
	
	public Stage getStage() {
		return _stage;
	}
	
	public int getRC() {
		return _rc;
	}
}
