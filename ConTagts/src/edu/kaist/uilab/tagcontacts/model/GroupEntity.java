// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.model;

import android.content.ContentValues;
import android.provider.ContactsContract.Data;

/**
 * Group entity for each group of a contact.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class GroupEntity extends Entity {
	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/group";
	public static final String MIMETYPE = CONTENT_ITEM_TYPE;

	/**
	 * Column where group id of this entity is stored.
	 */
	public static final String ID = "data1";
	
	private long id;

	/**
	 * Constructor for a group entity that belongs to a contact.
	 */
	public GroupEntity(long rawContactId, long id) {
		super(rawContactId);
		this.id = id;
	}
	
	/**
	 * Constructor for a group entity that is independent of any contact.
	 */
	public GroupEntity(long id) {
		this(-1, id);
	}

	@Override
	public String mimeType() {
		return MIMETYPE;
	}

	public long getId() {
		return id;
	}
	
	@Override
	public String[] getProjection() {
		return null;
	}
	
	@Override
	public String getSelectionForChange() {
		return Data.RAW_CONTACT_ID + "=?"
		 		+ " AND " + Data.MIMETYPE + "='" + MIMETYPE + "'"
				+ " AND " + GroupEntity.ID + "=?";
	}
	
	@Override
	public String[] getSelectionArgsForChange() {
		return new String[] { String.valueOf(getRawContactId()), String.valueOf(id) };
	}

	@Override
	public ContentValues buildContentValuesWithNoId() {
		ContentValues values = new ContentValues();
		values.put(Data.MIMETYPE, GroupEntity.MIMETYPE);
  	values.put(GroupEntity.ID, id);

  	return values;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof GroupEntity)) {
			return false;
		}
		
		GroupEntity that = (GroupEntity) o;
		return id == that.id;
	}
}
