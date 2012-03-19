package com.hae.pipe;

import java.io.*;

public class PipeEventPipelineAllocated extends PipeEvent {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private static final byte[] FILLER = new byte[] { 
		0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0
	};
	
	private Pipeline _pipeline;
	private byte _options;
	private short _flagsOn;
	private short _flagsOff;
	private char _stageSep;
	private char _endChar;
	private char _escChar;
	private String _spec;

	public PipeEventPipelineAllocated(Pipeline pipeline) {
		super(88+pipeline.getSpecification().length());
		Options options = pipeline.getOptions();
		_pipeline = pipeline;
		_stageSep = options.stageSep;
		_endChar  = options.endChar;
		_escChar  = options.escChar;
		_spec = pipeline.getSpecification();
		_options = 0;
		if (options.trace)   _options |= 0x01;
		if (options.listErr) _options |= 0x02;
		if (options.listRC)  _options |= 0x04;
		if (options.listCmd) _options |= 0x80;
		_flagsOn = (short)(options.msgLevel & 0x03);
		_flagsOff = (short)~(options.msgLevel & 0x03);
	}

	protected void populateBytes(DataOutputStream byteStream) throws IOException {
		byteStream.writeByte(4);                             // event id
		byteStream.writeByte(1);                             // custom format - we add the spec
		byteStream.writeShort(0);                            // reserved
		byteStream.writeChars("pipedef ");                   // magic
		byteStream.writeByte(2);                             // always send the long record
		byteStream.writeByte(_pipeline.getType());           // pipe/callpipe/addpipe flag
		byteStream.writeByte(_options);                      // options flag
		byteStream.writeByte(0);                             // reserved
		byteStream.writeChars(_pipeline.getName());          // name
		byteStream.writeShort(_flagsOn);                     // low order flags - on
		byteStream.writeShort(_flagsOff);                    // low order flags - off
		byteStream.writeByte(_stageSep);                     // stage sep
		byteStream.writeByte(_endChar);                      // end char
		byteStream.writeByte(_escChar);                      // esc char
		byteStream.writeShort(0);                            // reserved
		byteStream.writeByte(0);                             // reserved
		byteStream.writeShort(0);                            // offset to stage definitions
		byteStream.writeInt(0);                              // address of original spec
		byteStream.writeInt(_spec.length());                 // spec length
		byteStream.writeInt(0);                              // spec name address
		byteStream.writeInt(_pipeline.getName().length());   // length of the name
		byteStream.write(FILLER);                            // filler
		byteStream.writeChars(_spec);                        // spec
	}

	protected String asReadableString() {
		return "PIPE ALLOC: "+_pipeline.getSpecification();
	}
	
	public Pipeline getPipeline() {
    	return _pipeline;
    }

	public char getEndChar() {
    	return _endChar;
    }

	public char getEscChar() {
    	return _escChar;
    }

	public short getFlagsOff() {
    	return _flagsOff;
    }

	public short getFlagsOn() {
    	return _flagsOn;
    }

	public byte getOptions() {
    	return _options;
    }

	public String getSpec() {
    	return _spec;
    }

	public char getStageSep() {
    	return _stageSep;
    }
}
