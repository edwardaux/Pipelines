package com.hae.pipe;

import java.util.*;

public class RangeMultiple implements Range {
	public static final String COPYRIGHT = "Copyright 2007, H.A. Enterprises Pty Ltd. All Rights Reserved";
	
	private ArrayList _ranges = new ArrayList();
	
	public RangeMultiple(PipeArgs args) throws PipeException {
		String rangesString = args.nextExpression();
		if (rangesString.equals(""))
			throw new PipeException(-55);
		PipeArgs tmpArgs = new PipeArgs(rangesString);
		while (!tmpArgs.getRemainder().trim().equals("")) {
			_ranges.add(new RangeSingle(tmpArgs));
		}
	}
	
	/**
	 * Returns whether the passed string is contained within the range
	 */
	public boolean contains(String input, String target, boolean anycase, boolean anyof) throws PipeException {
	    for (int i = 0; i < _ranges.size(); i++) {
	    	if (((Range)_ranges.get(i)).contains(input, target, anycase, anyof))
	    		return true;
	    }
	    return false;
	}

	public String extractRange(String input) throws PipeException {
	    String s = "";
	    for (int i = 0; i < _ranges.size(); i++)
	    	s += ((Range)_ranges.get(i)).extractRange(input);
	    return s;
	}
	
	public ReplaceResult replace(String input, String from, String to, int numberOfOccurrences, boolean anycase) throws PipeException {
	    if (numberOfOccurrences < 0)
	    	numberOfOccurrences = Integer.MAX_VALUE;
	    
	    ReplaceResult result = new ReplaceResult(input, 0);
	    for (int i = 0; i < _ranges.size(); i++) {
	    	if (result.numberOfChanges >= numberOfOccurrences)
	    		break;
	    	
	    	Range range = (Range)_ranges.get(i);
	    	ReplaceResult tmp = range.replace(result.changed, from, to, numberOfOccurrences - result.numberOfChanges, anycase);
	    	result.changed = tmp.changed;
	    	result.numberOfChanges += tmp.numberOfChanges;
	    }
	    return result;
	}
	
	public String toString() {
		String s = "(";
		for (int i = 0; i < _ranges.size(); i++)
			s += (i == 0 ? "" : " ")+_ranges.get(i);
		return s+")";
	}
}
