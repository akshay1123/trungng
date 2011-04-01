// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.model;

import android.content.ContentValues;
import android.provider.ContactsContract.Data;

/**
 * Tag entity for each tag of a contact.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class TagEntity extends Entity {
	
	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/tag";
	public static final String MIMETYPE = CONTENT_ITEM_TYPE;
	/**
	 * Column where the label of this entity is stored.
	 */
	public static final String LABEL = "data1";
	
	private String label;
	
	/**
	 * Constructor
	 * 
	 * @param rawContactId
	 * @param label
	 */
	public TagEntity(long rawContactId, String label) {
		super(rawContactId);
		this.label = label;
	}
	
	public TagEntity(String label) {
		this.label = label;
	}
	
	public String getLabel() {
		return label;
	}

	@Override
	public String mimeType() {
		return MIMETYPE;
	}

	@Override
	public String[] getProjection() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getSelectionForChange() {
		return Data.RAW_CONTACT_ID + "=?"
				+ " AND " + Data.MIMETYPE + "='" + MIMETYPE + "'"
				+ " AND " + TagEntity.LABEL + "=?";
	}
	
	@Override
	public String[] getSelectionArgsForChange() {
		return new String[] { String.valueOf(getRawContactId()), label };
	}
	
	@Override
	public ContentValues buildContentValuesWithNoId() {
		ContentValues values = new ContentValues();
		values.put(Data.MIMETYPE, TagEntity.MIMETYPE);
  	values.put(TagEntity.LABEL, label);

  	return values;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof TagEntity))
			return false;
		
		TagEntity that = (TagEntity) o;
		if (label == null && that.label != null)
			return false;
		
		return label.equals(that.label);
	}
	
	@Override
	public String toString() {
		if (label != null) {
			return label;
		}
		return "";
	}
}
