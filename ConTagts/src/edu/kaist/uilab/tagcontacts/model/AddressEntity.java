// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.model;

import android.content.ContentValues;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import edu.kaist.uilab.tagcontacts.Constants;
import edu.kaist.uilab.tagcontacts.ContactUtils;

/**
 * Address entity
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class AddressEntity extends Entity {

	public static final String MIMETYPE = StructuredPostal.CONTENT_ITEM_TYPE;
	public static final String STREET = StructuredPostal.STREET;
	public static final String CITY = StructuredPostal.CITY;
	public static final String REGION = StructuredPostal.REGION;
	public static final String POSTCODE = StructuredPostal.POSTCODE;
	public static final String TYPE = StructuredPostal.TYPE;
	
	public static final String PROJECTION[] = {
		Data.RAW_CONTACT_ID,
		STREET,
		CITY,
		REGION,
		POSTCODE
	};
	
	private String mStreet;
	private String mCity;
	private String mRegion;
	private String mPostcode;
	
	/**
	 * Constructor
	 */
	public AddressEntity(String street, String city, String region,
			String postcode) {
		this(Constants.INVALID_ID, street, city, region, postcode);
	}
	
	/**
	 * Constructor with id
	 */
	public AddressEntity(long rawContactId, String street, String city,
			String region, String postcode) {
		super(rawContactId);
		this.mStreet = ContactUtils.toString(street);
		this.mCity = ContactUtils.toString(city);
		this.mRegion = ContactUtils.toString(region);
		this.mPostcode = ContactUtils.toString(postcode);
	}
	
	@Override
	public String[] getProjection() {
		return PROJECTION;
	}

	@Override
	public String mimeType() {
		return MIMETYPE;
	}

	public String getStreet() {
		return mStreet;
	}
	
	public String getCity() {
		return mCity;
	}

	public String getRegion() {
		return mRegion;
	}

	public String getPostcode() {
		return mPostcode;
	}
	
	@Override
	public ContentValues buildContentValuesWithNoId() {
		ContentValues values = new ContentValues();
		values.put(Data.MIMETYPE, MIMETYPE);
		// consider all organization type as home
		values.put(AddressEntity.TYPE, StructuredPostal.TYPE_HOME);
		put(values, AddressEntity.STREET, mStreet);
		put(values, AddressEntity.CITY, mCity);
  	put(values, AddressEntity.REGION, mRegion);
  	put(values, AddressEntity.POSTCODE, mPostcode);
  	
  	return values;
	}

	@Override
	public String getSelectionForChange() {
		return Data.RAW_CONTACT_ID + "=?"
			+ " AND " + Data.MIMETYPE + "='" + MIMETYPE + "'"
			+ " AND " + AddressEntity.STREET + "=?"
			+ " AND " + AddressEntity.CITY + "=?"
			+ " AND " + AddressEntity.REGION + "=?"
			+ " AND " + AddressEntity.POSTCODE + "=?";
	}
	
	@Override
	public String[] getSelectionArgsForChange() {
		return new String[] { String.valueOf(getRawContactId()), mStreet, mCity, mRegion, mPostcode };
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
		if (o == null || !(o instanceof AddressEntity)) {
			return false;
		}
		
		AddressEntity that = (AddressEntity) o;
		if (mStreet == null && that.mStreet != null)
			return false;
		if (mCity == null && that.mCity != null)
			return false;
		if (mRegion == null && that.mRegion != null)
			return false;
		if (mPostcode == null && that.mPostcode != null)
			return false;
		
		return mStreet.equals(that.mStreet) && mCity.equals(that.mCity)
				&& mRegion.equals(that.mRegion) && mPostcode.equals(that.mPostcode);
	}

	@Override
	public String toString() {
		return ContactUtils.concatenateStrings(",", mStreet, mCity, mRegion, mPostcode);
	}
}
