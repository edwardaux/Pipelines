package com.hae.pipe;

public class EOFException extends PipeException {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public EOFException() {
		// TODO this isn't actually error code 12!
		super(12);
	}
}
