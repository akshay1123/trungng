// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.contagts.server;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

/**
 * Class for representing a label (which could be a group or a tag).
 * 
 * <p> This class is composed of tagger (device id of the person who created the label)
 *  and the label itself.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
@PersistenceCapable
public class Label {
  
	@PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  private Key key;
	
	@Persistent
	private String deviceId; // device id of the person who created this label
  
	@Persistent
  private String label; // the label

  /**
   * Constructor
   * 
   * @param deviceId device id of the person who created this label
   * @param label the label
   */
  public Label(String deviceId, String label) {
  	this.deviceId = deviceId;
  	this.label = label;
  }
  
  @Override
  public String toString() {
  	return deviceId + ":" + label;
  }

	public void setKey(Key key) {
		this.key = key;
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

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
	
	/**
	 * Indicates whether {@code o} equals this {@code Label}.
	 * 
	 * <p> Two {@code Labels} are equal if their {@code deviceId} and {@code label}
	 * are the same.
	 * 
	 */
	@Override
	public boolean equals(Object o) {
		Label that = (Label) o;
		return (that.deviceId.equals(deviceId) && that.label.equals(label));
	}
}
