// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.contagts.server;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

/**
 * Represents a friend of some {@link ContactEntity}.
 *  
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
@PersistenceCapable
public class Friend {
	
	private static final float MINUTES_COEFFICIENT = 0.3f;
	private static final float CALLS_COEFFICIENT = 0.7f;
	private static final float NORMALIZATION =
		MINUTES_COEFFICIENT * 500 + CALLS_COEFFICIENT * 100;
	
	@PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  private Key key;

	@Persistent
	String deviceId; // device id of this friend
	
	@Persistent
	float minutes; // number of minutes communicated

	@Persistent
	int calls; // number of calls
	
	/**
	 * Constructor
	 * 
	 * @param deviceId device id of this friend
	 */
	public Friend(String deviceId, float mins, int calls) {
		this.deviceId = deviceId;
		this.minutes = mins;
		this.calls = calls;
	}

	public Key getKey() {
		return key;
	}

	public void setKey(Key key) {
		this.key = key;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public float getMinutes() {
		return minutes;
	}

	public void setMinutes(float minutes) {
		this.minutes = minutes;
	}
	
	public void addMinutes(float val) {
		minutes += val;
	}
	
	public int getCalls() {
		return calls;
	}
	
	public void setCalls(int calls) {
		this.calls = calls;
	}
	
	/**
	 * Returns the strength of relationship for this friend.
	 * 
	 * <p> The weight is normalized and smaller than 1 in most cases.
	 * 
	 * @return
	 */
	public float getWeight() {
		return (MINUTES_COEFFICIENT * minutes + CALLS_COEFFICIENT * calls) / NORMALIZATION; 
	}
	
	@Override
	public String toString() {
		return "friend=[" + deviceId + "," + minutes + "," + calls + "]";
	}
}
