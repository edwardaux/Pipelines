package com.hae.pipe;

public class PipeException extends Exception {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private int _messageNo;
	private String[] _parms;
	
	public PipeException(int messageNo) {
		this(messageNo, (String[])null);
	}
	public PipeException(int messageNo, String parm1) {
		this(messageNo, new String[] { parm1 });
	}
	public PipeException(int messageNo, String parm1, String parm2) {
		this(messageNo, new String[] { parm1, parm2 });
	}
	public PipeException(int messageNo, String parm1, String parm2, String parm3) {
		this(messageNo, new String[] { parm1, parm2, parm3 });
	}
	public PipeException(int messageNo, String parm1, String parm2, String parm3, String parm4) {
		this(messageNo, new String[] { parm1, parm2, parm3, parm4 });
	}
	public PipeException(int messageNo, int parm1) {
		this(messageNo, new String[] { ""+parm1 });
	}
	private PipeException(int messageNo, String[] parms) {
		_messageNo = messageNo;
		_parms = parms;
	}
	public int getMessageNo() {
		return _messageNo;
	}
	public String[] getParms() {
		return _parms;
	}
}
