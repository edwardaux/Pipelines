package com.hae.pipe;

import java.util.*;

public class Pipeline extends PipeArtifact {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	/**
	 * The pipeline spec that created this pipeline.  Only
	 * used for debugging purposes
	 */
	private String _specification;
	
	/**
	 * What type of pipe: PIPE, CALLPIPE, ADDPIPE
	 */
	private int _type;
	
	/**
	 * In the case of CALLPIPE and ADDPIPE pipes,
	 * this is who the calling stage was
	 */
	private Stage _callingStage;
	
	/**
	 * The options that control this pipe at the global level
	 */
	private Options _options = new Options();
	
	/**
	 * The number (sequence) that this pipeline is added
	 */
	private int _pipelineNumber;
	
	/**
	 * The name (if there is one) of this pipeline
	 */
	private String _name = null;
	
	/** 
	 * The collection of stages that make up this pipeline
	 */
	private ArrayList<Stage> _stages = new ArrayList<Stage>();
	
	/**
	 * The aggregate return code for this pipe
	 */
	private int _aggregateRC = 0;
	
	/**
	 * This contains a list of all the connectors for
	 * callpipe and addpipe
	 */
	private ArrayList<Scanner.Connector> _connectors = new ArrayList<Scanner.Connector>();
	
	/**
	 * Has this pipeline terminated?  
	 */
	private boolean _isTerminated = false;
	
	Pipeline(int type, String specification) {
		_type = type;
		_specification = specification;
	}
	
	ArrayList<Stage> getStages() {
		return _stages;
	}
	
	boolean isStillRunning() {
		if (_isTerminated)
			return false;
		
		// TODO make this more efficient
		for (int i = 0; i < _stages.size(); i++) {
			if (getStage(i).getState() != Pipe.STAGE_STATE_TERMINATED)
				return true;
		}
		return false;
	}
	
	boolean isTerminated() {
		return _isTerminated;
	}
	
	void setTerminated() {
		_isTerminated = true;
	}
	
	int getType() {
		return _type;
	}
	
	ArrayList<Scanner.Connector> getConnectors() {
		return _connectors;
	}
	
	void setDispatcher(Dispatcher dispatcher) {
		for (int i = 0; i < _stages.size(); i++) {
			getStage(i).setDispatcher(dispatcher);
		}
	}
	String getSpecification() {
		return _specification;
	}
	Options getOptions() {
		return _options;
	}
	void setOptions(Options options) {
		_options = options;
	}
	int getPipelineNumber() {
		return _pipelineNumber;
	}
	void setPipelineNumber(int pipelineNumber) {
		_pipelineNumber = pipelineNumber;
	}
	String getName() {
		return _name;
	}
	void setName(String name) {
		_name = name;
	}
	Stage getCallingStage() {
		return _callingStage;
	}
	void setCallingStage(Stage callingStage) {
		_callingStage = callingStage;
	}
	private Stage getStage(int i) {
		return (Stage)_stages.get(i);
	}
	void addStage(Stage stage) {
		_stages.add(stage);
	}
	int getAggregateRC() {
		return _aggregateRC;
	}
	void updateAggregateRC(Stage stage, int rc) {
		if (rc < 0 || _aggregateRC < 0)
			_aggregateRC = Math.min(rc, _aggregateRC);
		else
			_aggregateRC = Math.max(rc, _aggregateRC);
	}
	
	public void debugDump() {
		System.out.println("PIPE: "+_specification);
		for (int i = 0; i < _stages.size(); i++)
			getStage(i).debugDump();
	}
	public String toString() {
		return _specification;
	}
}
