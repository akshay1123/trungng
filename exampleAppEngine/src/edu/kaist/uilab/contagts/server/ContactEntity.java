// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.contagts.server;

import java.util.ArrayList;
import java.util.List;

import javax.jdo.annotations.Extension;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import edu.kaist.uilab.contagts.server.servlet.ServletUtils;

/**
 * Represents a contact entity in the global network. 
 * 
 * <p> A contact entity is uniquely determined by its device id and has a unique
 * phone number.
 * 
 * <p> This can be thought of as a node in the global graph.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
@PersistenceCapable
public class ContactEntity {

	@PrimaryKey
	@Persistent
	private String id;
	
	@Persistent
	private String number;
	
	@Persistent
	@Extension(vendorName = "datanucleus", key = "gae.unindexed", value = "true")
	private List<Friend> friends; // this contact's friends
	
	@Persistent
	@Extension(vendorName = "datanucleus", key = "gae.unindexed", value = "true")
	private List<Label> labels; // labels that this contact was annotated with
	
	/**
	 * Constructor
	 * 
	 * @param deviceId
	 * @param number
	 */
	public ContactEntity(String deviceId, String number) {
		this.id = deviceId;
		this.number = number;
		this.friends = new ArrayList<Friend>();
	}
	
	public String getDeviceId() {
		return id;
	}

	public void setDeviceId(String deviceId) {
		this.id = deviceId;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	/**
	 * Returns the internal list of {@link Friend}s for this entity.
	 * 
	 * <p> Caller can make changes to the returned list, for example by adding a new friend.
	 * @return
	 */
	public List<Friend> getFriends() {
		return friends;
	}

	public void setFriends(ArrayList<Friend> friends) {
		this.friends = friends;
	}

	public void setLabels(List<Label> newLabels) {
		labels = newLabels;
	}

	/**
	 * Returns the internal list of {@link Label}s for this entity.
	 * 
	 * <p> When another user tags/annotates this contact with a label, that label is
	 * considered belongs to this contact. This list contains all labels that this
	 * entity was annotated with.
	 *  
	 * @return
	 */
	public List<Label> getLabels() {
		return labels;
	}

	/**
	 * Gets the weight (relationship strength) of this contact entity and the friend
	 * whose device id is {@code deviceId}.
	 * 
	 * <p> In other words, this computes w(this, friend) which is normalized and
	 * should be smaller than 1 in most cases.
	 * 
	 * @param friend
	 * @return
	 * 			 the relationship strength between the two, 0 if they are not friend
	 */
	public float getWeight(String deviceId) {
		for (Friend f : friends) {
			if (f.deviceId.equals(deviceId)) {
				return f.getWeight();
			}
		}
		
		return 0;
	}
	
	@Override
	public String toString() {
		return "contact=[" + id + "," + number + "," + ServletUtils.toString(labels)
				+ "," + ServletUtils.toString(friends) + "]";
	}
}
