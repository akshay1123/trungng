// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.contagts.server;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

/**
 * Class for representing a search query.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
@PersistenceCapable
public class SearchQuery {

	@PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;
	
	@Persistent
	private String query;
	
	@Persistent
	private int times;
	
	@Persistent
	private String city;
	
	/**
	 * Constructor
	 * 
	 * @param query
	 */
	public SearchQuery(String query, String city) {
		this.query = query;
		this.city = city;
		this.times = 0;
	}

	public String getQuery() {
		return query;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCity() {
		return city;
	}
	
	public void increaseTimes() {
		this.times++;
	}

	public void setTimes(int times) {
		this.times = times;
	}

	public int getTimes() {
		return times;
	}
	
	@Override
	public String toString() {
		return query + "; " + times;
	}

	public void setKey(Key key) {
		this.key = key;
	}

	public Key getKey() {
		return key;
	}
}
