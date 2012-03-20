package com.hae.pipe;

import java.util.*;

public class Dispatcher implements PipeConstants {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	/**
	 * The owning Pipe that we are dispatching events for
	 */
	private Pipe _pipe;
	
	/**
	 * The collection of all the pipelines this dispatcher is 
	 * managing (including those that have been added by addpipe
	 * and callpipe
	 */
	private List<Pipeline> _pipelines;
	
	/**
	 * The collection of runlists that we are processing
	 */
	private ArrayList<RunList> _runlists;
	
	/**
	 * Internal debug flag
	 */
	private boolean debug = false;
	
	/**
	 * A collection of parameters that can be used to pass Java
	 * objects to/from stages
	 */
	private HashMap<String, Object> _parameters;

	public Dispatcher(Pipe pipe, HashMap<String, Object> parameters) {
		_pipe = pipe;
		_parameters = parameters;
		_pipelines = Collections.synchronizedList(new ArrayList<Pipeline>());
		_runlists = new ArrayList<RunList>();
	}
	
	public int execute() throws PipeException {
		Stream stream = null;
		
		StallDetector stallDetector = new StallDetector();
		boolean stalled = false;
		while (true) {
			boolean everythingRunning = false;
			for (int i = 0; i < _pipelines.size(); i++) {
				Pipeline pipeline = (Pipeline)_pipelines.get(i);
				
				stalled |= stallDetector.isStalled(pipeline);
				if (stalled) {
					everythingRunning = false;
					stall(pipeline, null);  // TODO which stage caused the stall?
					continue;
				}
				synchronized(pipeline) {
					boolean pipeRunning = pipeline.isStillRunning();
					everythingRunning |= pipeRunning;
	
					if (!pipeRunning) {
						if (debug)
							pipeline.debugDump();
						
						// there may not be a calling stage (ie. not called via callpipe), so 
						// we need to be careful that we handle that. Also, note that this code is 
						// in the main loop, so it will be called multiple times.  We only want to 
						// release the calling stage if it is the first time through after all the
						// pipeline stages have completed.
						Stage callingStage = pipeline.getCallingStage();
						if (callingStage != null && callingStage.getState() == Pipe.STAGE_STATE_WAITING_SUBROUTINE && !pipeline.isTerminated() ) {
							if (_pipe.hasListener())
								_pipe.getListener().handleEvent(new PipeEventSubroutineComplete(pipeline, pipeline.getAggregateRC()));
							//debugDispatcher(callingStage, "Unblocking stage waiting on CALLPIPE");
							unblock(callingStage, 0);
						}
						pipeline.setTerminated();
						if (_pipe.hasListener())
							_pipe.getListener().handleEvent(new PipeEventPipelineEnd(pipeline));
					}
					else {
						int aggregateRC = pipeline.getAggregateRC();
						RunList runList = getRunList(pipeline);
						ArrayList<Stage> stages = runList.getStagesAtLowestCommitLevel();
						
						if (debug)
							pipeline.debugDump();
						
						boolean allWaitingForCommit = true;
						for (int j = 0; j < stages.size(); j++) {
							Stage stage = (Stage)stages.get(j);
							debugState(pipeline);
							
							if (stage.getState() != Pipe.STAGE_STATE_WAITING_COMMIT)
								allWaitingForCommit = false;
							
							switch(stage.getState()) {
							case Pipe.STAGE_STATE_COMMITTED:
								//debugDispatcher(stage, "State=>NOT STARTED");
								stage.setState(Pipe.STAGE_STATE_NOT_STARTED);
								break;
							case Pipe.STAGE_STATE_NOT_STARTED:
								//debugDispatcher(stage, "State=>RUNNING");
								start(stage);
								break;
							case Pipe.STAGE_STATE_RUNNING:
								// no need to do anything as it is obviously 
								// doing some processing of its own
								break;
							case Pipe.STAGE_STATE_WAITING_PEEKTO:
							case Pipe.STAGE_STATE_WAITING_READTO:
								// waiting to peek or read from an input stream...
								int selectedInputStreamNo = stage.getSelectedInputStreamNo();
								if (selectedInputStreamNo == ANYINPUT) {
									// we can read from any stream...
									int numStreams = stage.getNumberOfInputStreams();
									int numAtEOF = 0;
									for (int z = 0; z < numStreams; z++) {
										stream = stage.getInputStream(z);
										// if the stream has been severed, that's OK. We will just
										// look at the next one.
										if (!stream.atInputEOF()) {
											if (checkRendesvouzInput(stage, stream, z))
												break;
										}
										else
											numAtEOF++;
									}
									if (numAtEOF == numStreams) {
										// if we get to here, then there are no input streams that
										// are still connected, so we have to let the stage know
										//debugDispatcher(stage, "XXXXTO: Unblocking due no conected input streams");
										// set the selected stream to the first one (they should all be in the
										// same input state (EOF), so it shouldn't matter which one it thinks 
										// is selected
										stage.setSelectedInputStreamNo(0);
										unblock(stage, 0);
									}
								}
								else {
									// have to read from a specific stream...
									stream = stage.getInputStream(selectedInputStreamNo);
									checkRendesvouzInput(stage, stream, selectedInputStreamNo);
								}
								break;
							case Pipe.STAGE_STATE_WAITING_OUTPUT:
								// waiting to write to an output stream...
								stream = stage.getOutputStream(stage.getSelectedOutputStreamNo());

								if (stream.atOutputEOF())
									checkRendesvouzOutput(stage, 0);
								else {
//									selectedInputStreamNo = stream.getConsumer().getSelectedInputStreamNo();
//									if (selectedInputStreamNo == ANYINPUT) {
//										// we can read from any stream...
//										int numStreams = stage.getNumberOfInputStreams();
//										for (int z = 0; z < numStreams; z++) {
//											if (checkRendesvouzOutput(stage, z))
//												break;
//										}
//									}
//									else {
//										// use only the stream that this stage is trying to write to
//										checkRendesvouzOutput(stage, selectedInputStreamNo);
//									}
								}
								break;
							case Pipe.STAGE_STATE_WAITING_SUBROUTINE:
								if (!stage.getOwningPipeline().isStillRunning())
									stage.setState(Pipe.STAGE_STATE_RUNNING);
								break;
							case Pipe.STAGE_STATE_WAITING_COMMIT:
								// do nothing because we only handle commits if all stages are
								// waiting to commit
								break;
							case Pipe.STAGE_STATE_WAITING_EXTERNAL:
								// should never happen...
								break;
							case Pipe.STAGE_STATE_READY:
							case Pipe.STAGE_STATE_TERMINATED:
							default:
								;  // nothing
							}
						}
						
						if (allWaitingForCommit) {
							if (_pipe.hasListener())
								_pipe.getListener().handleEvent(new PipeEventPipelineCommit(pipeline, aggregateRC, 0)); // TODO commitlevel
							ArrayList<Stage> copy = new ArrayList<Stage>(stages);
							for (int j = 0; j < copy.size(); j++) {
								Stage stage = copy.get(j);
								runList.commitStage(stage, Integer.valueOf(stage.getCommitLevel()), Integer.valueOf(stage.getBlockedCommitLevel()));
							}
							unblock(copy, aggregateRC);
						}
					}
				}
			}
			if (!everythingRunning)
				break;
		}
		
		int rc = 0;
		if (stalled)
			rc = 29;
		
		for (int i = 0; i < _pipelines.size(); i++) {
			Pipeline pipeline = (Pipeline)_pipelines.get(i);
			int pipeRC = pipeline.getAggregateRC();
			if (rc < 0 || pipeRC < 0)
				rc = Math.min(rc, pipeRC);
			else
				rc = Math.max(rc, pipeRC);
		}

		return rc;
	}
	
	// ------------------------------------------------------------------------------------------------------------
	// Called from stages
	// ------------------------------------------------------------------------------------------------------------
	protected void commit(Stage stage, int oldCommitLevel, int commitLevel) throws PipeException {
		if (_pipe.hasListener())
			_pipe.getListener().handleEvent(new PipeEventDispatcherCall(stage, commitLevel, 0, PipeEvent.CODE_COMMIT));
		// wait for all the others to be at that level...
		//debugDispatcher(stage, "blocking to commitlevel "+commitLevel);
		stage.setBlockedCommitLevel(commitLevel);
		block(stage, Pipe.STAGE_STATE_WAITING_COMMIT);
	}

	protected String peekto(Stage stage) throws PipeException {
		if (_pipe.hasListener())
			_pipe.getListener().handleEvent(new PipeEventDispatcherCall(stage, 0, 0, PipeEvent.CODE_PEEKTO));
		Stream stream = null;
		int selectedInputStreamNo = stage.getSelectedInputStreamNo();
		if (selectedInputStreamNo != ANYINPUT) {
			stream = stage.getInputStream(selectedInputStreamNo);
			if (isAtInputEOF(stage, stream)) {
				stage.RC = 12;
				return null;
			}
		}
		
		if (debug)
			debugStage(stage, stream, "peeking");
		block(stage, Pipe.STAGE_STATE_WAITING_PEEKTO);
		if (debug)
			debugStage(stage, stream, "unblocked");
		
		// the selected stream no may have been changed (if it was ANYINPUT)
		// so we just need to re-query it
		selectedInputStreamNo = stage.getSelectedInputStreamNo();
		stream = stage.getInputStream(selectedInputStreamNo);
		if (isAtInputEOF(stage, stream)) {
			stage.RC = 12;
			return null;
		}
		if (debug)
			debugStage(stage, stream, "peekto returned: "+stream.getConsumerRecord());
		return stage.getInputStream(selectedInputStreamNo).getConsumerRecord();
	}
	
	protected String readto(Stage stage) throws PipeException {
		if (_pipe.hasListener())
			_pipe.getListener().handleEvent(new PipeEventDispatcherCall(stage, 0, 0, PipeEvent.CODE_READTO));
		Stream stream = null;
		int selectedInputStreamNo = stage.getSelectedInputStreamNo();
		if (selectedInputStreamNo != ANYINPUT) {
			stream = stage.getInputStream(selectedInputStreamNo);
			if (isAtInputEOF(stage, stream)) {
				stage.RC = 12;
				return null;
			}
		}
		
		if (debug)
			debugStage(stage, stream,"reading");
		block(stage, Pipe.STAGE_STATE_WAITING_READTO);
		if (debug)
			debugStage(stage, stream,"unblocked");
		
		// the selected stream no may have been changed (if it was ANYINPUT)
		// so we just need to re-query it
		selectedInputStreamNo = stage.getSelectedInputStreamNo();
		stream = stage.getInputStream(selectedInputStreamNo);
		if (isAtInputEOF(stage, stream)) {
			stage.RC = 12;
			return null;
		}
		if (debug)
			debugStage(stage, stream,"readto returned: "+stream.getConsumerRecord());
		return stage.getInputStream(selectedInputStreamNo).consumeRecord();
	}
	
	protected void output(Stage stage, String record) throws PipeException {
		if (_pipe.hasListener())
			_pipe.getListener().handleEvent(new PipeEventDispatcherCall(stage, record.length(), 0, PipeEvent.CODE_OUTPUT));
		Stream stream = stage.getOutputStream(stage.getSelectedOutputStreamNo());
		if (debug)
			debugStage(stage, stream, "writing: "+record);
		if (isAtOutputEOF(stage, stream)) {
			stage.RC = 12;
			return;
		}
		stream.setProducerRecord(record);
		block(stage, Pipe.STAGE_STATE_WAITING_OUTPUT);
		if (debug)
			debugStage(stage, stream, "unblocked");
	}

	protected synchronized void shortStages(Stage whoCalled, int inputStreamNo, int outputStreamNo) {
		Stream inputStream = whoCalled.getInputStream(inputStreamNo);
		Stream outputStream = whoCalled.getOutputStream(outputStreamNo);
		// now connect our input stream to our output stream
		shortStages(whoCalled, inputStream.getProducer(), inputStream.getProducerStreamNo(), inputStream.getProducerStreamId(), outputStream.getConsumer(), outputStream.getConsumerStreamNo(), outputStream.getConsumerStreamId());
		// and make sure that those streams are now at EOF for this stage
		whoCalled.setInputStream(inputStreamNo, new Stream(null, null));
		whoCalled.setOutputStream(outputStreamNo, new Stream(null, null));
	}
	
	// note that this is synchronized to protect against the streams being modified
	// by another thread (eg. terminate) half-way through
	protected synchronized void shortStages(Stage whoCalled, Stage producer, int producerStreamNo, String producerStreamId, Stage consumer, int consumerStreamNo, String consumerStreamId) {
		if (_pipe.hasListener())
			_pipe.getListener().handleEvent(new PipeEventDispatcherCall(whoCalled, 0, 0, PipeEvent.CODE_SHORT));
		// if we have a valid producer, then we will use their stream as the new
		// stream to connect the two stages (especially as they may already have
		// written a record using output() and be waiting on someone to consume it)
		if (producer != null) {
			Stream stream = producer.getOutputStream(producerStreamNo);
			// now connect it to the new destination
			stream.setConsumer(consumer, consumerStreamNo, consumerStreamId);
			// and if the destination is still not at EOF, then make
			// sure it points back to the new producer
			if (consumer != null)
				consumer.setInputStream(consumerStreamNo, stream);
		}
		else if (consumer != null) {
			// well, the producer is null (so, we must be the left-most
			// stage of the still running stages), so we have to connect
			// to the consumer's input stream.  And note that we don't have
			// to reset the output stream of the producer because if the 
			// producer was not-null, we wouldn't be in this if-clause!
			Stream stream = consumer.getInputStream(consumerStreamNo);
			stream.setProducer(producer, producerStreamNo, producerStreamId);
		}
	}

	protected void addpipe(Stage stage, String pipelineSpec) throws PipeException {
		// TODO
		if (_pipe.hasListener())
			_pipe.getListener().handleEvent(new PipeEventDispatcherCall(stage, 4, 0, PipeEvent.CODE_MISC));
		Pipeline pipeline = new Scanner(_pipe, stage, Pipe.PIPE_TYPE_ADDPIPE, pipelineSpec).getPipeline();
		pipeline.setCallingStage(stage);		
	}
	
	protected void callpipe(Stage stage, String pipelineSpec) throws PipeException {
		if (_pipe.hasListener())
			_pipe.getListener().handleEvent(new PipeEventDispatcherCall(stage, 4, 0, PipeEvent.CODE_MISC));
		Pipeline pipeline = new Scanner(_pipe, stage, Pipe.PIPE_TYPE_CALLPIPE, pipelineSpec).getPipeline();
		if (_pipe.hasListener())
			_pipe.getListener().handleEvent(new PipeEventSubroutineWaiting(pipeline, stage));

		ArrayList<Scanner.Connector> connectors = pipeline.getConnectors();
		for (int i = 0; i < connectors.size(); i++) {
			Scanner.Connector connector = connectors.get(i);
			if (connector.getDirection() == INPUT) {
				Stream stream = stage.getInputStream(connector.getConnectorStreamNo());
				if (stream == null)
					throw new PipeException(-102, connector.getConnectorStreamNo());
				else {
					stream.push(true);
					stream.setConsumer(connector.getNewStage(), connector.getNewStageStreamNo(), connector.getNewStageStreamId());
					connector.getNewStage().setInputStream(connector.getNewStageStreamNo(), stream);
				}
			}
			else {
				Stream stream = stage.getOutputStream(connector.getConnectorStreamNo());
				if (stream == null)
					throw new PipeException(-102, connector.getConnectorStreamNo());
				else {
					stream.push(true);
					stream.setProducer(connector.getNewStage(), connector.getNewStageStreamNo(), connector.getNewStageStreamId());
					connector.getNewStage().setOutputStream(connector.getNewStageStreamNo(), stream);
				}
			}
		}
		addPipeline(pipeline);
		pipeline.setCallingStage(stage);		
		block(stage, Pipe.STAGE_STATE_WAITING_SUBROUTINE);
	}

	// note that this is synchronized to protect against the streams being modified
	// by another thread (eg. shotStreams) half-way through
	protected synchronized void terminate(Stage stage, int rc) {
		if (_pipe.hasListener())
			_pipe.getListener().handleEvent(new PipeEventStageEnd(stage, rc));
		if (debug)
			debugStage(stage, null, "terminated. rc="+rc);
		// log a message if required
		if (stage.getOptions().listRC || stage.getOptions().trace || (rc != 0 && stage.getOptions().listErr))
			_pipe.issueMessage(20, Pipe.MODULE_STAGE, new String[] { ""+rc }, stage);

		stage.getOwningPipeline().updateAggregateRC(stage, rc);
		stage.setState(Pipe.STAGE_STATE_TERMINATED);
		stage.SEMAPHORE.release();
		
		RunList runList = getRunList(stage.getOwningPipeline());
		synchronized(stage.getOwningPipeline()) {
			runList.removeStage(stage);
		}

		int inputStreamCount = stage.getNumberOfInputStreams();
		for (int k = 0; k < inputStreamCount; k++) {
			Stream stream = (Stream)stage.getInputStream(k);
			stream.severInput();
		}
		int outputStreamCount = stage.getNumberOfOutputStreams();
		for (int k = 0; k < outputStreamCount; k++) {
			Stream stream = stage.getOutputStream(k);
			stream.severOutput();
		}
	}

	protected Object getParameter(String parameterName) {
		return _parameters.get(parameterName);
	}
	protected void putParameter(String parameterName, Object parameter) {
		 _parameters.put(parameterName, parameter);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Internal dispatcher functions
	// ------------------------------------------------------------------------------------------------------------
	
	/**
	 * Called when a new pipeline needs to be managed by the dispatcher.
	 * Examples of this are when we create a brand new pipe to run, or
	 * when a sub-pipe is added via callpipe and addpipe
	 */
	void addPipeline(Pipeline pipeline) {
		if (_pipe.hasListener())
			_pipe.getListener().handleEvent(new PipeEventPipelineBegin(pipeline));
		pipeline.setDispatcher(this);
		pipeline.setPipelineNumber(_pipelines.size()+1);
		_runlists.add(new RunList(pipeline));
		_pipelines.add(pipeline);
	}
	
	/**
	 * Start a stage
	 */
	private void start(Stage stage) {
		// we don't really have a storage address, but for the sake
		// of conformity, we will output a message
		if (stage.getOptions().listRC || stage.getOptions().trace)
			_pipe.issueMessage(28, Pipe.MODULE_STAGE, new String[] { "00000000" }, stage);

		// and away we go...
		stage.setState(Pipe.STAGE_STATE_RUNNING);
		if (_pipe.hasListener())
			_pipe.getListener().handleEvent(new PipeEventStageStart(stage));
		Thread thread = new Thread(stage);
		thread.setName(stage.getNameAndArgs());
		thread.start();
	}
	
	/**
	 * Set the stage to the specified state, and tell the stage
	 * to stop doing anything for a little while
	 */
	private int block(Stage stage, int state) {
		stage.setState(state);
		try {
			stage.SEMAPHORE.acquire();
		}
		catch(InterruptedException e) {
		}
		return 0;
	}

	/**
	 * Let a stage know that they are ok to proceed again
	 */
	private void unblock(Stage stage, int rc) {
		stage.setState(Pipe.STAGE_STATE_RUNNING);
		synchronized(stage) {
			if (_pipe.hasListener())
				_pipe.getListener().handleEvent(new PipeEventDispatcherResume(stage, null, rc, "XX"));  // TODO stream and code
			stage.RC = rc;
			stage.SEMAPHORE.release();
		}
	}
	
	/**
	 * Let 2 stages know that they are ok to proceed again.  Note 
	 * that you can't just call unblock twice, because the first guy
	 * you unblock may then be dispatched and try to read/write from
	 * the second guy, which may still be in the previous state.
	 * For example, the producer get unblocked first, and writes a new
	 * record before the previous one has had a chance to be processed
	 * by the consumer.
	 */
	private void unblock(Stage stage1, Stage stage2, int rc1, int rc2) {
		stage1.setState(Pipe.STAGE_STATE_RUNNING);
		stage2.setState(Pipe.STAGE_STATE_RUNNING);
		stage1.RC = rc1;
		stage2.RC = rc2;
		if (_pipe.hasListener()) {
			_pipe.getListener().handleEvent(new PipeEventDispatcherResume(stage1, null, rc1, "XX"));  // TODO stream and code
			_pipe.getListener().handleEvent(new PipeEventDispatcherResume(stage2, null, rc2, "XX"));  // TODO stream and code
		}
		stage1.SEMAPHORE.release();
		stage2.SEMAPHORE.release();
	}

	/**
	 * Let many stages unblock... see comment for unblock(Stage, Stage)
	 */
	private void unblock(ArrayList<Stage> stages, int rc) {
		for (int i = 0; i < stages.size(); i++) {
			Stage stage = stages.get(i);
			stage.RC = rc;
			stage.setState(Pipe.STAGE_STATE_RUNNING);
		}
		for (int i = 0; i < stages.size(); i++) {
			Stage stage = (Stage)stages.get(i);
			if (_pipe.hasListener())
				_pipe.getListener().handleEvent(new PipeEventDispatcherResume(stage, null, rc, "XX"));  // TODO stream and code
			stage.SEMAPHORE.release();
		}
	}
	
	/**
	 * Is the passed stream at input EOF?
	 */
	private boolean isAtInputEOF(Stage stage, Stream stream) {
		if (stream.atInputEOF()) {
			if (debug)
				debugStage(stage, stream, "at input EOF");
			return true;
		}
		else
			return false;
	}
	
	/**
	 * Is the passed stream at output EOF?
	 */
	private boolean isAtOutputEOF(Stage stage, Stream stream) {
		if (stream.atOutputEOF()) {
			if (debug)
				debugStage(stage, stream, "at output EOF");
			return true;
		}
		else
			return false;
	}
	
	/**
	 * Cause a pipeline to stall
	 */
	private void stall(Pipeline pipeline, Stage stage) {
		if (_pipe.hasListener()) {
			_pipe.getListener().handleEvent(new PipeEventDispatcherCall(stage, 0, 0, PipeEvent.CODE_MISC));
			_pipe.getListener().handleEvent(new PipeEventPipelineStall(pipeline));
		}
		_pipe.issueMessage(29, "PIPSTA", new String[] {});
		ArrayList<Stage> stages = pipeline.getStages();
		for (int i = 0; i < stages.size(); i++)
			terminate((Stage)stages.get(i), -4095);
		unblock(stages, -4095);
	}
	
	/**
	 * Checks for a READTO/PEEKTO rendesvouz.  Returns a boolean indicating 
	 * whether we actually did anything in response to the current state
	 */
	private boolean checkRendesvouzInput(Stage stage, Stream stream, int selectedInputStreamNo) {
		if (debug)
			debugDispatcher(stage, "XXXXTO: Checking input: "+stream);
		if (stream.atInputEOF()) {
			// if we are at input EOF (ie. no producer any more) then
			// we can unblock this guy and get out
			if (debug)
				debugDispatcher(stage, "XXXXTO: Unblocking due to input EOF: "+selectedInputStreamNo);
			stage.setSelectedInputStreamNo(selectedInputStreamNo);
			unblock(stage, 0);
			return true;
		}
		else {
			Stage producer = stream.getProducer();
			if (producer != null && producer.getState() == Pipe.STAGE_STATE_WAITING_OUTPUT) {
				// now we have to make sure that the guy at the other end of the 
				// stream is trying to write to *this* stream.  He could be in 
				// OUTPUT state, but trying to write to a different stream
				Stream producerOutputStream = producer.getOutputStream(producer.getSelectedOutputStreamNo());
				if (producerOutputStream.equals(stream)) {
					stream.copyProducerRecordToConsumer();
					if (stage.getState() == Pipe.STAGE_STATE_WAITING_READTO) {
						if (debug)
							debugDispatcher(stage, "READTO: Unblocking producer: \""+stream.getProducer()+"\" and consumer: \""+stage+"\": "+selectedInputStreamNo);
						stage.setSelectedInputStreamNo(selectedInputStreamNo);
						unblock(stage, stream.getProducer(), 0, stage.getOutputRC(0));
						return true;
					}
					else {
						if (debug)
							debugDispatcher(stage, "PEEKTO: Unblocking consumer: \""+stage+"\": "+selectedInputStreamNo);
						stage.setSelectedInputStreamNo(selectedInputStreamNo);
						unblock(stage, 0);
						return true;
					}
				}
			}
			else {
				if (debug)
					debugDispatcher(stage, "Nothing to do");
			}
		}
		return false;
	}
	
	/**
	 * Checks for a OUTPUT rendesvouz.  Returns a boolean indicating 
	 * whether we actually did anything in response to the current state
	 */
	private boolean checkRendesvouzOutput(Stage stage, int selectedInputStreamNo) {
		Stream stream = stage.getOutputStream(stage.getSelectedOutputStreamNo());
		if (debug)
			debugDispatcher(stage, "OUTPUT: Writing to "+stream);
		if (stream.atOutputEOF()) {
			if (debug)
				debugDispatcher(stage, "OUTPUT: Unblocking due to output EOF");
			unblock(stage, 0);
			return true;
		}
		else {
			Stage consumer = stream.getConsumer();
			if (consumer.getState() == Pipe.STAGE_STATE_WAITING_PEEKTO) {
				// now we have to make sure that the guy at the other end of the 
				// stream is trying to read from *this* stream.  He could be in 
				// PEEKTO state, but trying to read from a different stream
				Stream consumerInputStream = consumer.getInputStream(selectedInputStreamNo);
				if (consumerInputStream.equals(stream)) {
					if (debug)
						debugDispatcher(stage, "OUTPUT: Unblocking consumer: \""+stream.getConsumer()+"\" that was waiting for peekto: "+selectedInputStreamNo);
					stream.copyProducerRecordToConsumer();
					consumer.setSelectedInputStreamNo(selectedInputStreamNo);
					unblock(stream.getConsumer(), 0);
					return true;
				}
				else {
					if (debug)
						debugDispatcher(stage, "Nothing to do");
				}
			}
			else if (stream.getConsumer() != null && stream.getConsumer().getState() == Pipe.STAGE_STATE_WAITING_READTO) {
				// now we have to make sure that the guy at the other end of the 
				// stream is trying to read from *this* stream.  He could be in 
				// PEEKTO state, but trying to read from a different stream
				Stream consumerInputStream = consumer.getInputStream(selectedInputStreamNo);
				if (consumerInputStream.equals(stream)) {
					if (debug)
						debugDispatcher(stage, "OUTPUT: Unblocking producer: \""+stage+"\" and consumer: \""+stream.getConsumer()+"\" that was waiting for READTO: "+selectedInputStreamNo);
					stream.copyProducerRecordToConsumer();
					consumer.setSelectedInputStreamNo(selectedInputStreamNo);
					unblock(stage, stream.getConsumer(), stream.getConsumer().getOutputRC(0), 0);
					return true;
				}
				else {
					if (debug)
						debugDispatcher(stage, "Nothing to do");
				}
			}
			else {
				if (debug)
					debugDispatcher(stage, "Nothing to do");
			}
		}
		return false;
	}

	private void debugStage(Stage stage, Stream stream, String message) {
		if (debug) {
			String indent = "";
			for (int i = 0; i < stage.getStageNumber(); i++)
				indent += "                     ";
			System.out.println(indent+stage+"/"+stream+": "+message);
		}
	}
	private void debugDispatcher(Stage stage, String message) {
		if (debug)
			System.out.println("  DISPATCHER> "+stage+": "+message);
	}
	private void debugState(Pipeline pipeline) {
		if (debug) {
			ArrayList<Stage> stages = getRunList(pipeline).getStagesAtLowestCommitLevel();
			String s = "";
			for (int i = 0; i < stages.size(); i++) {
				if (i != 0)
					s += " | ";
				Stage stage = (Stage)stages.get(i);
				s += stage+": "+stage.getPrintableState();
			}
			System.out.println("  ==> "+s);
		}
	}
	
	static Stream connectStages(Stage producer, int producerStreamNo, String producerStreamId, Stage consumer, int consumerStreamNo, String consumerStreamId) {
		Stream stream = new Stream(producer, consumer);
		if (producer != null) {
			stream.setProducer(producer, producerStreamNo, producerStreamId);
			producer.setOutputStream(producerStreamNo, stream);
		}
		if (consumer != null) {
			stream.setConsumer(consumer, consumerStreamNo, consumerStreamId);
			consumer.setInputStream(consumerStreamNo, stream);
		}
		return stream;
	}
	
	
	private class RunList {
		/**
		 * This is the run-list of stages that are grouped by commitlevel. 
		 * The key is an Integer (representing the commitLevel), and the
		 * value is an ArrayList of Stages that are at that commit level
		 */
		private TreeMap<Integer, ArrayList<Stage>> _runList = new TreeMap<Integer, ArrayList<Stage>>();

		public RunList(Pipeline pipeline) {
			for (int i = 0; i < pipeline.getStages().size(); i++) {
				Stage stage = (Stage)pipeline.getStages().get(i);
				getStagesForCommitLevel(Integer.valueOf(stage.getCommitLevel())).add(stage);
			}
		}
		
		/**
		 * Return a collection of stages at a certain commit level
		 * (and creates an empty collection if there are none at that level)
		 */
		private ArrayList<Stage> getStagesForCommitLevel(Integer commitLevel) {
			ArrayList<Stage> list = _runList.get(commitLevel);
			if (list == null) {
				list = new ArrayList<Stage>();
				_runList.put(commitLevel, list);
			}
			return list;
		}
		private int getLowestCommitLevel() {
			return ((Integer)_runList.firstKey()).intValue();
		}
		ArrayList<Stage> getStagesAtLowestCommitLevel() {
			return getStagesForCommitLevel(Integer.valueOf(getLowestCommitLevel()));
		}
		void commitStage(Stage stage, Integer oldCommitLevel, Integer newCommitLevel) {
			ArrayList<Stage> existingStages = getStagesForCommitLevel(oldCommitLevel);
			existingStages.remove(stage);
			if (existingStages.size() == 0) {
				_runList.remove(oldCommitLevel);
			}
			getStagesForCommitLevel(newCommitLevel).add(stage);
			stage.setCommitLevel(newCommitLevel.intValue());
		}
		void removeStage(Stage stage) {
			ArrayList<Stage> existingStages = getStagesForCommitLevel(Integer.valueOf(stage.getCommitLevel()));
			existingStages.remove(stage);
			if (existingStages.size() == 0)
				_runList.remove(Integer.valueOf(stage.getCommitLevel()));
		}
	}
	
	private RunList getRunList(Pipeline pipeline) {
		return (RunList)_runlists.get(pipeline.getPipelineNumber()-1);
	}
}
