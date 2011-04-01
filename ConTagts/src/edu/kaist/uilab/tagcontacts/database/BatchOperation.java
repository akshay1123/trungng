// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.database;

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.util.Log;
import edu.kaist.uilab.tagcontacts.model.Entity;

/**
 * This class handles execution of batch operations on Contacts provider.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class BatchOperation {
	private final String TAG = "BatchOperation";
	
	private final ContentResolver mResolver;
	ArrayList<ContentProviderOperation> mOperations;
	ArrayList<Entity> mEntities;
	
	/**
	 * Constructor
	 * 
	 * @param context
	 * @param resolver
	 */
	public BatchOperation(Context context, ContentResolver resolver) {
		this.mResolver = resolver;
		mOperations = new ArrayList<ContentProviderOperation>();
		mEntities = new ArrayList<Entity>();
	}
	
	/**
	 * Gets the number of operations.
	 */
	public int size() {
		return mOperations.size();
	}
	
	/**
	 * Adds a new operation to this batch.
	 * @param op
	 */
	public void add(ContentProviderOperation op) {
		mOperations.add(op);
		mEntities.add(new DummyEntity());
	}
	
	/**
	 * Adds a new operation to this batch.
	 */
	public void add(ContentProviderOperation op, Entity entity) {
		mOperations.add(op);
		mEntities.add(entity);
	}
	
	/**
	 * Removes an operation from this batch.
	 * @param op
	 */
	public void remove(ContentProviderOperation op) {
		int pos = 0;
		int size = mOperations.size();
		// find location of the entity
		for (; pos < size; pos++) {
			if (mOperations.get(pos).equals(op)) {
				break;
			}
		}
		// if found, remove from both collections
		if (pos < size) {
			mEntities.remove(pos);
			mOperations.remove(pos);
		}
	}
	
	/**
	 * Removes the operation associated with {@code entity} from this batch.
	 * @param entity
	 */
	public void remove(Entity entity) {
		int pos = 0;
		int size = mEntities.size();
		// find location of the entity
		for (; pos < size; pos++) {
			if (mEntities.get(pos).equals(entity)) {
				break;
			}
		}
		// if found, remove from both collections
		if (pos < size) {
			mEntities.remove(pos);
			mOperations.remove(pos);
		}
	}
	
	/**
	 * Executes all operations (in a single transaction).
	 * 
	 * <p> This method returns the uri result of the first operation which can then be used to access
	 * the newly inserted contact if an insert new contact operation was executed.
	 */
	public Uri execute() {
		ContentProviderResult[] result = null;
		if (mOperations.size() > 0) {
			try {
				result = mResolver.applyBatch(ContactsContract.AUTHORITY,
						mOperations);
			} catch (final RemoteException e) {
				Log.e(TAG, "storing contact data failed", e);
			} catch (final OperationApplicationException e) {
				Log.e(TAG, "storing contact data failed", e);
			}
		}
		mOperations.clear();
		mEntities.clear();
		if (result != null) {
			return result[0].uri;
		} else {
			return null;
		}
	}

	/**
	 * Dummy entity that can be used to associate with operation that has no entity.
	 */
	class DummyEntity extends Entity {
		@Override
		public ContentValues buildContentValuesWithNoId() {
			return null;
		}

		@Override
		public String[] getProjection() {
			return null;
		}

		@Override
		public String mimeType() {
			return null;
		}

		@Override
		public boolean equals(Object o) {
			return false;
		}

		@Override
		public String[] getSelectionArgsForChange() {
			return null;
		}

		@Override
		public String getSelectionForChange() {
			return null;
		}
	}
}
