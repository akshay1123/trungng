// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.tagsuggestion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Class for providing tag suggestions.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class SuggestionsProvider {

	public static final int MAX_SUGGESTIONS = 15;
	
	private ArrayList<FrequentTag> mFrequentTags;
	
	/**
	 * Constructor
	 * 
	 * @param frequentTags the list of frequent tags sorted by decreasing order of frequency
	 */
	public SuggestionsProvider(ArrayList<FrequentTag> frequentTags) {
		mFrequentTags = frequentTags;
	}
	
	/**
	 * Computes p(c|u).
	 * 
	 * <p> p(u, c) must always be known since u is in vocab. If u is not in vocab, we would not
	 * run into this method.
	 */
	private float p(String c, Tag u) {
		// p(c|u) = p(c, u) / p(u) = p(u, c) / p(u)
		if (u.mCoOcurring.get(c) == null) {
			return 0;
		} else {
			return (float) u.mCoOcurring.get(c).intValue() / u.mFrequency;
		}	
	}
	
	/**
	 * Gets the suggestions sorted by score for the list of tags created by users.
	 *  
	 * @param userTags
	 * @return
	 */
	public List<String> getSuggestions(List<Tag> userTags) {
		if (userTags.size() == 0) {
			Suggestion[] suggestion = new Suggestion[mFrequentTags.size()];
			for (int i = 0; i < suggestion.length; i++) {
				FrequentTag tag = mFrequentTags.get(i);
				suggestion[i] = new Suggestion(tag.mTag, tag.mTimes);
			}
			return getSuggestionStrings(userTags, suggestion);
		}
		
		HashSet<String> candidates = new HashSet<String>();
		// get C(u) for all u in tags
		for (Tag tag : userTags) {
			candidates.addAll(tag.mCoOcurring.keySet());
		}
		// get C(p) for this user
		for (FrequentTag p : mFrequentTags) {
			candidates.add(p.mTag);
		}
		Suggestion[] suggestion = new Suggestion[candidates.size()];
		int iSuggestion = 0;
		Iterator<String> iter = candidates.iterator();
		while (iter.hasNext()) {
			String candidate = iter.next();
			suggestion[iSuggestion++] = new Suggestion(candidate, getScore(userTags, candidate));
		}
		Arrays.sort(suggestion);
		return getSuggestionStrings(userTags, suggestion);
	}
	
	/**
	 * Returns true if {@code tag} is in the list {@code userTags}.
	 * 
	 * @param userTags
	 * @return
	 */
	private boolean contains(List<Tag> userTags, String tag) {
		for (Tag t : userTags) {
			if (t.mTag.equals(tag))
				return true;
		}
		return false;
	}
	
	/**
	 * Returns {@value #MAX_SUGGESTIONS} string for the given array of suggestion.
	 */
	private List<String> getSuggestionStrings(List<Tag> userTags, Suggestion[] suggestion) {
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < suggestion.length; i++) {
			if (!contains(userTags, suggestion[i].getTag())) {
				list.add(suggestion[i].getTag());
				if (list.size() == MAX_SUGGESTIONS)
					break;
			}
		}
		
		return list;
	}

	/**
	 * Gets score for the {@code candidate} tags with given {@code userTags}.
	 * 
	 * @param userTags
	 * @param candidate
	 * @return
	 */
	private float getScore(List<Tag> userTags, String candidate) {
		float coOccuringScore = 0;
		for (Tag userTag : userTags) {
			if (userTag.hasCoOccuring(candidate)) {
				coOccuringScore += p(candidate, userTag); 
			}
		}
		float frequentScore = 0;
		for (FrequentTag frequentTag : mFrequentTags) {
			if (!frequentTag.equals(candidate)) {
				frequentScore += p(candidate, frequentTag);
			}
		}
		
		return (float) (0.75 * coOccuringScore + 0.25 * frequentScore);
	}
}
