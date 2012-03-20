package com.hae.pipe;

import java.io.*;

public abstract class PipeEvent {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public static final String CODE_ISSUE_CMD   = "CM";
	public static final String CODE_COMMIT      = "CT";
	public static final String CODE_END_STAGE   = "ES";
	public static final String CODE_PEEKTO      = "LO";
	public static final String CODE_MISC        = "MS";
	public static final String CODE_PARM        = "PA";
	public static final String CODE_READTO      = "PI";
	public static final String CODE_OUTPUT      = "PO";
	public static final String CODE_STREAMSTATE = "SA";
	public static final String CODE_SHORT       = "SH";
	public static final String CODE_SELECT      = "SL";
	public static final String CODE_STAGENUM    = "SN";
	public static final String CODE_SEVER       = "SV";
	public static final String CODE_WAITECB     = "WE";
	public static final String CODE_EXIT        = "X ";

	private int _size;
	
	protected PipeEvent(int size) {
		_size = size;
	}
	
	protected abstract void populateBytes(DataOutputStream byteStream) throws IOException;
	protected abstract String asReadableString();

	public byte[] getBytes() throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream(_size);
		populateBytes(new DataOutputStream(byteStream));
		return byteStream.toByteArray();
	}
	
	protected String padAndTrim(String s, int length) {
		if (s == null)
			s = "";
		
		return PipeUtil.align(s, length, PipeUtil.LEFT);
	}
}
