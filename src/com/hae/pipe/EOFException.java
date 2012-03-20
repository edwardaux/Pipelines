package com.hae.pipe;

public class EOFException extends PipeException {
	public EOFException() {
		// TODO this isn't actually error code 12!
		super(12);
	}
}
