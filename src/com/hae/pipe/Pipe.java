package com.hae.pipe;

import java.text.*;
import java.util.*;

public class Pipe {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public static final int    VERSION_LEVEL   =  0;
	public static final int    VERSION_RELEASE = 50;
	public static final int    VERSION_MOD     =  0;
	public static final int    VERSION_SERIAL  =  0;
	public static final String VERSION       = "v"+VERSION_LEVEL+"."+VERSION_RELEASE+"."+VERSION_MOD;
	public static final String VERSION_DATE  = "Mon 7th July 2007 10:45:23";
	
	public static final int PIPE_TYPE_PIPE     = 0;
	public static final int PIPE_TYPE_ADDPIPE  = 1;
	public static final int PIPE_TYPE_CALLPIPE = 2;
	
	public static final int STREAM_DIRECTION_INPUT  = 0;
	public static final int STREAM_DIRECTION_OUTPUT = 1;

	public static final int COMMIT_MAX =  2147483647;
	public static final int COMMIT_MIN = -2147483647;
	
	public static final int STAGE_STATE_COMMITTED          = 1;
	public static final int STAGE_STATE_NOT_STARTED        = 2;
	public static final int STAGE_STATE_RUNNING            = 3;
	public static final int STAGE_STATE_WAITING_PEEKTO     = 4;
	public static final int STAGE_STATE_WAITING_READTO     = 5;
	public static final int STAGE_STATE_WAITING_OUTPUT     = 6;
	public static final int STAGE_STATE_WAITING_SUBROUTINE = 7;
	public static final int STAGE_STATE_WAITING_COMMIT     = 8;
	public static final int STAGE_STATE_WAITING_EXTERNAL   = 9;
	public static final int STAGE_STATE_READY              = 10;
	public static final int STAGE_STATE_TERMINATED         = 11;
	public static final int STAGE_STATE_NON_DISPATCHABLE   = 12;

	public static final String MODULE_SCANNER = "PIPSCA";
	public static final String MODULE_STAGE   = "PIPSTG";
	public static final String MODULE_TRACE   = "PIPTRC";

	/**
	 * A listener for dispatcher events
	 */
	private PipeListener _listener;
	
	private HashMap<String, Object> _parameters = new HashMap<String, Object>();
	
	public Pipe() {
	}
	
	public Pipe(HashMap<String, Object> parameters) {
		_parameters = parameters;
	}
	
	public Pipe addParam(String name, Object value) {
		_parameters.put(name, value);
		return this;
	}
	
	public HashMap<String, Object> getParameters() {
		return _parameters;
	}
	
	public int run(String pipelineSpec) {
		return run(pipelineSpec, null);
	}
	
	public int run(String pipelineSpec, PipeListener listener) {
		Scanner scanner = null;
		try {
			_listener = listener;
			scanner = new Scanner(this, PIPE_TYPE_PIPE, pipelineSpec);
			Dispatcher dispatcher = new Dispatcher(this, _parameters);
			dispatcher.addPipeline(scanner.getPipeline());
			return dispatcher.execute();
		}
		catch(PipeException e) {
			issueMessage(e.getMessageNo(), MODULE_SCANNER, e.getParms());
			return e.getMessageNo();
		}
	}
	
	public boolean hasListener() {
		return _listener != null;
	}
	public PipeListener getListener() {
		return _listener;
	}
	public void issueMessage(String message) {
		outputMessage(null, 0, null, message);
	}
	public void issueMessage(int messageNo, String moduleId, String[] parms) {
		outputMessage(null, messageNo, parms, buildMessage(messageNo, moduleId, parms));
	}
	public void issueMessage(int messageNo, String moduleId, String[] parms, Stage stage) {
		outputMessage(stage, messageNo, parms, buildMessage(messageNo, moduleId, parms));
		if ((stage.getOptions().msgLevel & Options.MSGLEVEL_ISSUEDFROM) > 0) {
			if (stage.getOwningPipeline().getName() != null) {
				String[] tmpParms = new String[] { ""+stage.getStageNumber(), ""+stage.getOwningPipeline().getPipelineNumber(), stage.getOwningPipeline().getName() };
				outputMessage(stage, 4, tmpParms, buildMessage(4, MODULE_SCANNER, tmpParms));
			}
			else {
				String[] tmpParms = new String[] { ""+stage.getStageNumber(), ""+stage.getOwningPipeline().getPipelineNumber() };
				outputMessage(stage, 3, tmpParms, buildMessage(3, MODULE_SCANNER, tmpParms));
			}
		}
		if ((stage.getOptions().msgLevel & Options.MSGLEVEL_RUNNING) > 0) {
			String[] tmpParms = new String[] { stage.getNameAndArgs() };
			outputMessage(stage, 1, tmpParms, buildMessage(1, MODULE_SCANNER, tmpParms));
			if (messageNo == 35) {
				tmpParms = new String[] { parms[1] };
				outputMessage(stage, 39, tmpParms, buildMessage(39, MODULE_TRACE, tmpParms));
			}
		}
	}
	public static String buildMessage(int messageNo, String moduleId, String[] parms) {
		String msg;
		messageNo = Math.abs(messageNo);
		try {
			msg = PropertyResourceBundle.getBundle("pipmsg").getString("msg."+messageNo);
		}
		catch(MissingResourceException e) {
			try {
				msg = ResourceBundle.getBundle("pipmsg").getString("msg.0");
				String parmString = "";
				for (int i = 0; i < parms.length; i++)
					parmString += parms[i]+" ";
				parms = new String[] { ""+messageNo, parmString };
			}
			catch(MissingResourceException e2) {
				msg = "Unable to find message text for message "+messageNo;
			}
		}
		MessageFormat format = new MessageFormat(msg);
		return moduleId+formatNo(messageNo)+format.format(parms);
	}
	private static String formatNo(int messageNo) {
		if (messageNo > 999)
			return ""+messageNo;
		else {
			String s = "00"+messageNo;
			return s.substring(s.length()-3);
		}
	}
	private void outputMessage(Stage stage, int messageNo, String[] parms, String message) {
		if (hasListener())
			getListener().handleEvent(new PipeEventMessage(stage, messageNo, parms, message));
		System.out.println(message);
	}

	public static void register(String stageName, Class<?> stageClass) {
		Scanner.registerStage(stageName, stageClass);
	}
	
	public static void main(String[] args) {
		new Pipe().run(args == null || args.length == 0 ? "" : args[0]);
	}
}
