// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.model;

import android.content.ContentValues;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Website;
import edu.kaist.uilab.tagcontacts.Constants;

/**
 * Website entity
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class WebsiteEntity extends Entity {
	
	public static final String MIMETYPE = Website.CONTENT_ITEM_TYPE;
	public static final String URL = Website.URL;
	public static final String TYPE = Website.TYPE;
	public static final String LABEL = Website.LABEL;
	
	public static final String PROJECTION[] = {
		Data.RAW_CONTACT_ID,
		URL
	};
	
	public static final String LABEL_WEBSITE = "website";
	
	private String url;
	
	/**
	 * Constructor for a website without id.
	 */
	public WebsiteEntity(String url) {
		this(Constants.INVALID_ID, url);
	}
	
	/**
	 * Constructs a new {@link EmailEntity} instance with the given data.
	 * 
	 * @param rawContactId
	 * @param data
	 * @param type
	 */
	public WebsiteEntity(long rawContactId, String url) {
		super(rawContactId);
		this.url = url;
	}
	
	public String getUrl() {
		return url;
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
		values.put(Data.MIMETYPE, WebsiteEntity.MIMETYPE);
  	values.put(WebsiteEntity.URL, url);
  	// default to type custom labeled "website"
  	values.put(WebsiteEntity.TYPE, Website.TYPE_CUSTOM);
  	values.put(WebsiteEntity.LABEL, LABEL_WEBSITE);
  	
  	return values;
	}
	
	@Override
	public String[] getSelectionArgsForChange() {
		return new String[] {
				String.valueOf(getRawContactId()), url,
		};
	}

	@Override
	public String getSelectionForChange() {
		return Data.RAW_CONTACT_ID + "=?"
				+ " AND " + Data.MIMETYPE + "='" + MIMETYPE + "'"
				+ " AND " + WebsiteEntity.URL + "=?";
	}

	@Override
	public String toString() {
		if (url != null) {
			return url;
		}
		return "";
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof WebsiteEntity))
			return false;
		
		WebsiteEntity that = (WebsiteEntity) o;
		if (url == null && that.url != null)
			return false;
		return url.equals(that.url);
	}
}
