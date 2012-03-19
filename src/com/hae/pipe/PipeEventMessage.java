package com.hae.pipe;

import java.io.*;

public class PipeEventMessage extends PipeEvent {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private Stage _stage;
	private int _messageNo;
	private String[] _parms;
	private String _substitutedMessage;

	public PipeEventMessage(Stage stage, int messageNo, String[] parms, String substitutedMessage) {
		super(24+substitutedMessage.length());
		_stage = stage;
		_messageNo = messageNo;
		_parms = parms == null ? new String[] {} : parms;
		_substitutedMessage = substitutedMessage;
	}

	protected void populateBytes(DataOutputStream byteStream) throws IOException {
		String item = (_messageNo > 0 ? (_parms.length > 0 ? _parms[0] : ""): "");
		int refId = _stage == null ? 0 : _stage.getRefId();
		byteStream.writeByte(0);                             // event id
		byteStream.writeByte(0);                             // normal format
		byteStream.writeShort(0);                            // reserved
		byteStream.writeInt(refId);                          // stage reference
		byteStream.writeInt(0);                              // module name address
		byteStream.writeInt(_messageNo);                     // message num
		byteStream.writeChars(padAndTrim(item, 8));          // item
		byteStream.writeInt(0);                              // address of subst list
		byteStream.writeInt(_parms.length);                  // size of subst list
		byteStream.writeByte(_substitutedMessage.length());  // length of message
		byteStream.writeChars(_substitutedMessage);          // length of message
	}

	protected String asReadableString() {
		return "MESSAGE: "+_substitutedMessage;
	}
	
	public int getMessageNo() {
    	return _messageNo;
    }

	public String[] getParms() {
    	return _parms;
    }

	public Stage getStage() {
    	return _stage;
    }

	public String getSubstitutedMessage() {
    	return _substitutedMessage;
    }

}
