package com.hae.pipe;

public interface PipeListener {
	public void handleEvent(PipeEventConsoleInput event);
	public void handleEvent(PipeEventConsoleOutput event);
	public void handleEvent(PipeEventDispatcherCall event);
	public void handleEvent(PipeEventDispatcherResume event);
	public void handleEvent(PipeEventMessage event);
	public void handleEvent(PipeEventPause event);
	public void handleEvent(PipeEventPipelineAllocated event);
	public void handleEvent(PipeEventPipelineBegin event);
	public void handleEvent(PipeEventPipelineCommit event);
	public void handleEvent(PipeEventPipelineEnd event);
	public void handleEvent(PipeEventPipelineStall event);
	public void handleEvent(PipeEventScannerEnter event);
	public void handleEvent(PipeEventScannerItem event);
	public void handleEvent(PipeEventScannerLeave event);
	public void handleEvent(PipeEventStageEnd event);
	public void handleEvent(PipeEventStageStart event);
	public void handleEvent(PipeEventStageState event);
	public void handleEvent(PipeEventSubroutineComplete event);
	public void handleEvent(PipeEventSubroutineWaiting event);
	public void handleEvent(PipeEventSyntaxExit event);
}
