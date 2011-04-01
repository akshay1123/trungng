// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.model;

import android.content.ContentValues;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import edu.kaist.uilab.tagcontacts.Constants;

/**
 * Nickname entity
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class NicknameEntity extends Entity {

	public static final String MIMETYPE = Nickname.CONTENT_ITEM_TYPE;
	public static final String NAME = Nickname.NAME;
	public static final String TYPE = Nickname.TYPE;
	public static final String LABEL = Nickname.LABEL;
	
	public static final String[] PROJECTION = {
		Data.RAW_CONTACT_ID,
		NAME,
	};
	
	public static final String LABEL_NICKNAME = "nickname";
	
	private String name;
	
	/**
	 * Constructor for a nickname without id.
	 */
	public NicknameEntity(String name) {
		this(Constants.INVALID_ID, name);
	}
	
	/**
	 * Constructs a new {@link NicknameEntity} instance with the given data.
	 * 
	 * @param rawContactId
	 */
	public NicknameEntity(long rawContactId, String name) {
		super(rawContactId);
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public String[] getProjection() {
		return PROJECTION;
	}

	@Override
	public String mimeType() {
		return MIMETYPE;
	}

	@Override
	public ContentValues buildContentValuesWithNoId() {
		ContentValues values = new ContentValues();
		values.put(Data.MIMETYPE, NicknameEntity.MIMETYPE);
  	values.put(NicknameEntity.NAME, name);
  	// default to type custom labeled "nickname"
  	values.put(NicknameEntity.TYPE, Nickname.TYPE_CUSTOM);
  	values.put(NicknameEntity.LABEL, LABEL_NICKNAME);
  	
  	return values;
	}
	
	@Override
	public String[] getSelectionArgsForChange() {
		return new String[] { String.valueOf(getRawContactId()), name };
	}

	@Override
	public String getSelectionForChange() {
		return Data.RAW_CONTACT_ID + "=?"
				+ " AND " + Data.MIMETYPE + "='" + MIMETYPE + "'"
				+ " AND " + NicknameEntity.NAME + "=?";
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof NicknameEntity))
			return false;
		
		NicknameEntity that = (NicknameEntity) o;
		if (name == null && that.name != null)
			return false;
		
		return name.equals(that.name);
	}

	@Override
	public String toString() {
		if (name != null) {
			return name;
		}
		return "";
	}
}
