// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.tagsuggestion;

/**
 * A tag that is frequently used.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class FrequentTag extends Tag {
	int mTimes;
	
	public FrequentTag(String tag, int times) {
		super(tag);
		mTimes = times;
	}
	
	public int getTimes() {
		return mTimes;
	}
	
	public void setTimes(int times) {
		mTimes = times;
	}
}
