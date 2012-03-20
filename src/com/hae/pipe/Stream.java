package com.hae.pipe;

import java.util.*;

public class Stream extends PipeArtifact {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private Stage _producer;
	private Stage _consumer;
	private int _producerStreamNo;
	private int _consumerStreamNo;
	private String _producerStreamId;
	private String _consumerStreamId;
	private boolean _poppable;

	private String _producerRecord;
	private String _consumerRecord;
	
	private List<StreamState> _stateStack = Collections.synchronizedList(new ArrayList<StreamState>());
	
	public Stream(Stage producer, Stage consumer) {
		_producer = producer;
		_consumer = consumer;
		_producerRecord = null;
		_consumerRecord = null;
		_producerStreamId = null;
		_consumerStreamId = null;
		_poppable = false;
	}
	
	protected synchronized void setProducer(Stage producer, int producerStreamNo, String producerStreamId) {
		_producer = producer;
		_producerStreamNo = producerStreamNo;
		_producerStreamId = producerStreamId;
	}
	
	protected void setConsumer(Stage consumer, int consumerStreamNo, String consumerStreamId) {
		_consumer = consumer;
		_consumerStreamNo = consumerStreamNo;
		_consumerStreamId = consumerStreamId;
	}

	public Stage getProducer() {
		return _producer;
	}
	
	public int getProducerStreamNo() {
		return _producerStreamNo;
	}

	public String getProducerStreamId() {
		return _producerStreamId;
	}
	
	public Stage getConsumer() {
		return _consumer;
	}
	
	public int getConsumerStreamNo() {
		return _consumerStreamNo;
	}

	public String getConsumerStreamId() {
		return _consumerStreamId;
	}
	
	protected String getConsumerRecord() {
		return _consumerRecord;
	}
	
	protected void copyProducerRecordToConsumer() {
		_consumerRecord = _producerRecord;
	}
	
	protected String getProducerRecord() {
		return _producerRecord;
	}
	
	protected void setProducerRecord(String producerRecord) {
		_producerRecord = producerRecord;
	}
	
	protected boolean atInputEOF() {
		// we are not at EOF completely until any written
		// records have been consumed
		return _producer == null && _consumerRecord == null;
	}
	
	protected boolean atOutputEOF() {
		return _consumer == null;
	}
	
	protected String consumeRecord() {
		String s = _consumerRecord;
		_consumerRecord = null;
		return s;
	}
	
	public synchronized void severInput() {
		if (_poppable) {
			if (_consumer != null)
				_consumer.setInputStream(_consumerStreamNo, new Stream(null, _consumer));
			pop();
		}
		else
			_consumer = null;
	}
	
	public synchronized void severOutput() {
		if (_poppable) {
			if (_producer != null)
				_producer.setOutputStream(_producerStreamNo, new Stream(_producer, null));
			pop();
		}
		else {
			_producer = null;
		}
	}
	
	public synchronized void push(boolean poppable) {
		_stateStack.add(new StreamState(_producer, _producerStreamNo, _producerStreamId, _consumer, _consumerStreamNo, _consumerStreamId, _poppable));
		_poppable = poppable;
	}
	
	public synchronized void pop() {
		if (_stateStack.size() != 0) {
			StreamState state = (StreamState)_stateStack.get(_stateStack.size()-1);
			_stateStack.remove(_stateStack.size()-1);
			_producer = state.producer;
			_producerStreamNo = state.producerStreamNo;
			_producerStreamId = state.producerStreamId;
			_consumer = state.consumer;
			_consumerStreamNo = state.consumerStreamNo;
			_consumerStreamId = state.consumerStreamId;
			_poppable = state.poppable;
			
			// there is a chance that the guy who caused the EOF condition
			// was outside the callpipe, so we need to make sure that we set
			// EOF on the streams in this case
			if (_producer != null && _producer.getState() == Pipe.STAGE_STATE_TERMINATED)
				_producer = null;
			if (_consumer != null && _consumer.getState() == Pipe.STAGE_STATE_TERMINATED)
				_consumer = null;
		}
	}
	
	public boolean equals(Object o) {
		if (o instanceof Stream) {
			Stream s = (Stream)o;
			return _producer == s._producer && 
			       _consumer == s._consumer &&
			       _producerStreamNo == s._producerStreamNo &&
			       _consumerStreamNo == s._consumerStreamNo;
		}
		return false;
	}
	
	public String toString() {
		return (_producer == null ? "<EOF>" : _producer.toString())+"["+_producerRecord+"]<==>"+(_consumer == null ? "<EOF>" : _consumer.toString())+"["+_consumerRecord+"]";
	}

	public void debugDump() {
		System.out.println("Instance="+getRefId()+": "+toString()); 
	}
	
	private class StreamState {
		public Stage producer;
		public Stage consumer;
		public int producerStreamNo;
		public int consumerStreamNo;
		public String producerStreamId;
		public String consumerStreamId;
		public boolean poppable;
		public StreamState(Stage producer, int producerStreamNo, String producerStreamId, Stage consumer, int consumerStreamNo, String consumerStreamId, boolean poppable) {
			this.producer = producer;
			this.producerStreamNo = producerStreamNo;
			this.producerStreamId = producerStreamId;
			this.consumer = consumer;
			this.consumerStreamNo = consumerStreamNo;
			this.consumerStreamId = consumerStreamId;
			this.poppable = poppable;
		}
	}
}
