package com.hae.pipe;

import java.io.*;

public class PipeEventScannerItem extends PipeEvent {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private Pipeline _pipeline;
	private Stage _stage;
	private int _itemType;
	private String _label;
	private int _direction;
	private int _streamNo;
	private String _streamId;

	/**
	 * Variant for pipeline begin
	 */
	public PipeEventScannerItem(Pipeline pipeline) {
		super(19);
		_pipeline = pipeline;
		_itemType = 0;
	}

	/**
	 * Variant for stage
	 */
	public PipeEventScannerItem(Pipeline pipeline, Stage stage) {
		super(56+stage.getArgs().length());
		_pipeline = pipeline;
		_stage = stage;
		_itemType = 1;
	}

	/**
	 * Variant for label
	 */
	public PipeEventScannerItem(Pipeline pipeline, Stage stage, String label, String streamId) {
		super(36);
		_pipeline = pipeline;
		_stage = stage;
		_label = label;
		_streamId = streamId;
		_itemType = 2;
	}

	/**
	 * Variant for connector
	 */
	public PipeEventScannerItem(Pipeline pipeline, int direction, int streamNo, String streamId) {
		super(28);
		_pipeline = pipeline;
		_direction = direction;
		_streamNo = streamNo;
		_streamId = streamId;
		_itemType = 3;
	}

	protected void populateBytes(DataOutputStream byteStream) throws IOException {
		int pipeNo = _pipeline.getPipelineNumber();
		int pipeId = _pipeline.getRefId();
		int stageNo = _stage == null ? 0 : _stage.getStageNumber();
		int stageId = _stage == null ? 0 : _stage.getRefId();
		String parms = _stage == null ? "" : _stage.getArgs();
		if (_itemType == 0) {
			// begin
			byteStream.writeByte(6);                         // event id
			byteStream.writeByte(0);                         // normal format
			byteStream.writeShort(0);                        // reserved
			byteStream.writeInt(pipeId);                     // pipe reference
			byteStream.writeInt(pipeNo);                     // pipe number
			byteStream.writeInt(stageNo);                    // stage number
			byteStream.writeShort(2);                        // num of bytes
			byteStream.writeByte(_itemType);                 // pipeline begin
		}
		else if (_itemType == 1) {
			// stage
			int flags = 0;
			if (_pipeline.getOptions().trace)   flags |= 0x01;
			if (_pipeline.getOptions().listErr) flags |= 0x02;
			if (_pipeline.getOptions().listRC)  flags |= 0x04;
			if (_pipeline.getOptions().listCmd) flags |= 0x80;
			short flagsOn = (short)(_pipeline.getOptions().msgLevel & 0x03);
			short flagsOff = (short)~(_pipeline.getOptions().msgLevel & 0x03);
			byteStream.writeByte(6);                         // event id
			byteStream.writeByte(1);                         // custom format
			byteStream.writeShort(0);                        // reserved
			byteStream.writeInt(stageId);                    // stage reference
			byteStream.writeInt(pipeNo);                     // pipe number
			byteStream.writeInt(stageNo);                    // stage number
			byteStream.writeShort(38+parms.length());        // num of bytes
			byteStream.writeByte(_itemType);                 // stage
			byteStream.writeByte(flags);                     // flags
			byteStream.writeShort(flagsOn);                  // message level flags on  
			byteStream.writeShort(flagsOff);                 // message level flags off
			byteStream.writeChars(padAndTrim(_stage.getLabel(), 8)); // label 
			byteStream.writeChars(padAndTrim(_stage.getLabel(), 8)); // stream identifier
			byteStream.writeInt(0);                          // entry point address
			byteStream.writeInt(0);                          // address of verb
			byteStream.writeInt(_stage.getNameAndArgs().length()); // length of verb
			byteStream.writeInt(0);                          // address of parm string 
			byteStream.writeInt(parms.length());             // length of parm string
			byteStream.writeChars(parms);                    // parm string
		}
		else if (_itemType == 2) {
			// label
			byteStream.writeByte(6);                         // event id
			byteStream.writeByte(0);                         // normal format
			byteStream.writeShort(0);                        // reserved
			byteStream.writeInt(pipeId);                     // pipe reference
			byteStream.writeInt(pipeNo);                     // pipe number
			byteStream.writeInt(stageNo);                    // stage number
			byteStream.writeShort(18);                       // num of bytes
			byteStream.writeByte(_itemType);                 // label
			byteStream.writeShort(0);                        // reserved 
			byteStream.writeInt(stageId);                    // stage reference
			byteStream.writeChars(padAndTrim(_label, 8));    // label
			byteStream.writeChars(padAndTrim(_streamId, 4)); // stream identifier
		}
		else {
			// connector
			byteStream.writeByte(6);                         // event id
			byteStream.writeByte(0);                         // normal format
			byteStream.writeShort(0);                        // reserved
			byteStream.writeInt(pipeId);                     // pipe reference
			byteStream.writeInt(pipeNo);                     // pipe number
			byteStream.writeInt(stageNo);                    // stage number
			byteStream.writeShort(10);                       // num of bytes
			byteStream.writeByte(_itemType);                 // connector
			byteStream.writeByte(_direction);                // direction
			byteStream.writeChars(padAndTrim(_streamId, 4)); // stream identifier
			byteStream.writeInt(_streamNo);                  // stage no
		}
	}

	protected String asReadableString() {
		if (_itemType == 0)
			return "SCANNER BEGIN";
		else if (_itemType == 1)
			return "SCANNER STAGE: "+_stage.getNameAndArgs();
		else if (_itemType == 1)
			return "SCANNER LABEL: "+_stage.getNameAndArgs();
		else 
			return "SCANNER CONNECTOR: DIR="+_direction+" ID="+_streamId+" NO="+_streamNo;
	}
	
	public Pipeline getPipeline() {
		return _pipeline;
	}

	public int getDirection() {
		return _direction;
	}

	public int getItemType() {
		return _itemType;
	}

	public String getLabel() {
		return _label;
	}

	public Stage getStage() {
		return _stage;
	}

	public String getStreamId() {
		return _streamId;
	}

	public int getStreamNo() {
		return _streamNo;
	}
}
