// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.model;

import android.content.ContentValues;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Note;
import edu.kaist.uilab.tagcontacts.Constants;

/**
 * Note entity.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class NoteEntity extends Entity {

	public static final String MIMETYPE = Note.CONTENT_ITEM_TYPE;
	public static final String NOTE = Note.NOTE;
	
	public static final String[] PROJECTION = {
		Data.RAW_CONTACT_ID,
		NOTE,
	};
	
	private String note;
	
	/**
	 * Constructor for a note without id.
	 */
	public NoteEntity(String note) {
		this(Constants.INVALID_ID, note);
	}
	
	/**
	 * Constructs a new {@link NoteEntity} instance with the given data.
	 * 
	 * @param rawContactId
	 */
	public NoteEntity(long rawContactId, String note) {
		super(rawContactId);
		this.note = note;
	}
	
	public String getNote() {
		return note;
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
		values.put(Data.MIMETYPE, NoteEntity.MIMETYPE);
  	values.put(NoteEntity.NOTE, note);
  	
  	return values;
	}


	@Override
	public String[] getSelectionArgsForChange() {
		return new String[] { String.valueOf(getRawContactId()), note };
	}

	@Override
	public String getSelectionForChange() {
		return Data.RAW_CONTACT_ID + "=?"
				+ " AND " + Data.MIMETYPE + "='" + MIMETYPE + "'"
				+ " AND " + NoteEntity.NOTE + "=?";
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof NoteEntity)) 
			return false;
		
		NoteEntity that = (NoteEntity) o;
		if (note == null && that.note != null)
			return false;
		
		return note.equals(that.note);
	}
	
	@Override
	public String toString() {
		if (note != null) {
			return note;
		}
		return "";
	}
}
