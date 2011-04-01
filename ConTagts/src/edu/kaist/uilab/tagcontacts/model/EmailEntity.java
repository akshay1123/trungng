// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.model;

import android.content.ContentValues;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Email;
import edu.kaist.uilab.tagcontacts.Constants;

/**
 * Email entity
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 *
 */
public class EmailEntity extends Entity {

	public static final String MIMETYPE = Email.CONTENT_ITEM_TYPE;
	public static final String DATA = Email.DATA;
	public static final String TYPE = Email.TYPE;
	public static final String LABEL = Email.LABEL;

	public static final String[] PROJECTION = {
		Data.RAW_CONTACT_ID,
		DATA,
	};
	
	public static final String LABEL_EMAIL = "email";
	private String data;
	
	/**
	 * Constructor for an email without id.
	 */
	public EmailEntity(String data) {
		this(Constants.INVALID_ID, data);
	}
	
	/**
	 * Constructs a new {@link EmailEntity} instance with the given data.
	 * 
	 * @param rawContactId
	 * @param data
	 * @param type
	 */
	public EmailEntity(long rawContactId, String data) {
		super(rawContactId);
		this.data = data;
	}
	
	public String getData() {
		return data;
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
		values.put(Data.MIMETYPE, EmailEntity.MIMETYPE);
		// default email type to custom "email"
		values.put(EmailEntity.TYPE, Email.TYPE_CUSTOM);
		values.put(EmailEntity.LABEL, LABEL_EMAIL);
  	values.put(EmailEntity.DATA, data);
  	
  	return values;
	}
	
	@Override
	public String getSelectionForChange() {
		return Data.RAW_CONTACT_ID + "=?"
				+ " AND " + Data.MIMETYPE + "='" + EmailEntity.MIMETYPE + "'"		
				+ " AND " + EmailEntity.DATA + "=?";
	}

	@Override
	public String[] getSelectionArgsForChange() {
		return new String[] { String.valueOf(getRawContactId()), data };
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof EmailEntity)) {
			return false;
		}
		
		EmailEntity that = (EmailEntity) o;
		if (data == null && that.data != null)
			return false;
		
		return data.equals(that.data);
	}
	
	@Override
	public String toString() {
		if (data != null) {
			return data;
		}
		return "";
	}
}
