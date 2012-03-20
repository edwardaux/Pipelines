package com.hae.pipe;

import java.util.*;

public abstract class Stage extends PipeArtifact implements Runnable, PipeConstants {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private int _state = Pipe.STAGE_STATE_COMMITTED;
	private String _args;
	private String _label;
	private int _stageNumber;
	private ArrayList<Stream> _inputStreams = new ArrayList<Stream>();
	private ArrayList<Stream> _outputStreams = new ArrayList<Stream>();
	private int _selectedInputStreamNo = 0;
	private int _selectedOutputStreamNo = 0;
	private int _commitLevel = Pipe.COMMIT_MIN;
	private int _blockedCommitLevel = Pipe.COMMIT_MIN;
	private boolean _signalOnError = false;
	public Semaphore SEMAPHORE = new Semaphore(0);
	
	/**
	 * Holds the return code for the last operation
	 */
	protected int RC = 0;

	/**
	 * The pipeline that this stage belongs to
	 */
	private Pipeline _owningPipeline;
	
	/**
	 * The pipe that owns everything
	 */
	private Pipe _pipe;
	
	/**
	 * The dispatcher running the show
	 */
	private Dispatcher _dispatcher;
	
	/**
	 * The local options for this stage
	 */
	private Options _options;

	/**
	 * The current eofreport setting
	 */
	private int _eofreport = CURRENT;
	
	/**
	 * The value that the producer sees when it does its next OUTPUT
	 */
	private int _outputRC = 0;
	private boolean _outputRCSet = false;
	
	/**
	 * Should we autocommit on output/readto/peekto?
	 */
	private boolean _autoCommit = true;
	
	public Stage() {
		_inputStreams.add(new Stream(null, this));
		_outputStreams.add(new Stream(this, null));
	}
	
	public abstract int execute(String args) throws PipeException;
	
	public void run() {
		// OK... let's start this puppy...
		int rc = 0;
		try {
			rc = execute(_args);
		}
		catch(PipeException e) {
			// this isn't that bad... it just means that the stage didn't 
			// handle it in the execute() method
			_pipe.issueMessage(e.getMessageNo(), Pipe.MODULE_STAGE, e.getParms());
			rc = e.getMessageNo();
		}
		catch(Throwable t) {
			// if any exception gets thrown, then we need make sure we tidy up.
			_pipe.issueMessage(5001, Pipe.MODULE_STAGE, new String[] { t.getMessage() });
			rc = 5001;
			t.printStackTrace();
		}
		finally {
			// tell the dispatcher that we have finished
			_dispatcher.terminate(this, rc);
		}
	}
	
	// internal APIs
	Pipeline getOwningPipeline() {
		return _owningPipeline;
	}
	void setOwningPipeline(Pipeline owningPipeline) {
		_owningPipeline = owningPipeline;
	}
	protected Options getOptions() {
		return _options;
	}
	void setOptions(Options options) {
		_options = options;
	}
	Pipe getPipe() {
		return _pipe;
	}
	void setPipe(Pipe pipe) {
		_pipe = pipe;
	}
	Dispatcher getDispatcher() {
		return _dispatcher;
	}
	void setDispatcher(Dispatcher dispatcher) {
		_dispatcher = dispatcher;
	}
	int getState() {
		return _state;
	}
	void setState(int state) {
		_state = state;
	}
	String getPrintableState() {
		switch(_state) {
		case Pipe.STAGE_STATE_COMMITTED: 
			return "CREATED";
		case Pipe.STAGE_STATE_NOT_STARTED:
			return "NOT_STARTED";
		case Pipe.STAGE_STATE_RUNNING:
			return "RUNNING";
		case Pipe.STAGE_STATE_WAITING_PEEKTO:
			return "PEEKTO";
		case Pipe.STAGE_STATE_WAITING_READTO:
			return "READTO";
		case Pipe.STAGE_STATE_WAITING_OUTPUT:
			return "OUTPUT";
		case Pipe.STAGE_STATE_WAITING_SUBROUTINE:
			return "SUBROUTINE";
		case Pipe.STAGE_STATE_WAITING_COMMIT:
			return "COMMIT";
		case Pipe.STAGE_STATE_WAITING_EXTERNAL:
			return "EXTERNAL";
		case Pipe.STAGE_STATE_READY:
			return "READY";
		case Pipe.STAGE_STATE_TERMINATED:
			return "TERMINATED";
		case Pipe.STAGE_STATE_NON_DISPATCHABLE:
			return "NONDISPATCH";
		default: 
			return "UNKNOWN";
		}
	}
	int getStageNumber() {
		return _stageNumber;
	}
	void setStageNumber(int stageNumber) {
		_stageNumber = stageNumber;
	}
	String getArgs() {
		return _args;
	}
	void setArgs(String args) {
		_args = args;
	}
	String getName() {
		return Scanner.reverseLookupStage(getClass());
	}
	String getNameAndArgs() {
		return getName()+" "+getArgs();
	}
	String getLabel() {
		return _label;
	}
	void setLabel(String label) {
		_label = label;
	}
	int getNumberOfInputStreams() {
		return _inputStreams.size();
	}
	int getNumberOfOutputStreams() {
		return _outputStreams.size();
	}
	int getSelectedInputStreamNo() {
		return _selectedInputStreamNo;
	}
	int getSelectedOutputStreamNo() {
		return _selectedOutputStreamNo;
	}
	void setSelectedInputStreamNo(int selectedInputStreamNo) {
		_selectedInputStreamNo = selectedInputStreamNo;
	}
	Stream getInputStream(int streamNo) {
		return (Stream)_inputStreams.get(streamNo);
	}
	int getInputStreamNo(String streamId) {
		// maybe the streamId is numeric?
		if (Syntax.isNumber(streamId)) {
			int streamNo = Integer.parseInt(streamId);
			if (streamNo < _inputStreams.size())
				return streamNo;
			else
				return -1;
		}
		
		// nope, must be a stream id
		for (int i = 0; i < _inputStreams.size(); i++) {
			Stream stream = getInputStream(i);
			if (stream.getConsumer() == this && streamId.equals(stream.getConsumerStreamId()))
					return i;
		}
		return -1;
	}
	Stream getOutputStream(int streamNo) {
		return (Stream)_outputStreams.get(streamNo);
	}
	int getOutputStreamNo(String streamId) {
		// maybe the streamId is numeric?
		if (Syntax.isNumber(streamId)) {
			int streamNo = Integer.parseInt(streamId);
			if (streamNo < _outputStreams.size())
				return streamNo;
			else
				return -1;
		}
		
		// nope, must be a stream id
		for (int i = 0; i < _outputStreams.size(); i++) {
			Stream stream = getOutputStream(i);
			if (stream.getProducer() == this && streamId.equals(stream.getProducerStreamId()))
					return i;
		}
		return -1;
	}
	void setInputStream(int streamNo, Stream stream) { 
		_inputStreams.set(streamNo, stream);
	}
	void setOutputStream(int streamNo, Stream stream) { 
		_outputStreams.set(streamNo, stream);
	}
	int getCommitLevel() {
		return _commitLevel;
	}
	void setCommitLevel(int commitLevel) {
		_commitLevel = commitLevel;
	}
	int getBlockedCommitLevel() {
		return _blockedCommitLevel;
	}
	void setBlockedCommitLevel(int blockedCommitLevel) {
		_blockedCommitLevel = blockedCommitLevel;
	}
	int getOutputRC(int suggestedRC) {
		if (_outputRCSet) {
			_outputRCSet = false;
			return _outputRC;
		}
		else
			return suggestedRC;
	}

	private boolean hasConnectedInputStream() {
		for (int i = 0; i < _inputStreams.size(); i++) {
			Stream stream = (Stream)_inputStreams.get(i);
			if (!stream.atInputEOF() && !stream.atOutputEOF()) {
				return true;
			}
		}
		return false;
	}
	private boolean hasConnectedOutputStream() {
		for (int i = 0; i < _outputStreams.size(); i++) {
			Stream stream = (Stream)_outputStreams.get(i);
			if (!stream.atInputEOF() && !stream.atOutputEOF()) {
				return true;
			}
		}
		return false;
	}
	private void enterCommand(String command) {
		RC = 0;
		if (_options.trace || _options.listCmd)
			_pipe.issueMessage(34, Pipe.MODULE_TRACE, new String[] { command });
	}
	protected int exitCommand(PipeException e) throws PipeException {
		if (e.getParms() == null || e.getParms().length == 0)
			return exitCommand(e.getMessageNo());
		else
			return exitCommand(e.getMessageNo(), e.getParms()[0]);
	}
	protected int exitCommand(int rc) throws PipeException {
		return exitCommand(rc, true);
	}
	protected int exitCommand(int rc, boolean noisy) throws PipeException {
		return exitCommand(rc, null, noisy);
	}
	protected int exitCommand(int rc, String parm1) throws PipeException {
		return exitCommand(rc, parm1, true);
	}
	protected int exitCommand(int rc, String parm1, boolean noisy) throws PipeException {
		if (noisy && _options.trace)
			_pipe.issueMessage(31, Pipe.MODULE_TRACE, new String[] { ""+rc });

		RC = rc;
		if (rc < 0 && noisy) {
			// if we are signalling, then we don't need to output the error
			// message (because the error handling of the stage will do that
			// for us.)  However, if we are not going to throw an exception,
			// then we need manually dump out the message
			if (_signalOnError)
				throw new PipeException(rc, parm1);
			else
				_pipe.issueMessage(rc, Pipe.MODULE_STAGE, new String[] { parm1 });
		}
		return rc;
	}
	
	// --------------------------------------------------------------------------------------------------------------
	// public APIs...
	// --------------------------------------------------------------------------------------------------------------
	protected final void signalOnError() {
		_signalOnError = true;
	}
	
	protected final void addpipe(String pipelineSpec) throws PipeException {
		enterCommand("ADDPIPE");
		_dispatcher.addpipe(this, pipelineSpec);
	}

	/**
	 * Adds a new input and output stream 
	 */
	protected final int addStream() throws PipeException {
		return addStream(BOTH, null);
	}

	/**
	 * Adds a new stream based on the value of direction
	 * @param direction One of the constants: BOTH, INPUT or OUTPUT
	 */
	protected final int addStream(int direction) throws PipeException {
		return addStream(direction, null);
	}
	
	/**
	 * Adds a new stream based on the value of direction
	 * @param direction One of the constants: BOTH, INPUT or OUTPUT
	 * @param streamId A stream identifier for the new stream(s). A valid 
	 *                 stream contains up to 4 alpha-numeric characters
	 *                 (with no blanks)
	 */
	protected final int addStream(int direction, String streamId) throws PipeException {
		enterCommand("ADDSTREAM");
		
		if (!Syntax.isValidDirection(direction))
			return exitCommand(-164, ""+direction);
			
		// verify that the stream id is valid
		if (streamId != null) {
			// syntactic check first
			if (!Syntax.isStreamId(streamId))
				return exitCommand(-165, streamId);
		
			// now lets check to see if the stream already exists
			if ((direction & INPUT) > 0) {
				if (getInputStreamNo(streamId) != -1)
					return exitCommand(-174, streamId);
			}
			if ((direction & OUTPUT) > 0) {
				if (getOutputStreamNo(streamId) != -1)
					return exitCommand(-174, streamId);
			}
		}

		if ((direction & INPUT) > 0) {
			_inputStreams.add(new Stream(null, null));
			Dispatcher.connectStages(null, 0, "", this, _inputStreams.size()-1, streamId);
		}
		if ((direction & OUTPUT) > 0) {
			_outputStreams.add(new Stream(null, null));
			Dispatcher.connectStages(this, _outputStreams.size()-1, streamId, null, 0, "");
		}
		
		return exitCommand(0);
	}

	/**
	 * Invokes a synchronous pipeline
	 * @param The pipeline to launch
	 */
	protected final int callpipe(String pipelineSpec) throws PipeException {
		if (_commitLevel < 0 && _autoCommit)
			_dispatcher.commit(this, _commitLevel, 0);

		enterCommand("CALLPIPE");
		try {
			_dispatcher.callpipe(this, pipelineSpec);
			return exitCommand(0);
		}
		catch(PipeException e) {
			return exitCommand(e);
		}
	}
	
	/**
	 * Commit this stage to a new commit level.  This function will
	 * block until all other stages in this pipeline have also committed
	 * to the same level.
	 * @param commitLevel The requested new commit level
	 */
	protected final int commit(int commitLevel) throws PipeException {
		enterCommand("COMMIT");
		
		// we only try to commit if we are heading to a higher level
		if (commitLevel > _commitLevel) {
			_dispatcher.commit(this, _commitLevel, commitLevel);
		
			if (_options.trace)
				_pipe.issueMessage(537, Pipe.MODULE_STAGE, new String[] { ""+commitLevel }, this);
		}

		return exitCommand(_owningPipeline.getAggregateRC());
	}
	
	/**
	 * Returns a string from the passed input based on a range. Note
	 * that it is a destructive read!
	 * @param range The range that describes the expected input
	 * @param input The string to read from
	 */
	protected final String getRange(Range range, String input) throws PipeException {
		enterCommand("GETRANGE");
		return range.extractRange(input);
	}

	/**
	 * Controls how EOF is reported for streams
	 * @param streamType One of the constants: CURRENT, ANY or ALL
	 */
	protected final int eofReport(int streamType) throws PipeException {
		enterCommand("EOFREPORT");
		
		// make sure the user has passed valid data
		if (!Syntax.isValidStreamType(streamType))
			return exitCommand(-111, ""+streamType);
		
		// now, lets be sure that there is at least one connected 
		// stream (either input or output).
		boolean connected = hasConnectedInputStream() || hasConnectedOutputStream();
		
		// now store it...
		_eofreport = streamType;
		
		// if no connected streams, then return code 8
		if (!connected)
			return exitCommand(8);
		else
			return exitCommand(0);
	}
	
	/**
	 * Issue a message (assumes no variables need to be substituted)
	 * @param messageNo The message number
	 * @param moduleId The 6-character moduleid for the message
	 */
	protected final int issueMessage(int messageNo, String moduleId) throws PipeException {
		return issueMessage(messageNo, moduleId, new String[] {});
	}
	
	/**
	 * Issue a message substituting the variables as required 
	 * @param messageNo The message number
	 * @param moduleId The 6-character moduleid for the message
	 * @param parms A collection of variables that will be substituted into the message text
	 */
	protected final int issueMessage(int messageNo, String moduleId, String[] parms) throws PipeException {
		enterCommand("ISSUEMSG");
		// we are only supposed to issue positive messages
		if (messageNo < 0)
			return exitCommand(-58, ""+messageNo);
		
		_pipe.issueMessage(messageNo, moduleId, parms, this);
		return exitCommand(messageNo);
	}
	
	/**
	 * Returns the highest-numbered stream
	 * @param direction One of the constants: INPUT or OUTPUT
	 */
	protected final int maxStream(int direction) throws PipeException {
		enterCommand("MAXSTREAM");
		
		if (!Syntax.isValidDirection(direction) || direction == BOTH)
			return exitCommand(-164, ""+direction);
		
		if (direction == INPUT)
			return exitCommand(_inputStreams.size()-1);
		else 
			return exitCommand(_outputStreams.size()-1);
	}

	/**
	 * Outputs an arbitrary message to the console
	 * @param message The message to output
	 */
	protected final int message(String message) throws PipeException {
		enterCommand("MESSAGE");
		_pipe.issueMessage(message);
		return exitCommand(0);
	}

	/**
	 * Tells the stage not to automatically commit on output/readto/peekto/select anyinput
	 */
	protected final int noCommit() throws PipeException {
		if (_commitLevel == 0)
			return exitCommand(8);
		
		if (_autoCommit == false || _commitLevel != Integer.MIN_VALUE)
			return exitCommand(4);
		
		return exitCommand(0);
	}
	
	protected final int output(String record) throws PipeException {
		if (_commitLevel < 0 && _autoCommit)
			_dispatcher.commit(this, _commitLevel, 0);
		
		if (_options.trace)
			_pipe.issueMessage(35, Pipe.MODULE_STAGE, new String[] { ""+record.length(), PipeUtil.trunc(record, 60) }, this);

		// TODO EOFREPORT handling
		
		// NOTE: the dispatcher sets the RC when it unblocks the stage
		_dispatcher.output(this, record);

		if (RC == 12 && _signalOnError)
			throw new EOFException();
		
		return exitCommand(RC);
	}

	protected final String peekto() throws PipeException {
		if (_commitLevel < 0 && _autoCommit)
			_dispatcher.commit(this, _commitLevel, 0);
		
		// TODO EOFREPORT handling

		// NOTE: the dispatcher sets the RC when it unblocks the stage
		String result = _dispatcher.peekto(this);

		if (RC == 12 && _signalOnError)
			throw new EOFException();
		
		return result;
	}
	
	protected final String readto() throws PipeException {
		if (_commitLevel < 0 && _autoCommit)
			_dispatcher.commit(this, _commitLevel, 0);
		
		if (_options.trace)
			_pipe.issueMessage(33, Pipe.MODULE_STAGE, new String[] { "0" }, this);

		// TODO EOFREPORT handling

		// NOTE: the dispatcher sets the RC when it unblocks the stage
		String result = _dispatcher.readto(this);
		
		if (RC == 12 && _signalOnError)
			throw new EOFException();

		return result;
	}
	/**
	 * Resolve entry point for stages.  RC=0 if not found, RC=1 otherwise
	 * @param entryPoint The name of the entry point that we are searching for
	 */
	protected final int resolve(String entryPoint) throws PipeException {
		enterCommand("RESOLVE");
		String[] words = PipeUtil.split(entryPoint);
		if (words.length == 0)
			return exitCommand(-42);
		else if (words.length != 1)
			return exitCommand(-112, entryPoint);

		try {
			Scanner.lookupStage(words[0]);
			return exitCommand(1);
		}
		catch(PipeException e) { 
			return exitCommand(0);
		}
	}
	
	/**
	 * Scans the input and returns the first Range object 
	 * @param required Indicates whether the range is mandatory or not
	 * @param input The input string
	 */
	protected final Range scanRange(PipeArgs pa, boolean required) throws PipeException {
		enterCommand("SCANRANGE");
		return Syntax.getRange(pa, required);
	}
	
	/**
	 * Scans the input and extracts one delimited string
	 * @param input The input to scan
	 */
	protected final String scanString(PipeArgs pa, boolean mandatory) throws PipeException {
		enterCommand("SCANSTRING");
		return pa.nextDelimString(mandatory);
	}

	/**
	 * Scans the input and extracts one space-separated word
	 * @param input The input to scan
	 */
	protected final String scanWord(PipeArgs pa) throws PipeException {
		enterCommand("SCANWORD");
		return pa.nextWord();
	}

	/**
	 * Selects a stream.  Changes the selected stream number for INPUT and OUTPUT
	 * @param streamNo A valid stream number, or the constant: ANYINPUT
	 */
	protected final int select(int streamNo) throws PipeException {
		return select(BOTH, streamNo);
	}
	
	/**
	 * Selects a stream.  Changes the selected stream number for INPUT and OUTPUT
	 * @param streamId A valid stream id
	 */
	protected final int select(String streamId) throws PipeException {
		return select(BOTH, streamId);
	}
	
	/**
	 * Selects a stream.  
	 * @param direction One of the constants: INPUT, OUTPUT, BOTH
	 * @param streamId A valid stream id
	 */
	protected final int select(int direction, String streamId) throws PipeException {
		// verify that the stream id is valid
		if (streamId != null) {
			// syntactic check first
			if (!Syntax.isStream(streamId))
				return exitCommand(-165, streamId);
		
			// now lets check to see if the stream already exists
			if ((direction & INPUT) > 0) {
				if (getInputStreamNo(streamId) == -1)
					return exitCommand(-178, streamId);
			}
			if ((direction & OUTPUT) > 0) {
				if (getOutputStreamNo(streamId) == -1)
					return exitCommand(-178, streamId);
			}
		}
		else 
			return exitCommand(-178, streamId);
		
		// OK, so we have a valid streamId, now lets actually select it
		if (direction == INPUT)
			return select(direction, getInputStreamNo(streamId));
		else if (direction == OUTPUT)
			return select(direction, getOutputStreamNo(streamId));
		else {
			// must be both, so we firstly try the input stream.  If
			// everything goes OK, we ca try the output stream
			select(INPUT, getInputStreamNo(streamId));
			if (RC == 0)
				return select(OUTPUT, getOutputStreamNo(streamId));
			else
				return RC;
		}
	}
	
	/**
	 * Selects a stream.  
	 * @param direction One of the constants: INPUT, OUTPUT, BOTH
	 * @param streamNo A valid stream number, or the constant: ANYINPUT
	 */
	protected final int select(int direction, int streamNo) throws PipeException {
		// a little bit of trickery with the direction that is passed.  The directions
		// that can be passed to this method for (INPUT, OUTPUT, BOTH, ANY) are:
		// (1, 2, 3, -1) respectively.  The values we have to pass to the event are
		// (-1, -2, -3, -4) respectively... 
		if (_pipe.hasListener())
			_pipe.getListener().handleEvent(new PipeEventDispatcherCall(this, direction == ANYINPUT ? -4 : (-direction), streamNo, PipeEvent.CODE_SELECT));
		
		enterCommand("SELECT");
		
		if (streamNo == ANYINPUT && direction != INPUT)
			return exitCommand(-168, ""+direction);
			
		if (!Syntax.isValidDirection(direction))
			return exitCommand(-164, ""+direction);
			
		if ((direction & INPUT) > 0) {
			// check to see if the stream exists
			if (streamNo != ANYINPUT && streamNo >= _inputStreams.size())
				return exitCommand(4);

			if ((_eofreport == ANY || _eofreport == ALL) && !hasConnectedOutputStream())
				return exitCommand(8);
			
			if (streamNo == ANYINPUT && _commitLevel < 0 && _autoCommit)
				_dispatcher.commit(this, _commitLevel, 0);

			_selectedInputStreamNo = streamNo;
		}
		if ((direction & OUTPUT) > 0) {
			// check to see if the stream exists
			if (streamNo != ANYINPUT && streamNo >= _inputStreams.size())
				return exitCommand(4);

			if (streamNo == ANYINPUT && !hasConnectedInputStream())
				return exitCommand(12);
			
			_selectedOutputStreamNo = streamNo;
		}
		
		return exitCommand(0);
	}
	
	/**
	 * Sets the return code that the producer will see when it
	 * next completes an OUTPUT command that writes to this stage
	 * @param rc The return code to be set
	 */
	protected final int setRC(int rc) throws PipeException {
		enterCommand("SETRC");
		Stream stream = (Stream)_inputStreams.get(_selectedInputStreamNo);
		if (stream.atInputEOF() && stream.getProducer().getState() != Pipe.STAGE_STATE_WAITING_OUTPUT)
			return exitCommand(-4);
		_outputRC = rc;
		_outputRCSet = true;
		return exitCommand(0);
	}
	
	/**
	 * Severs a stream
	 * @param direction One of the constants: INPUT or OUTPUT
	 */
	protected final int sever(int direction) throws PipeException {
		if (_pipe.hasListener())
			_pipe.getListener().handleEvent(new PipeEventDispatcherCall(this, -direction, 0, PipeEvent.CODE_SEVER));

		enterCommand("SEVER");

		if (!Syntax.isValidDirection(direction) && direction != BOTH)
			return exitCommand(-164, ""+direction);
			
		if (direction == INPUT) {
			Stream stream = (Stream)_inputStreams.get(_selectedInputStreamNo);
			stream.severInput();
		}
		else {
			Stream stream = (Stream)_outputStreams.get(_selectedOutputStreamNo);
			stream.severOutput();
		}
		return exitCommand(0);
	}
	
	/**
	 * Connects the currently selected input and output streams
	 */
	protected final int shortStreams() throws PipeException {
		enterCommand("SHORT");
		_dispatcher.shortStages(this, _selectedInputStreamNo, _selectedOutputStreamNo);
		return exitCommand(0);
	}
	
	/**
	 * Returns the stage's position in the pipeline
	 */
	protected final int stageNum() throws PipeException {
		if (_pipe.hasListener())
			_pipe.getListener().handleEvent(new PipeEventDispatcherCall(this, 0, 0, PipeEvent.CODE_STAGENUM));
		enterCommand("STAGENUM");
		return exitCommand(getStageNumber());
	}
	
	/**
	 * Returns the currently selected stream number for the selected direction
	 * @param direction One of the constants: INPUT or OUTPUT
	 */
	protected final int streamNum(int direction) throws PipeException {
		return streamNum(direction, -1);
	}

	/**
	 * Returns the stream number for the selected direction and stream id
	 * @param direction One of the constants: INPUT or OUTPUT
	 * @param streamId A stream id
	 */
	protected final int streamNum(int direction, String streamId) throws PipeException {
		// verify that the stream id is valid
		if (streamId != null) {
			// syntactic check first
			if (!Syntax.isStreamId(streamId))
				return exitCommand(-165, streamId);
		
			if (!Syntax.isValidDirection(direction) && direction != BOTH)
				return exitCommand(-164, ""+direction);
		}
		else 
			return exitCommand(-178, streamId);

		// OK, so we have a valid streamId, now lets actually select it
		if (direction == INPUT) {
			int streamNo = getInputStreamNo(streamId);
			if (streamNo == -1)
				return exitCommand(-178, streamId);
			else
				return streamNum(direction, streamNo);
		}
		else {
			int streamNo = getOutputStreamNo(streamId);
			if (streamNo == -1)
				return exitCommand(-178, streamId);
			else
				return streamNum(direction, streamNo);
		}
	}

	/**
	 * Returns the stream number for the selected direction and stream id
	 * @param direction One of the constants: INPUT or OUTPUT
	 * @param streamNo A stream number
	 */
	protected final int streamNum(int direction, int streamNo) throws PipeException {
		enterCommand("STREAMNUM");
		
		if (!Syntax.isValidDirection(direction) && direction != BOTH)
			return exitCommand(-164, ""+direction);

		// cater for the case where they want the currently selected stream
		if (streamNo == -1)
			streamNo = (direction == INPUT ? _selectedInputStreamNo : _selectedOutputStreamNo);
		
		if (direction == INPUT) {
			if (streamNo < _inputStreams.size())
				return exitCommand(streamNo);
			else
				return exitCommand(-4);
		}
		else {
			if (streamNo < _outputStreams.size())
				return exitCommand(streamNo);
			else
				return exitCommand(-4);
		}
	}
	
	/**
	 * Returns the state of the stream
	 * @param direction One of the constants: INPUT, OUTPUT, or SUMMARY
	 */
	protected final int streamState(int direction) throws PipeException {
		return streamState(direction, -1);
	}

	/**
	 * Returns the state of the stream
	 * @param direction One of the constants: INPUT or OUTPUT
	 * @param streamId A stream id
	 */
	protected final int streamState(int direction, String streamId) throws PipeException {
		// verify that the stream id is valid
		if (streamId != null) {
			// syntactic check first
			if (!Syntax.isStreamId(streamId))
				return exitCommand(-165, streamId);
		
			if (!Syntax.isValidDirection(direction) && direction != SUMMARY)
				return exitCommand(-164, ""+direction);
		}
		else 
			return exitCommand(-178, streamId);

		// OK, so we have a valid streamId, now lets actually select it
		if (direction == INPUT) {
			int streamNo = getInputStreamNo(streamId);
			if (streamNo == -1)
				return exitCommand(-178, streamId);
			else
				return streamState(direction, streamNo);
		}
		else {
			int streamNo = getOutputStreamNo(streamId);
			if (streamNo == -1)
				return exitCommand(-178, streamId);
			else
				return streamState(direction, streamNo);
		}
	}

	/**
	 * Returns the state of the stream
	 * @param direction One of the constants: INPUT or OUTPUT
	 * @param streamNo A stream number
	 */
	protected final int streamState(int direction, int streamNo) throws PipeException {
		if (_pipe.hasListener())
			_pipe.getListener().handleEvent(new PipeEventDispatcherCall(this, -direction, streamNo, PipeEvent.CODE_STREAMSTATE));

		enterCommand("STREAMSTATE");
		
		if (!Syntax.isValidDirection(direction))
			return exitCommand(-164, ""+direction);

		// called from the streamState(SUMMARY) method
		if (direction == SUMMARY) {
			// doesn't matter what streamNo they pass, this parameter indicates
			// that we need to return 0 if we have at least 1 connected input
			// and output streams
			if (hasConnectedInputStream() && hasConnectedOutputStream())
				return exitCommand(0);
			else
				return exitCommand(-4, false);     // because -4 is a valid rc, we return quietly
		}
		else {
			// cater for the case where they want the currently selected stream
			if (streamNo == -1)
				streamNo = (direction == INPUT ? _selectedInputStreamNo : _selectedOutputStreamNo);
			
			if (direction == INPUT) {
				if (streamNo < _inputStreams.size())
					return exitCommand(calcStreamState(INPUT, (Stream)_inputStreams.get(streamNo)));
				else
					return exitCommand(-4, false);   // because -4 is a valid rc, we return quietly
			}
			else {
				if (streamNo < _outputStreams.size())
					return exitCommand(calcStreamState(OUTPUT, (Stream)_outputStreams.get(streamNo)));
				else
					return exitCommand(-4, false);   // because -4 is a valid rc, we return quietly
			}
		}
	}

	private int calcStreamState(int direction, Stream stream) {
		if (stream.atInputEOF() || stream.atOutputEOF())
			return 12;
		else
			return 0;    // TODO return 4 or 8 depending on the state of the producer/consumer
	}
	
	/**
	 * Returns a string the represents the state of all streams. 
	 */
	protected final String streamStateAll() throws PipeException {
		enterCommand("STREAMSTATE");
		int max = Math.max(_inputStreams.size(), _outputStreams.size());
		String s = "";
		for (int i = 0; i < max; i++) {
			if (i < _inputStreams.size())
				s += calcStreamState(INPUT, (Stream)_inputStreams.get(i));
			else
				s += "-4";
			s += ":";
			if (i < _outputStreams.size())
				s += calcStreamState(OUTPUT, (Stream)_outputStreams.get(i));
			else
				s += "-4";
			s += " ";
		}
		exitCommand(0);
		return s.trim();
	}

	/**
	 * Notionally gives up CPU slice to another stage, however,
	 * given the multi-threaded nature of this implementation,
	 * it will just return immediately
	 */
	protected final int suspend() throws PipeException {
		enterCommand("SUSPEND");
		return exitCommand(0);
	}
	
	protected final Object getParameter(String parameterName) {
		return _dispatcher.getParameter(parameterName);
	}
	
	protected final void putParameter(String parameterName, Object parameter) {
		_dispatcher.putParameter(parameterName, parameter);
	}
	
	public String toString() {
		return getName()+"("+getStageNumber()+")";
	}
	
	public void debugDump() {
		int maxStreams = Math.max(_inputStreams.size(), _outputStreams.size());
		for (int i = 0; i < maxStreams; i++) {
			Stream producerStream = (_inputStreams.size() >= i+1 ? (Stream)_inputStreams.get(i) : null);
			Stream consumerStream = (_outputStreams.size() >= i+1 ? (Stream)_outputStreams.get(i) : null);
			String producer = (producerStream == null ? "(not defined)" : (producerStream.getProducer() == null ? "EOF" : ""+producerStream.getProducer()));
			String consumer = (consumerStream == null ? "(not defined)" : (consumerStream.getConsumer() == null ? "EOF" : ""+consumerStream.getConsumer()));
			String producerRefId = PipeUtil.align((producerStream == null ? "?" : ""+producerStream.getRefId()), 2, LEFT);
			String consumerRefId = PipeUtil.align((consumerStream == null ? "?" : ""+consumerStream.getRefId()), 2, LEFT);
			System.out.println(PipeUtil.align(producer, 20, RIGHT)+"<="+producerRefId+"=>"+PipeUtil.align(toString(), 20, CENTRE)+"<="+consumerRefId+"=>"+PipeUtil.align(consumer, 20, LEFT)+" "+getPrintableState());
		}
	}
}
