// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.model;

import edu.kaist.uilab.tagcontacts.Constants;
import edu.kaist.uilab.tagcontacts.ContactUtils;
import android.content.ContentValues;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;

/**
 * Class for representing a name entity.
 * 
 * <p> This class contains data which corresponds to the data of
 *  {@link ContactsContract.CommonDataKinds.StructuredName} 
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class NameEntity extends Entity {
	
	public static final String MIMETYPE = StructuredName.CONTENT_ITEM_TYPE;
	public static final String GIVEN_NAME = StructuredName.GIVEN_NAME;
	public static final String MIDDLE_NAME = StructuredName.MIDDLE_NAME;
	public static final String FAMILY_NAME = StructuredName.FAMILY_NAME;
	
	public static final String PROJECTION[] = {
		Data.RAW_CONTACT_ID,
		GIVEN_NAME,
		FAMILY_NAME,
		MIDDLE_NAME,
	};
	
	private String mGiven;
	private String mFamily;
	private String mMiddle;
	
	/**
	 * Constructor
	 */
	public NameEntity(String given, String middle, String family) {
		this(Constants.INVALID_ID, given, middle, family);
	}
	
	/**
	 * Constructs a new {@link NameEntity} instance with the given data.
	 * 
	 * <p> Other data is initialized to null and can be set via set methods.
	 * @param rawContactId
	 * @param given
	 * @param family
	 */
	public NameEntity(long rawContactId, String given, String middle, String family) {
		super(rawContactId);
		this.mGiven = ContactUtils.toString(given);
		this.mMiddle = ContactUtils.toString(middle);
		this.mFamily = ContactUtils.toString(family);
	}

	public String getGiven() {
		return mGiven;
	}
	
	public String getFamily() {
		return mFamily;
	}
	
	public String getMiddle() {
		return mMiddle;
	}
	
	@Override
	public String mimeType() {
		return MIMETYPE;
	}

	/**
	 * This data is the same as {@link NameEntity.PROJECTION}.
	 */
	@Override
	public String[] getProjection() {
		return PROJECTION;
	}
	
	@Override
	public ContentValues buildContentValuesWithNoId() {
		ContentValues values = new ContentValues();
		values.put(Data.MIMETYPE, NameEntity.MIMETYPE);
  	put(values, NameEntity.GIVEN_NAME, mGiven);
  	put(values, NameEntity.FAMILY_NAME, mFamily);
  	put(values, NameEntity.MIDDLE_NAME, mMiddle);
  	
  	return values;
	}

	@Override
	public String getSelectionForChange() {
		return Data.RAW_CONTACT_ID + "=?"
				+ " AND " + Data.MIMETYPE + "='" + MIMETYPE + "'"
				+ " AND " + NameEntity.FAMILY_NAME + "=?"
				+ " AND " + NameEntity.GIVEN_NAME + "=?"
				+ " AND " + NameEntity.MIDDLE_NAME + "=?";
	}
	
	@Override
	public String[] getSelectionArgsForChange() {
		return new String[] { String.valueOf(getRawContactId()), mFamily, mGiven, mMiddle };
	}

	/*
	 * Puts data into values if the associated value is not null.
	 */
	private void put(ContentValues values, String key, String value) {
		if (value != null) {
			values.put(key, value);
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof NameEntity))
			return false;
		
		NameEntity that = (NameEntity) o;
		if (mGiven == null && that.mGiven != null)
			return false;
		if (mMiddle == null && that.mMiddle != null)
			return false;
		if (mFamily == null && that.mFamily != null)
			return false;
		
		return mGiven.equals(that.mGiven) && mMiddle.equals(that.mMiddle)
				&& mFamily.equals(that.mFamily);
	}

	@Override
	public String toString() {
		return ContactUtils.concatenateStrings(" ", mGiven, mMiddle, mFamily, null);
	}
}
