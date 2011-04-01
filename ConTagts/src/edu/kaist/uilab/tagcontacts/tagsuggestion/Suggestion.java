// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.tagsuggestion;

/**
 * A tag suggestion for returning to user.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class Suggestion implements Comparable<Suggestion> {
	private String mTag;
	private float mScore;
	
	public Suggestion(String tag, float score) {
		mTag = tag;
		mScore = score;
	}

	public String getTag() {
		return mTag;
	}
	
	@Override
	public int compareTo(Suggestion that) {
		if (that.mScore > mScore) {
			return 1;
		} else if (that.mScore == mScore) {
			return 0;
		} else {
			return -1;
		}
	}
	
	@Override
	public String toString() {
		return String.format("\n%s, %.3f", mTag, mScore);
	}
}
