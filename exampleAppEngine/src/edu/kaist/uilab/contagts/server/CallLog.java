// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.contagts.server;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

/**
 * A class for representing a phone call log.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */

@PersistenceCapable
public class CallLog {
  @PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  private Key key;

  @Persistent
  private String deviceId;

  @Persistent
  private String number;
  
  @Persistent
  private int duration;
  
	@Persistent
  private long date;

  public CallLog(String deviceId, String number, int duration, long date) {
  	this.deviceId = deviceId;
  	this.number = number;
  	this.duration = duration;
  	this.date = date;
  }

	public Key getKey() {
		return key;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public long getDate() {
		return date;
	}

	public void setDate(long date) {
		this.date = date;
	}

	@Override
	public String toString() {
		return deviceId + "," + number + "," + duration + "," + date;
	}
}