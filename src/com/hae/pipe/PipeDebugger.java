package com.hae.pipe;

import java.util.*;

public class PipeDebugger implements PipeListener {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	private boolean _dumpImmediately = true;
	private ArrayList<String> _records = new ArrayList<String>();
	
	public PipeDebugger() {
		this(true);
	}
	
	public PipeDebugger(boolean dumpImmediately) {
		_dumpImmediately = dumpImmediately;
	}
	
	public void dumpAll() {
		for (int i = 0; i < _records.size(); i++) 
			System.out.println(_records.get(i));
	}
	
	private synchronized void dump(Stage stage, PipeEvent event) {
//		if (stage != null && stage.getOwningPipeline() != null)
//			stage.getOwningPipeline().debugDump();
		String s = indent(stage)+stage+" "+event.asReadableString();
		if (_dumpImmediately)
			System.out.println(s);
		else
			_records.add(s);
	}
	private String indent(Stage stage) {
		if (stage == null)
			return "";
		String indent = "";
		for (int i = 0; i < stage.getStageNumber(); i++)
			indent += "                            ";
		return indent;
	}
	public void handleEvent(PipeEventConsoleInput event) {
		dump(event.getStage(), event);
	}
	public void handleEvent(PipeEventConsoleOutput event) {
		dump(event.getStage(), event);
	}
	public void handleEvent(PipeEventDispatcherCall event) {
		dump(event.getStage(), event);
	}
	public void handleEvent(PipeEventDispatcherResume event) {
		dump(event.getStage(), event);
	}
	public void handleEvent(PipeEventMessage event) {
		dump(event.getStage(), event);
	}
	public void handleEvent(PipeEventPause event) {
		dump(event.getStage(), event);
	}
	public void handleEvent(PipeEventPipelineAllocated event) {
		dump(null, event);
	}
	public void handleEvent(PipeEventPipelineBegin event) {
		dump(null, event);
	}
	public void handleEvent(PipeEventPipelineCommit event) {
		dump(null, event);
	}
	public void handleEvent(PipeEventPipelineEnd event) {
		dump(null, event);
	}
	public void handleEvent(PipeEventPipelineStall event) {
		dump(null, event);
	}
	public void handleEvent(PipeEventScannerEnter event) {
		dump(null, event);
	}
	public void handleEvent(PipeEventScannerItem event) {
		dump(event.getStage(), event);
	}
	public void handleEvent(PipeEventScannerLeave event) {
		dump(null, event);
	}
	public void handleEvent(PipeEventStageEnd event) {
		dump(event.getStage(), event);
	}
	public void handleEvent(PipeEventStageStart event) {
		dump(event.getStage(), event);
	}
	public void handleEvent(PipeEventStageState event) {
		dump(event.getStage(), event);
	}
	public void handleEvent(PipeEventSubroutineComplete event) {
		dump(null, event);
	}
	public void handleEvent(PipeEventSubroutineWaiting event) {
		dump(event.getStage(), event);
	}
	public void handleEvent(PipeEventSyntaxExit event) {
		dump(event.getStage(), event);
	}
}
