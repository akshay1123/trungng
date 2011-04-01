// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.model;

import android.content.ContentValues;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import edu.kaist.uilab.tagcontacts.Constants;
import edu.kaist.uilab.tagcontacts.ContactUtils;

public class JobEntity extends Entity {

	public static final String MIMETYPE = Organization.CONTENT_ITEM_TYPE;
	public static final String COMPANY = Organization.COMPANY;
	public static final String TITLE = Organization.TITLE;
	public static final String DEPARTMENT = Organization.DEPARTMENT;
	public static final String TYPE = Organization.TYPE;
	
	public static final String PROJECTION[] = {
		Data.RAW_CONTACT_ID,
		COMPANY,
		TITLE,
		DEPARTMENT,
	};

	private String company;
	private String title;
	private String department;
	
	/**
	 * Constructor
	 */
	public JobEntity(String company, String title, String department) {
		this(Constants.INVALID_ID, company, title, department);
	}
	
	/**
	 * Constructor with id
	 */
	public JobEntity(long rawContactId, String company, String title,
			String department) {
		super(rawContactId);
		this.company = ContactUtils.toString(company);
		this.title = ContactUtils.toString(title);
		this.department = ContactUtils.toString(department);
	}

	public String getCompany() {
		return company;
	}

	public String getTitle() {
		return title;
	}

	public String getDepartment() {
		return department;
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
		values.put(Data.MIMETYPE, JobEntity.MIMETYPE);
		// default type to work
		values.put(JobEntity.TYPE, Organization.TYPE_WORK);
  	put(values, JobEntity.COMPANY, company);
  	put(values, JobEntity.TITLE, title);
  	put(values, JobEntity.DEPARTMENT, department);
  	
  	return values;
	}
	
	@Override
	public String[] getSelectionArgsForChange() {
		return new String[] {
			String.valueOf(getRawContactId()), company, title, department,	
		};
	}

	@Override
	public String getSelectionForChange() {
		return Data.RAW_CONTACT_ID + "=?"
				+ " AND " + Data.MIMETYPE + "='" + MIMETYPE + "'"
				+ " AND " + JobEntity.COMPANY + "=?"
				+ " AND " + JobEntity.TITLE + "=?"
				+ " AND " + JobEntity.DEPARTMENT + "=?";
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
		if (o == null || !(o instanceof JobEntity)) {
			return false;
		}
		
		JobEntity that = (JobEntity) o;
		if (company == null && that.company != null)
			return false;
		if (title == null && that.title != null)
			return false;
		if (department == null && that.department != null)
			return false;
		
		return (company.equals(that.company) && title.equals(that.title)
				&& department.equals(that.department));
	}
	
	@Override
	public String toString() {
		return ContactUtils.concatenateStrings("\n", company, title, department, null);
	}
}
