// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.tagsuggestion;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;

/**
 * Class representing a tag which is composed of the word itself and its
 * frequency. Frequency of a tag is determined by the number of times it appears
 * in the reviews collected from sources.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class Tag {
	String mTag;
	int mFrequency;
	Map<String, Integer> mCoOcurring;
	
	/**
	 * Constructor
	 */
	public Tag(String word) {
		this(word, 1);
	}
	
	/**
	 * Constructor
	 * 
	 * @param word the tag
	 * @param frequency frequency of the tag
	 */
	public Tag(String word, int frequency) {
		this.mTag = word;
		mFrequency = frequency;
		mCoOcurring = new HashMap<String, Integer>();
	}
	
	/**
	 * Constructor
	 * 
	 * @param word the tag
	 * @param frequency frequency of the tag
	 * @param coOccuring the string representing the list of co-occuring tags.
	 */
	public Tag(String word, int frequency, String coOccuring) {
		this(word, frequency);
		StringTokenizer tokenizer = new StringTokenizer(coOccuring, ",");
		while (tokenizer.hasMoreTokens()) {
			mCoOcurring.put(tokenizer.nextToken(), Integer.parseInt(tokenizer.nextToken()));
		}
	}
	
	public void setFrequency(int frequency) {
		mFrequency = frequency;
	}
	
	public void setCoOccuring(Map<String, Integer> map) {
		mCoOcurring = map;
	}
	
	public String getTag() {
		return mTag;
	}
	
	public int getFrequency() {
		return mFrequency;
	}
	
	/**
	 * Returns the map of strings and frequency for all co-occuring tags.
	 * 
	 * @return
	 */
	public Map<String, Integer> getCoOccuring() {
		return mCoOcurring;
	}
	
	/**
	 * Returns a string representing co-occuring tags of this tag.
	 */
	public String getCoOccuringString() {
		StringBuilder builder = new StringBuilder();
		Iterator<Entry<String, Integer>> iter = mCoOcurring.entrySet().iterator();
		Entry<String, Integer> e = null;
		while (iter.hasNext()) {
			e = iter.next();
			builder.append(e.getKey()).append(",").append(e.getValue()).append(",");
		}
		
		return builder.toString();
	}
	
	/**
	 * Returns true if {@code word} is in the top-occuring list of this tag.
	 * 
	 * @param word
	 * @return
	 */
	public boolean hasCoOccuring(String word) {
		if (mCoOcurring.get(word) == null) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Adds {@code word} to the list of this tag's co-occuring tags.
	 * 
	 * @param word
	 */
	public void addCoOccuring(String word) {
		Integer value = mCoOcurring.get(word);
		if (value == null) {
			value = 1;
		} else {
			value += 1;
			mCoOcurring.remove(word);
		}
		mCoOcurring.put(word, value);
	}
	
	@Override
	public String toString() {
		return "[" + mTag + ", " + mFrequency + "]";
	}
}
