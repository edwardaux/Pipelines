package com.hae.pipe;

import java.io.*;

public class PipeEventDispatcherResume extends PipeEvent {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private Stage _stage;
	private int _rc;
	private String _code;
	private int _producerRef;
	private int _consumerRef;
	private int _selectedInputStreamNo;
	private int _selectedOutputStreamNo;
	private int _producerSelectedStreamNo;
	private int _consumerSelectedStreamNo;
	

	public PipeEventDispatcherResume(Stage stage, Stream stream, int rc, String code) {
		super(48);
		_stage = stage;
		Stage producer = (stream == null ? null : stream.getProducer()); // TODO remove null check once TODO in Dispatcher.unblock is fixed
		Stage consumer = (stream == null ? null : stream.getConsumer()); // TODO remove null check once TODO in Dispatcher.unblock is fixed
		_rc = rc;
		_code = code;
		_selectedInputStreamNo = stage.getSelectedInputStreamNo();
		_selectedOutputStreamNo = stage.getSelectedOutputStreamNo();
		_producerRef = (producer == null ? 0 : (producer.getSelectedOutputStreamNo() == stream.getProducerStreamNo() ? producer.getRefId() : -1));
		_consumerRef = (consumer == null ? 0 : (consumer.getSelectedInputStreamNo() == stream.getConsumerStreamNo() ? consumer.getRefId() : -1));
		_producerSelectedStreamNo = (producer == null ? -1 : producer.getSelectedOutputStreamNo());
		_consumerSelectedStreamNo = (consumer == null ? -1 : consumer.getSelectedInputStreamNo());
	}

	protected void populateBytes(DataOutputStream byteStream) throws IOException {
		byteStream.writeByte(10);                                // event id
		byteStream.writeByte(0);                                 // normal format
		byteStream.writeShort(0);                                // reserved
		byteStream.writeInt(_stage.getRefId());                  // stage reference
		byteStream.writeInt(_rc);                                // return code
		byteStream.writeInt(0);                                  // register 0 
		byteStream.writeInt(0);                                  // register 1
		byteStream.writeShort(0);                                // reserved
		byteStream.writeChars(_code);                            // service call code
		byteStream.writeInt(_selectedInputStreamNo);             // stage selected input stream no 
		byteStream.writeInt(_producerRef);                       // producer stage reference
		byteStream.writeInt(_producerSelectedStreamNo);          // producer selected output stream no
		byteStream.writeInt(_selectedOutputStreamNo);            // stage selected output stream no
		byteStream.writeInt(_consumerRef);                       // consumer stage reference
		byteStream.writeInt(_consumerSelectedStreamNo);          // consumer selected input stream no
	}

	protected String asReadableString() {
		return "RESUME "+_code+": rc="+_rc;
	}
	
	public Stage getStage() {
		return _stage;
	}

	public int getRC() {
    	return _rc;
    }
	
	public String getCode() {
    	return _code;
    }

	public int getConsumerRef() {
    	return _consumerRef;
    }

	public int getConsumerSelectedStreamNo() {
    	return _consumerSelectedStreamNo;
    }

	public int getProducerRef() {
    	return _producerRef;
    }

	public int getProducerSelectedStreamNo() {
    	return _producerSelectedStreamNo;
    }

	public int getSelectedInputStreamNo() {
    	return _selectedInputStreamNo;
    }

	public int getSelectedOutputStreamNo() {
    	return _selectedOutputStreamNo;
    }

}
