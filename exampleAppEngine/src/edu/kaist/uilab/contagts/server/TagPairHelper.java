// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.contagts.server;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import edu.kaist.uilab.contagts.server.servlet.ServletUtils;

/**
 * Helper class for updating the datastore for {@link TagPair} entity.
 *  
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public final class TagPairHelper {
	
  /**
   * Updates the {@code TagPairs} database.
   * 
   * @param labels
   */
	public static void updateTagPairs(String labels) {
		PersistenceManager pm = ServletUtils.getPmf().getPersistenceManager();
  	List<TagPair> newPairs = new ArrayList<TagPair>();
  	String[] tags = toStringArray(labels);
  	TagPair pair = null;
  	for (int i = 0; i < tags.length - 1; i++) {
  		for (int j = i + 1; j < tags.length; j++) {
  			pair = getTagPair(pm, tags[i], tags[j]);
  			if (pair == null) {
  				newPairs.add(new TagPair(tags[i], tags[j], 1));
  			} else {
  				pair.setValue(pair.getValue() + 1);
  			}
  		}
  	}
  	try {
    	if (newPairs.size() > 0) {
   			pm.makePersistentAll(newPairs);
   		}
  	} finally {
  		pm.close();
  	}
	}

  /**
   * Returns the {@link TagPair} which contains {@code tag1} and {@code tag2}.
   *  
   * @param pm
   * @param tag1
   * @param tag2
   * @return
   */
  public static TagPair getTagPair(PersistenceManager pm, String tag1, String tag2) {
  	String tag, relatedTag;
  	if (tag1.compareTo(tag2) < 0) {
  		tag = tag1;
  		relatedTag = tag2;
  	} else {
  		tag = tag2;
  		relatedTag = tag1;
  	}
  	Query query = pm.newQuery(TagPair.class);
  	query.setFilter("tag == tagParam && relatedTag == relatedTagParam");
  	query.declareParameters("String tagParam, String relatedTagParam");
  	query.setUnique(true);
  	try {
  		return (TagPair) query.execute(tag, relatedTag);
  	} finally {
  		query.closeAll();
  	}
  }
	
  private static String[] toStringArray(String labels) {
  	StringTokenizer tokenizer = new StringTokenizer(labels.substring(1, labels.length() - 1),
  			"-");
  	String[] results = new String[tokenizer.countTokens()];
  	int idx = 0;
  	while (tokenizer.hasMoreTokens()) {
  		results[idx++] = tokenizer.nextToken();
  	}
  	
  	return results;
  }
}
