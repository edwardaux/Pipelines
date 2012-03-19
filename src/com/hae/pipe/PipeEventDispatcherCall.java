package com.hae.pipe;

import java.io.*;

public class PipeEventDispatcherCall extends PipeEvent {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private Stage _stage;
	private int _register0;
	private int _register1;
	private String _code;

	public PipeEventDispatcherCall(Stage stage, int register0, int register1, String code) {
		super(24);
		_stage = stage;
		_register0 = register0;
		_register1 = register1;
		_code = code;
	}

	protected void populateBytes(DataOutputStream byteStream) throws IOException {
		byteStream.writeByte(11);                            // event id
		byteStream.writeByte(0);                             // normal format
		byteStream.writeShort(0);                            // reserved
		byteStream.writeInt(_stage.getRefId());              // stage reference
		byteStream.writeInt(0);                              // reserved
		byteStream.writeInt(_register0);                     // register 0
		byteStream.writeInt(_register1);                     // register 1
		byteStream.writeShort(0);                            // reserved
		byteStream.writeChars(_code);                        // service code
	}

	protected String asReadableString() {
		String s = _code+": ";
		if (_code.equals(CODE_ISSUE_CMD))
			s += "Issuing command";
		else if (_code.equals(CODE_COMMIT))
			s += "Committing to "+_register0;
		else if (_code.equals(CODE_END_STAGE))
			s += "Ending stage";
		else if (_code.equals(CODE_PEEKTO))
			s += "Peeking";
		else if (_code.equals(CODE_MISC)) {
			s += "Misc - ";
			switch(_register0) {
			case 0: s += "Stall"; break;
			case 1: s += "Suspend"; break;
			case 2: s += "Set break wait"; break;
			case 3: s += "Clear break wait"; break;
			case 4: s += "Process encoded pipe"; break;
			case 5: s += "Test events"; break;
			}
		}
		else if (_code.equals(CODE_PARM))
			s += "Setting parameter";
		else if (_code.equals(CODE_READTO))
			s += "Reading";
		else if (_code.equals(CODE_OUTPUT))
			s += "Outputting";
		else if (_code.equals(CODE_STREAMSTATE))
			s += "Checking "+(_register0 == -1 ? "INPUT" : "OUTPUT")+" stream ["+_register1+"] state";
		else if (_code.equals(CODE_SHORT))
			s += "Shorting";
		else if (_code.equals(CODE_SELECT)) {
			String dir = "?";
			switch(_register0) {
			case -1: dir = "INPUT"; break;
			case -2: dir = "OUTPUT"; break;
			case -3: dir = "BOTH"; break;
			case -4: dir = "ANYINPUT"; break;
			}
			s += "Selecting "+dir+" stream for stream ["+_register1+"]";
		}
		else if (_code.equals(CODE_STAGENUM))
			s += "Getting stage number";
		else if (_code.equals(CODE_SEVER))
			s += "Severing "+(_register0 == -1 ? "INPUT" : "OUTPUT")+" stream ["+_register1+"] state";
		else if (_code.equals(CODE_WAITECB))
			s += "Waiting for ECB";
		else if (_code.equals(CODE_EXIT))
			s += "Dispatcher exiting";
		return "CALL: "+s;
	}
	
	public String getCode() {
    	return _code;
    }

	public int getRegister0() {
    	return _register0;
    }

	public int getRegister1() {
    	return _register1;
    }

	public Stage getStage() {
    	return _stage;
    }

}
