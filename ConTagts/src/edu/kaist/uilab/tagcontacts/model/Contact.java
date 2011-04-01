// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for modeling the a contact with a bit more information than {@link BasicContact}.
 * Specifically, a {@link Contact} extends a {@link BasicContact} and adds its own information
 * including tags and groups.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class Contact extends BasicContact {
	List<String> tags;
	List<GroupEntity> groups;
	
	/**
	 * Constructs a new instance with given data.
	 */
	public Contact(long rawContactId, String name, List<String> tags,
			List<GroupEntity> groups) {
		super(rawContactId, name);
		this.tags = tags;
		this.groups = groups;
	}

	/**
	 * Constructor
	 * 
	 * @param rawContactId
	 */
	public Contact(long rawContactId, String name) {
		this(rawContactId, name, new ArrayList<String>(),
				new ArrayList<GroupEntity>());
	}
	
	public void addTag(String tag) {
		tags.add(tag);
	}
	
	public void addGroup(GroupEntity group) {
		groups.add(group);
	}

	public void removeGroup(GroupEntity entity) {
		groups.remove(entity);
	}
	
	public List<String> getTags() {
		return tags;
	}

	public List<GroupEntity> getGroups() {
		return groups;
	}
	
	/**
	 * Returns true if this contact has a group entity with name {@code name}.
	 */
	public boolean hasGroup(long id) {
		for (GroupEntity entity : groups) {
			if (entity.getId() == id) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		return getRawContactId() + "; " + getName();
	}
}
