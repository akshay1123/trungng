// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.model;

/**
 * Class for representing a raw contact which are composed of a raw id and a name.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class RawContact implements Comparable<RawContact> {
	
	private String mName;
	private long mRawContactId;
	
	public RawContact(long rawContactId, String name) {
		mRawContactId = rawContactId;
		mName = name;
	}
	
	public void setName(String name) {
		mName = name;
	}
	
	public String getName() {
		return mName;
	}
	
	public long getRawContactId() {
		return mRawContactId;
	}

	@Override
	public int compareTo(RawContact another) {
		return mName.toLowerCase().compareTo(another.mName.toLowerCase());
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null && !(o instanceof RawContact)) {
			return false;
		}
		RawContact that = (RawContact) o;
		
		return mRawContactId == that.mRawContactId;
	}
}
