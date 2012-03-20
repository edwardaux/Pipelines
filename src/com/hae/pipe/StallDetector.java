package com.hae.pipe;

import java.util.*;

public class StallDetector implements PipeConstants {
	public static final String COPYRIGHT = "Copyright 2007,2012. H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	public boolean isStalled(Pipeline pipeline) {
		// for now, just say we're not stalled
		boolean notWorkingYet = true;
		if (notWorkingYet)
			return false;
		else {
			ArrayList<Stage> stages = pipeline.getStages();
			for (int i = 0; i < stages.size(); i++) {
				Stage stage = (Stage)stages.get(i);
				if (isStalled(stage))
					return true;
			}
			return false;
		}
	}
	
	private boolean isStalled(Stage stage) {
		HashMap<Integer, Stage> alreadyFound = new HashMap<Integer, Stage>();
		while (stage != null) {
			if (alreadyFound.get(new Integer(stage.getStageNumber())) != null)
				return true;
			
			alreadyFound.put(new Integer(stage.getStageNumber()), stage);
			if (stage.getState() == Pipe.STAGE_STATE_WAITING_PEEKTO || stage.getState() == Pipe.STAGE_STATE_WAITING_READTO) {
				if (stage.getSelectedInputStreamNo() != ANYINPUT) {
					Stream stream = stage.getInputStream(stage.getSelectedInputStreamNo());
					System.out.println("Waiting for READTO/PEEKTO: "+stage+" producer="+stream.getProducer());
					stage = stream.getProducer();
				}
			}
			else if (stage.getState() == Pipe.STAGE_STATE_WAITING_OUTPUT) {
				Stream stream = stage.getOutputStream(stage.getSelectedOutputStreamNo());
				System.out.println("Waiting for OUTPUT: "+stage+" consumer="+stream.getConsumer());
				stage = stream.getConsumer();
			}
			else
				return false;
		}
		return false;
	}
}
