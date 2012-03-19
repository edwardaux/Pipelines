package com.hae.pipe;

public class Options {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public static final int MSGLEVEL_UNDEFINED1  = 0x8000;
	public static final int MSGLEVEL_UNDEFINED2  = 0x4000;
	public static final int MSGLEVEL_TIMESPENT   = 0x2000;
	public static final int MSGLEVEL_CHECKENTRY  = 0x1000;
	public static final int MSGLEVEL_STOREALLOC  = 0x0800;
	public static final int MSGLEVEL_MOREDEBUG   = 0x0400;
	public static final int MSGLEVEL_STOREMANAGE = 0x0200;
	public static final int MSGLEVEL_STACKMSGS   = 0x0100;
	public static final int MSGLEVEL_RESERVED8   = 0x0080;
	public static final int MSGLEVEL_RESERVED4   = 0x0040;
	public static final int MSGLEVEL_RESERVED2   = 0x0020;
	public static final int MSGLEVEL_RESERVED1   = 0x0010;
	public static final int MSGLEVEL_UNDEFINED3  = 0x0008;
	public static final int MSGLEVEL_ISSUEDFROM  = 0x0004;
	public static final int MSGLEVEL_PROCESSCMD  = 0x0002;
	public static final int MSGLEVEL_RUNNING     = 0x0001;

	public char endChar = '\u0000';
	public char escChar = '\u0000';
	public boolean listCmd = false;
	public boolean listErr = false;
	public boolean listRC = false;
	public int msgLevel = 0x0007;
	public String name = null;
	public char stageSep = '|';
	public boolean trace = false;

	public boolean endCharSet = false;
	
	/**
	 * Default constructor
	 */
	public Options() {
	}
	
	/**
	 * Copy constructor
	 */
	public Options(Options options) {
		endChar = options.endChar;
		escChar = options.escChar;
		listCmd = options.listCmd;
		listErr = options.listErr;
		listRC = options.listRC;
		msgLevel = options.msgLevel;
		name = options.name;
		stageSep = options.stageSep;
		trace = options.trace;
	}
}
