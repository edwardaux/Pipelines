package com.hae.pipe;

import java.util.*;

import com.hae.pipe.stages.*;

public class Scanner implements PipeConstants {
	/**
	 * The owning Pipe that we are scanning for
	 */
	private Pipe _pipe;

	/**
	 * Contains the global list of lookups for internal stages. Key=String (eg.
	 * literal) Value=Class (eg. com.hae.pipe.stages.Literal.class)
	 */
	private static HashMap<String, Class<?>> _stageLookup = new HashMap<String, Class<?>>();

	/**
	 * The pipeline that stages will be placed into
	 */
	private Pipeline _pipeline;

	/**
	 * Internal working variable that keeps track of the previous stage so we can
	 * link the current to the prev
	 */
	private Stage _prevStage = null;

	/**
	 * A lookup table of all stages that were prefixed with a label (so, used when
	 * the labels are referenced)
	 */
	private HashMap<String, Stage> _labelledStages = new HashMap<String, Stage>();

	/**
	 * In the cases where this spec is being parsed for ADDPIPE and CALLPIPE, this
	 * is set to the stage that is invoking it
	 */
	private Stage _owningStage;

	/**
	 * When a connector is the first stage in a pipe, we need to hang on to it.
	 */
	private Connector _connector = null;

	public Scanner(Pipe pipe, int type, String pipelineSpec) throws PipeException {
		this(pipe, null, type, pipelineSpec);
	}

	public Scanner(Pipe pipe, Stage owningStage, int type, String pipelineSpec) throws PipeException {
		_pipe = pipe;
		_owningStage = owningStage;
		_pipeline = new Pipeline(type, pipelineSpec);
		if (_pipe.hasListener()) {
			_pipe.getListener().handleEvent(new PipeEventScannerEnter(_pipeline));
			_pipe.getListener().handleEvent(new PipeEventPipelineAllocated(_pipeline));
		}

		int rc = 0;
		try {
			parse(pipelineSpec);
		}
		catch(PipeException e) {
			// catch any exceptions so we can get the error no (but rethrow)
			rc = e.getMessageNo();
			throw e;
		}
		finally {
			if (_pipe.hasListener())
				_pipe.getListener().handleEvent(new PipeEventScannerLeave(_pipeline, rc));
		}
	}

	public Pipeline getPipeline() {
		return _pipeline;
	}

	private void add(String label, Stage stage) throws PipeException {
		if (label != null) {
			stage.setLabel(label);
			_labelledStages.put(label, stage);
		}

		if (_pipe.hasListener() && _prevStage == null)
			_pipe.getListener().handleEvent(new PipeEventScannerItem(_pipeline));
		if (_pipe.hasListener())
			_pipe.getListener().handleEvent(new PipeEventScannerItem(_pipeline, stage));

		// add it to our list of stages for this pipe
		_pipeline.addStage(stage);
		stage.setOwningPipeline(_pipeline);
		stage.setStageNumber(_pipeline.getStages().size());

		// if are the second, or subsequent, stage, then we
		// may need to associate ourself with a previous connector
		if (_connector != null) {
			_connector.setNewStage(stage);
			_connector = null;
		}

		// and connect it to the previous and next stage (which
		// at this point, should always be null)
		int prevStageStreamNo = (_prevStage == null ? 0 : _prevStage.maxStream(OUTPUT));
		String prevStageStreamId = getStreamId(_prevStage, prevStageStreamNo, OUTPUT);
		int stageInputStreamNo = stage.maxStream(INPUT);
		String stageInputStreamId = getStreamId(stage, stageInputStreamNo, INPUT);
		int stageOutputStreamNo = stage.maxStream(OUTPUT);
		String stageOutputStreamId = getStreamId(stage, stageOutputStreamNo, OUTPUT);
		Dispatcher.connectStages(_prevStage, prevStageStreamNo, prevStageStreamId, stage, stageInputStreamNo, stageInputStreamId);
		Dispatcher.connectStages(stage, stageOutputStreamNo, stageOutputStreamId, null, 0, "");

		_prevStage = stage;
	}

	private void add(String label, boolean firstStage, boolean lastStage) throws PipeException {
		Stage stage = (Stage) _labelledStages.get(label);
		if (stage == null)
			throw new PipeException(-46, label);

		if (_pipe.hasListener())
			_pipe.getListener().handleEvent(new PipeEventScannerItem(_pipeline, stage, label, ""));  // TODO streamId

		stage.addStream(BOTH);
		if (!lastStage) {
			int stageOutputStreamNo = stage.maxStream(OUTPUT);
			String stageOutputStreamId = getStreamId(stage, stageOutputStreamNo, OUTPUT);
			Dispatcher.connectStages(stage, stageOutputStreamNo, stageOutputStreamId, null, 0, "");
		}
		if (!firstStage) {
			int prevStageStreamNo = (_prevStage == null ? 0 : _prevStage.maxStream(OUTPUT));
			String prevStageStreamId = getStreamId(_prevStage, prevStageStreamNo, OUTPUT);
			int stageInputStreamNo = stage.maxStream(INPUT);
			String stageInputStreamId = getStreamId(stage, stageInputStreamNo, INPUT);
			Dispatcher.connectStages(_prevStage, prevStageStreamNo, prevStageStreamId, stage, stageInputStreamNo, stageInputStreamId);
		}
		_prevStage = stage;
	}

	private void end() {
		_prevStage = null;
	}

	private void parse(String pipelineSpec) throws PipeException {
		// we could probably do this more efficiently, but let's
		// start simply, shall we?
		Options options = _pipeline.getOptions();

		// check that we actually have some data
		if (pipelineSpec.trim().length() == 0)
			throw new PipeException(256);

		if (pipelineSpec.trim().charAt(0) == '(') {
			// TODO theoretically, they could open the PIPE options, not close them,
			// and then open a set of stage options
			int closeParenIndex = pipelineSpec.indexOf(')');
			if (closeParenIndex == -1)
				throw new PipeException(636, "6", "Missing end parenthesis");
			parseOptions(pipelineSpec.substring(1, closeParenIndex), options);
			pipelineSpec = pipelineSpec.substring(closeParenIndex + 1);
		}

		// check again now that we have handled the options
		if (pipelineSpec.trim().length() == 0)
			throw new PipeException(256);

		while (true) {
			// TODO esc chars
			int endIndex = (options.endCharSet ? pipelineSpec.indexOf(options.endChar) : -1);
			int stageSepIndex = pipelineSpec.indexOf(options.stageSep);

			// neither being found must mean this is the last stage
			if (endIndex == -1 && stageSepIndex == -1) {
				boolean firstStage = (_prevStage == null);
				parseStage(pipelineSpec, options, firstStage, true);
				break;
			}

			// now we have to handle 3 cases:
			// - both are found
			// - end char is found
			// - stagesep is found
			boolean end = false;
			int nextIndex = 0;
			if (endIndex != -1 && stageSepIndex != -1) {
				// both were found
				if (endIndex < stageSepIndex) {
					end = true;
					nextIndex = endIndex;
				} else
					nextIndex = stageSepIndex;
			} else if (endIndex != -1) {
				// end char is found
				end = true;
				nextIndex = endIndex;
			} else {
				// stagesep is found
				nextIndex = stageSepIndex;
			}

			boolean firstStage = (_prevStage == null);
			String stageSpec = pipelineSpec.substring(0, nextIndex);
			parseStage(stageSpec, options, firstStage, end);
			if (end)
				end();

			pipelineSpec = pipelineSpec.substring(nextIndex + 1);
		}

	}

	private void parseOptions(String optionSpec, Options options) throws PipeException {
		String[] words = PipeUtil.split(optionSpec);
		for (int i = 0; i < words.length; i++) {
			String word = words[i].toUpperCase();
			boolean noMode = false;

			if (word.startsWith("NO")) {
				noMode = true;

				// if the options are (NO LISTCMD), we consume the NO
				// and move to the real keyword, otherwise (NOLISTCMD), we
				// just set the word to everything after the NO

				if (word.equals("NO"))
					word = words[++i];
				else
					word = word.substring(2);
			}

			if (Syntax.abbrev("ENDCHAR", word, 3)) {
				options.endChar = PipeUtil.makeChar(words[++i]);
				options.endCharSet = true;
			}
			else if (Syntax.abbrev("ESCAPE", word, 3))
				options.escChar = PipeUtil.makeChar(words[++i]);
			else if (Syntax.abbrev("LISTCMD", word, 7))
				options.listCmd = noMode ? false : true;
			else if (Syntax.abbrev("LISTERR", word, 7))
				options.listErr = noMode ? false : true;
			else if (Syntax.abbrev("LISTRC", word, 6))
				options.listRC = noMode ? false : true;
			else if (Syntax.abbrev("MSGLEVEL", word, 3)) {
				if (noMode)
					options.msgLevel &= ~PipeUtil.makeInt(words[++i]); // NAND
				else
					options.msgLevel = PipeUtil.makeInt(words[++i]);
			} else if (Syntax.abbrev("NAME", word, 4))
				options.name = words[++i];
			else if (Syntax.abbrev("STAGESEP", word, 8) || Syntax.abbrev("SEP", word, 3))
				options.stageSep = PipeUtil.makeChar(words[++i]);
			else if (Syntax.abbrev("TRACE", word, 5))
				options.trace = noMode ? false : true;
		}
	}

	private void parseStage(String stageSpec, Options globalOptions, boolean firstStage, boolean lastStage) throws PipeException {
		Options localOptions = new Options(globalOptions);

		String trimmedStageSpec = stageSpec.trim();
		if (trimmedStageSpec.length() == 0)
			throw new PipeException(17);

		if (trimmedStageSpec.trim().charAt(0) == '(') {
			// TODO theoretically, they could open the PIPE options, not close them,
			// and then open a set of stage options
			int closeParenIndex = trimmedStageSpec.indexOf(')');
			if (closeParenIndex == -1)
				throw new PipeException(636, "6", "Missing end parenthesis");
			parseOptions(trimmedStageSpec.substring(1, closeParenIndex), localOptions);
			trimmedStageSpec = trimmedStageSpec.substring(closeParenIndex + 1).trim();
		}

		// use the split function to get out the first word (ie. stage name or
		// label)
		String[] words = PipeUtil.split(trimmedStageSpec);
		String stageName = words[0];

		if (stageName.startsWith("*")) {
			if (words.length == 1) {
				if (stageName.endsWith(":")) {
					Connector connector = new Connector(stageName, firstStage, lastStage);
					if (_pipe.hasListener())
						_pipe.getListener().handleEvent(new PipeEventScannerItem(_pipeline, connector.getDirection(), connector.getConnectorStreamNo(), stageName)); // TODO streamId
					_pipeline.getConnectors().add(connector);
					if (!firstStage && !lastStage)
						throw new PipeException(-99); // must be first or last stage
					if (firstStage && lastStage)
						throw new PipeException(-195); // can't contain only a connector

					// this is not the first stage, so we
					// set the producer to whatever was previous
					if (_prevStage != null) {
						connector.setNewStage(_prevStage);
						connector.setNewStageStreamNo(_prevStage.maxStream(OUTPUT));
					} else
						_connector = connector;
				} else
					throw new PipeException(-193); // doesn't end with a :
			} else
				throw new PipeException(-98); // has to be by itself
		} else if (words.length == 1 && Syntax.isLabel(stageName)) {
			// add a label only-stage
			add(stageName, firstStage, lastStage);
		} else {
			String label = null;
			if (Syntax.isLabel(stageName)) {
				label = stageName;
				stageName = words[1];
			}
			Stage stage = lookupStage(stageName);
			stage.setPipe(_pipe);

			// now, the parameters to the stage are everything after
			// the first space after the first word. But we need to be
			// careful in case the only thing passed was the stage name
			String args = "";
			if (stageSpec.length() > (stageSpec.indexOf(stageName) + stageName.length() + 1))
				args = stageSpec.substring(stageSpec.indexOf(stageName) + stageName.length() + 1);

			stage.setArgs(args);
			stage.setOptions(localOptions);
			add(label, stage);
		}
	}

	private String getStreamId(Stage stage, int streamNo, int direction) {
		if (stage == null)
			return "";
		Stream stream = (direction == INPUT ? stage.getInputStream(streamNo) : stage.getOutputStream(streamNo));
		if (stream == null)
			return "";
		return (direction == INPUT ? stream.getConsumerStreamId() : stream.getProducerStreamId());
	}

	static void registerStage(String name, Class<?> stageClass) {
		registerStage(name, stageClass, name.length());
	}

	static void registerStage(String name, Class<?> stageClass, int minLength) {
		name = name.toLowerCase();
		for (int i = minLength; i < name.length()+1; i++) {
			_stageLookup.put(name.substring(0, i), stageClass);
		}
	}

	static Stage lookupStage(String name) throws PipeException {
		Class<?> stageClass = _stageLookup.get(name.toLowerCase());
		if (stageClass == null) {
			// it isn't a built-in stage or a pre-registered stage
			// let's look to see if we can find it in the classpath
			try {
				stageClass = Class.forName(name);
			} catch (Throwable t) {
				throw new PipeException(27, name);
			}
		}
		try {
			return (Stage) stageClass.newInstance();
		} catch (Throwable t) {
			t.printStackTrace();
			throw new PipeException(27, name, t.getMessage());
		}
	}

	static String reverseLookupStage(Class<?> stageClass) {
		Iterator<?> i = _stageLookup.keySet().iterator();
		while (i.hasNext()) {
			String name = (String) i.next();
			Class<?> sc = _stageLookup.get(name.toLowerCase());
			if (stageClass.equals(sc))
				return name;
		}
		return "<unknown>";
	}

	public class Connector {
		/**
		 * If the connector was at the start of a pipe, then this will contain the
		 * previous stage. If the connector was at the end, it will contain the next
		 * stage.
		 */
		private Stage _newStage;

		/**
		 * The stream number of the consumer/producer that we are supposed to
		 * connect to
		 */
		private int _newStageStreamNo;

		/**
		 * The stream id of the consumer/producer that we are supposed to connect to
		 */
		private String _newStageStreamId;

		/**
		 * What type of connector is it? INPUT or OUTPUT
		 */
		private int _direction;

		/**
		 * What stream no of the owning stage is it connecting to
		 */
		private int _connectorStreamNo;

		/**
		 * Takes a string that is known to start with a '*', and end with a ':'
		 */
		public Connector(String string, boolean firstStage, boolean lastStage) throws PipeException {
			// make sure that we are either a ADDPIPE or CALLPIPE invocation
			if (_owningStage == null || (_pipeline.getType() != Pipe.PIPE_TYPE_ADDPIPE && _pipeline.getType() != Pipe.PIPE_TYPE_CALLPIPE))
				throw new PipeException(-101, string);

			// can only have a connecter as first or last stage
			if (!(firstStage || lastStage))
				throw new PipeException(-99);

			// second char must be a period (or a colon)
			if (string.charAt(1) != '.' && string.charAt(1) != ':')
				throw new PipeException(-191);

			// setup defaults...
			_direction = (firstStage ? INPUT : OUTPUT);
			_connectorStreamNo = (_direction == INPUT ? _owningStage.getSelectedInputStreamNo() : _owningStage.getSelectedOutputStreamNo());

			// there are three types of connector:
			// *:
			// *.dir:
			// *.dir.stream: where dir can be blank
			StringTokenizer st = new StringTokenizer(string, ".:");
			String token = st.nextToken();
			// we have at least one token, but from here on in,
			// we may not get any more, so have to make sure we
			// catch any NoSuchElementException
			try {
				if (token.equals("*")) {
					token = st.nextToken();
				}

				if (Syntax.abbrev("INPUT", token, 2) || Syntax.abbrev("OUTPUT", token, 3)) {
					_direction = Syntax.asDirection(token);
					token = st.nextToken();
				}

				if (Syntax.isStream(token) || token.equals("*")) {
					_connectorStreamNo = Syntax.extractStreamNo(token, _direction, _owningStage);
				} else
					throw new PipeException(-165, string);
			} catch (NoSuchElementException e) {
				// no more input
			}
		}

		public int getDirection() {
			return _direction;
		}

		public int getConnectorStreamNo() {
			return _connectorStreamNo;
		}

		public Stage getNewStage() {
			return _newStage;
		}

		public void setNewStage(Stage newStage) {
			_newStage = newStage;
		}

		public int getNewStageStreamNo() {
			return _newStageStreamNo;
		}

		public void setNewStageStreamNo(int newStageStreamNo) {
			_newStageStreamNo = newStageStreamNo;
		}

		public String getNewStageStreamId() {
			return _newStageStreamId;
		}

		public void setNewStageStreamId(String newStageStreamId) {
			_newStageStreamId = newStageStreamId;
		}

	}

	static {
		registerStage("<", Diskr.class);
		registerStage(">", Diskw.class);
		registerStage(">>", Diskwa.class);
		registerStage("abbrev", Abbrev.class);
		registerStage("addrdw", Addrdw.class);
		registerStage("aggrc", Aggrc.class);
		registerStage("beat", Beat.class);
		registerStage("between", Between.class);
		registerStage("buffer", Buffer.class);
		registerStage("change", Change.class);
		registerStage("chop", Chop.class);
		registerStage("cmd", Command.class);
		registerStage("command", Command.class);
		registerStage("console", Console.class, 4);
		registerStage("count", Count.class);
		registerStage("diskr", Diskr.class);
		registerStage("diskw", Diskw.class);
		registerStage("duplicate", Duplicate.class, 3);
		registerStage("fanin", Fanin.class);
		registerStage("faninany", Faninany.class);
		registerStage("fanout", Fanout.class);
		registerStage("gate", Gate.class);
		registerStage("hole", Hole.class);
		registerStage("hostid", Hostid.class);
		registerStage("hostname", Hostname.class);
		registerStage("literal", Literal.class);
		registerStage("locate", Locate.class);
		registerStage("nlocate", NLocate.class);
		registerStage("noeofback", NoEofBack.class);
		registerStage("noteofback", NoEofBack.class);
		registerStage("not", Not.class);
		registerStage("notlocate", NLocate.class);
		registerStage("query", Query.class, 1);
		registerStage("reverse", Reverse.class);
		registerStage("specs", Specs.class, 4);
		registerStage("split", Split.class);
		registerStage("stem", Stem.class);
		registerStage("strliteral", StrLiteral.class);
		registerStage("take", Take.class);
		registerStage("terminal", Console.class, 4);
		registerStage("truncate", Change.class, 5);
		registerStage("zzzgen", ZZZGenChars.class);
		registerStage("zzzcheck", ZZZCheckChars.class);
	}
}
