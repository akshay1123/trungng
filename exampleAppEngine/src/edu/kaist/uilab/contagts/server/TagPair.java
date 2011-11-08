// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.contagts.server;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

/**
 * Class for representation of relatedness between two tags.
 * 
 * <p> In storing a pair of tags in the database, the smaller tag is always stored in
 * the {@code tag} field whereas the larger is stored in the {@code relatedTag} field.
 * (Here comparison is done according to the lexicographical order).
 * 
 * <p> This constraint is particularly important when you query for a pair of tags. Unless
 * the {code tag} field is alphabetically smaller than the {@code relatedTag}, no result
 * will be returned.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
@PersistenceCapable
public class TagPair {
  @PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  private Key key;

  @Persistent
  private String tag; // a tag
  
  @Persistent
  private String relatedTag; // a related tag
  
  @Persistent
  private int value; // relatedness of two tags
  
  /**
   * Constructor
   * 
   * @param tag1 a tag
   * @param tag2 another tag
   * @param value relatedness of two tags
   */
  public TagPair(String tag1, String tag2, int value) {
  	if (tag1.compareTo(tag2) < 0) {
    	this.tag = tag1;
    	this.relatedTag = tag2;
  	} else {
  		this.tag = tag2;
  		this.relatedTag = tag1;
  	}
  	this.value = value;
  }

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getRelatedTag() {
		return relatedTag;
	}

	public void setRelatedTag(String relatedTag) {
		this.relatedTag = relatedTag;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public Key getKey() {
		return key;
	}
	
	@Override
	public String toString() {
		return "[" + tag + "," + relatedTag + "," + value + "]";
	}
}
