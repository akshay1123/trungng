// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

// TODO(trung): might not even provide this!
public class ContactProvider extends ContentProvider {

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		return null;
	}

  public Uri insert(Uri uri, ContentValues values) {
    throw new UnsupportedOperationException();
  }

	public int delete(Uri uri, String selection, String[] selectionArgs) {
	    throw new UnsupportedOperationException();
	}

	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
	    throw new UnsupportedOperationException();
	}
}
