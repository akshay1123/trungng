// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.model;

import android.content.ContentValues;
import android.provider.ContactsContract.Data;

/**
 * Abstract class for representing an entity.
 * 
 * <p> All entity should extends this abstract class.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public abstract class Entity {

	/**
	 * Entity types. 
	 */
	public enum EntityType {
		ADDRESS_ENTITY, EMAIL_ENTITY, GROUP_ENTITY, JOB_ENTITY, NAME_ENTITY,
		NICKNAME_ENTITY, NOTES_ENTITY, PHONE_ENTITY, TAG_ENTITY, WEBSITE_ENTITY
	}

	private long rawContactId;
	
	/**
	 * Default constructor
	 */
	public Entity() {
		
	}
	
	/**
	 * Constructor
	 * 
	 * @param rawContactId the raw contact id of this entity
	 * @param 
	 */
	public Entity(long rawContactId) {
		this.rawContactId = rawContactId;
	}
	
	public long getRawContactId() {
		return rawContactId;
	}

	/**
	 * Data type represented by this entity. Sub-classes must override this and
	 * supply their own values.
	 * 
	 * <p> Notes that this method is class specific rather than instance specific,
	 * i.e., it should have been a static method rather than an instance method.
	 * However, it is being declared as instance method for the sake of using
	 * inheritance and polymorphism.
	 */
	public abstract String mimeType();
	
	/**
	 * Gets projection array (to be used with querying data) for this entity.
	 * 
	 * <p> Notes that this method is class specific rather than instance specific,
	 * i.e., it should have been a static method rather than an instance method.
	 * However, it is being declared as instance method for the sake of using
	 * inheritance and polymorphism.
	 */
	public abstract String[] getProjection();
	
	/**
	 * Gets selection that can be used with any operating which might need to
	 * change the data (update or delete).
	 * 
	 * <p> Sub-classes should override this method by defining the appropriate
	 *  selection string.
	 */
	public abstract String getSelectionForChange();

	/**
	 * Gets selection arguments that are associated with the selection returned by
	 *  {@link #getSelectionForChange()}.
	 * 
	 * <p>Sub-classes must override this method by defining the appropriate
	 *  selection arguments.
	 */
	public abstract String[] getSelectionArgsForChange();
	
	/**
	 * Builds content values for the this entity with its id.
	 * 
	 * <p> The returned value can only be used for inserting an entity into the
	 * database. {@link ContentValues} for deleting or updating are provided
	 * by {@link #buildContentValuesWithNoId()}.
	 * 
	 * <p> The default implementation takes result from the method
	 * {@link #buildContentValuesWithNoId()} and adds the id to the result. This
	 * works fine for all entity.
	 */
	public ContentValues buildContentValuesWithId() {
		ContentValues values = buildContentValuesWithNoId();
  	values.put(Data.RAW_CONTACT_ID, getRawContactId());
		
		return values;
	}
	
	/**
	 * Builds content values for the this entity with no id. This is typically
	 * used for deleting or updating data.
	 */
	public abstract ContentValues buildContentValuesWithNoId();

	@Override
	public abstract boolean equals(Object o);
}
