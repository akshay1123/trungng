// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.contagts.server;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

/**
 * A message for logging.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
@PersistenceCapable
public class LogMessage {
	private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmmssZ");
	
  @PrimaryKey
  @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
  private Key key;

  @Persistent
  private String deviceId;
  
	@Persistent
  private Date time;
  
  @Persistent
  private String type;

  @Persistent
  private String content;
  
  @Persistent
  private String id;

  /**
   * Constructor
   * 
   * @param deviceId
   * @param time
   * @param type
   * @param content
   * @param id
   */
  public LogMessage(String deviceId, String time, String type, String content,
  		String id) {
  	this.deviceId = deviceId;
  	this.time = new Date(Long.parseLong(time));
  	this.type = type;
  	this.content = content;
  	this.id = id;
  }

  public Key getKey() {
    return key;
  }
  
  public String getDeviceId() {
		return deviceId;
	}

	public Date getTime() {
		return time;
	}

	public String getType() {
		return type;
	}

	public String getContent() {
		return content;
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return deviceId + "," + dateFormatter.format(time) + ","
				+ type + "," + content + "," + id; 
	}
}