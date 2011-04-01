// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.model;

import edu.kaist.uilab.tagcontacts.Constants;
import android.content.ContentValues;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Phone;

/**
 * Class for representing a phone entity.
 * 
 * <p> This class contains data which corresponds to the data of
 *  {@link ContactsContract.CommonDataKinds.Phone} 
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class PhoneEntity extends Entity {
	
	public static final String MIMETYPE = Phone.CONTENT_ITEM_TYPE;
	public static final String NUMBER = Phone.NUMBER;
	public static final String TYPE = Phone.TYPE;
	public static final String LABEL = Phone.LABEL;
	
	private static final String LABEL_PHONE = "phone";
	
	private String number;
	
	/**
	 * Constructor for an entity without id.
	 */
	public PhoneEntity(String number) {
		this(Constants.INVALID_ID, number);
	}
	
	/**
	 * Constructs a new {@link PhoneEntity} instance with the given data.
	 *
	 * @param rawContactId
	 * @param number
	 */
	public PhoneEntity(long rawContactId, String number) {
		super(rawContactId);
		this.number = number;
	}

	public String getNumber() {
		return number;
	}
	
	@Override
	public String mimeType() {
		return MIMETYPE;
	}

	@Override
	public String[] getProjection() {
		return null;
	}

	@Override
	public String getSelectionForChange() {
		return Data.RAW_CONTACT_ID + "=?"
				+ " AND " + Data.MIMETYPE + "='" + MIMETYPE + "'"
				+ " AND " + PhoneEntity.NUMBER + "=?";
	}
	
	@Override
	public String[] getSelectionArgsForChange() {
		return new String[] { String.valueOf(getRawContactId()), number };
	}
	
	@Override
	public ContentValues buildContentValuesWithNoId() {
		ContentValues values = new ContentValues();
		values.put(Data.MIMETYPE, PhoneEntity.MIMETYPE);
		// default to type custom "phone"
		values.put(PhoneEntity.TYPE, Phone.TYPE_CUSTOM);
		values.put(PhoneEntity.LABEL, LABEL_PHONE);
  	values.put(PhoneEntity.NUMBER, number);
  	
  	return values;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof PhoneEntity))
			return false;
		
		PhoneEntity that = (PhoneEntity) o;
		if (number == null && that.number != null)
			return false;
		
		return number.equals(that.number);
	}

	@Override
	public String toString() {
		if (number != null) {
			return number;
		}
		return "";
	}
}
