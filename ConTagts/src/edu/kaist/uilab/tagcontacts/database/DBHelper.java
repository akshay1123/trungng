// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.database;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;
import edu.kaist.uilab.tagcontacts.R;
import edu.kaist.uilab.tagcontacts.ServerConnector;
import edu.kaist.uilab.tagcontacts.model.Group;
import edu.kaist.uilab.tagcontacts.tagsuggestion.FrequentTag;
import edu.kaist.uilab.tagcontacts.tagsuggestion.Tag;

/**
 * A helper class for communicating with database.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class DBHelper {
	
	private static final String TAG = "DBHelper";
	public static final int MAX_LOCAL_QUERY = 6;
	// database information
	private static final int DB_VERSION = 3;
	private static final String DB_NAME = "uicontacts";
	private static final String DB_TABLE_GROUP = "contactsgroup";
	private static final String DB_TABLE_TAGS = "FTScontactstags";
	private static final String DB_TABLE_FREQUENT_TAGS = "frequenttags";
	private static final String DB_TABLE_SEARCH = "search";
	
	// columns of the group table
	public static final String COL_GROUPS_ID = "_id";	
	private static final String COL_GROUPS_LABEL = "label";
	private static final String COL_GROUPS_COLOR = "color";
	// columns of the tags table
	private static final String COL_TAGS_TAG = "tag";
	private static final String COL_TAGS_FREQUENCY = "freq";
	private static final String COL_TAGS_COOCURING = "cooccur";
	// columns of the frequents tags table
	private static final String COL_FREQUENT_TAG = "tag";
	private static final String COL_FREQUENT_TIMES = "times";
	// columns of the frequent search query table
	private static final String COL_SEARCH_QUERY = "query";
	private static final String COL_SEARCH_TIMES = "times";
	
	private static final String CLASSNAME = DBHelper.class.getSimpleName();
	private static final String[] projection_groups = new String[] {
		COL_GROUPS_ID, COL_GROUPS_LABEL, COL_GROUPS_COLOR
	};
	private static final String[] projection_tags = new String[] {
		COL_TAGS_TAG, COL_TAGS_FREQUENCY, COL_TAGS_COOCURING
	};
	private static final String[] projection_frequent = new String[] {
		COL_FREQUENT_TAG, COL_FREQUENT_TIMES
	};
	private static final String[] projection_search = new String[] {
		COL_SEARCH_QUERY, COL_SEARCH_TIMES
	};
	
	private final DBOpenHelper mDatabaseOpenHelper; // share for all connections
	private static boolean tagSuggestionReady = true;

	/**
	 * Creates a new DBHelper that handle communication with database. 
	 * 
	 * @param context context of the application that uses this database
	 */
	public DBHelper(Context context) {
		mDatabaseOpenHelper = new DBOpenHelper(context, DB_NAME, null, DB_VERSION);
	}
	
	public void close() {
		mDatabaseOpenHelper.close();
	}
		
	/**
	 * Inserts a new group.
	 * 
	 * @param group the group to be inserted
	 */
	public void insertGroup(Group group) {
		try {
			mDatabaseOpenHelper.getWritableDatabase().insert(DB_TABLE_GROUP, null,
					buildContentValues(group));
		} catch (Exception e) {
			Log.e(TAG, "error inserting group", e);
		}
	}
	
	/**
	 * Deletes the group at the given {@code rowId}.
	 * 
	 * @param rowId
	 */
	public void deleteGroup(long rowId) {
		if (rowId != Group.NO_GROUP_ID) {
			try {
				mDatabaseOpenHelper.getWritableDatabase().delete(DB_TABLE_GROUP, COL_GROUPS_ID + "=?",
						new String[] { String.valueOf(rowId) });
			} catch (Exception e) {
				Log.e(TAG, "error deleting group", e);
			}
		}	
	}
	
	/**
	 * Deletes the group from the database given its label.
	 *  
	 * @param label
	 */
	public void deleteGroup(String label) {
		try {
			mDatabaseOpenHelper.getWritableDatabase().delete(
					DB_TABLE_GROUP, COL_GROUPS_LABEL + "='" + label + "'", null);
		} catch (Exception e) {
			Log.e(TAG, "error deleting group", e);
		}
	}
	
	/**
	 * Updates the group with given {@code label} to {@code newGroup}.
	 * 
	 * @param label
	 * @param newGroup
	 */
	public void updateGroup(String label, Group newGroup) {
		try {
			mDatabaseOpenHelper.getWritableDatabase().update(DB_TABLE_GROUP, buildContentValues(newGroup),
					COL_GROUPS_LABEL + "='" + label + "'", null);
		} catch (Exception e) {
			Log.e(TAG, "error updating group", e);
		}
	}

	/**
	 * Updates the group at given {@code rowId} to {@code newGroup}.
	 * 
	 * @param label
	 * @param newGroup
	 */
	public void updateGroup(long rowId, Group newGroup) {
		try {
			mDatabaseOpenHelper.getWritableDatabase().update(DB_TABLE_GROUP, buildContentValues(newGroup),
					COL_GROUPS_ID + "=?", new String[] { String.valueOf(rowId) });
		} catch (Exception e) {
			Log.e(TAG, "error updating group", e);
		}
	}
	
	/**
	 * Looks up for and returns a {@link Group} with given {@code name}.
	 * 
	 * @param name the name to be looked for
	 * 
	 * @return a {@link Group}, null if not found
	 */
	public Group lookUpGroupByName(String name) {
		Cursor c = null;
		try {
			c = mDatabaseOpenHelper.getReadableDatabase().query(false, DB_TABLE_GROUP, projection_groups,
			    COL_GROUPS_LABEL + "='" + name + "'", null, null, null, null,
			    null);
			if (c.moveToNext()) {
				return new Group(c.getLong(0), c.getString(1), c.getInt(2));
			}
		} catch (Exception e) {
			Log.e(TAG, "error querying data", e);
		} finally {
			if (c != null && !c.isClosed()) {
				// remember to close the cursor
				c.close();
			}
		}
		
		return null;
	}

	/**
	 * Looks up for and returns all ids of groups whose label contains {@code query}.
	 * 
	 * @param query
	 * 
	 * @return a list of group ids. If no match occurs, an empty list is returned.
	 */
	public List<Long> queryContains(String query) {
		ArrayList<Long> groupIds = new ArrayList<Long>();
		Cursor c = null;
		try {
			c = mDatabaseOpenHelper.getReadableDatabase().query(false, DB_TABLE_GROUP,
					new String [] { COL_GROUPS_ID },
			    COL_GROUPS_LABEL + " LIKE '%" + query + "%'", null, null, null, null,
			    null);
			while (c.moveToNext()) {
				groupIds.add((long) c.getInt(0));
			}
		} catch (Exception e) {
			Log.e(TAG, "error querying data", e);
		} finally {
			if (c != null && !c.isClosed()) {
				c.close();
			}
		}
		
		return groupIds;
	}
	
	/**
	 * Looks up for and returns the {@link Group} with given id.
	 * 
	 * @param id id of the group to be looked for
	 * 
	 * @return a {@link Group}, null if not found
	 */
	public Group lookUpByGroupId(long id) {
		Cursor c = null;
		try {
			c = mDatabaseOpenHelper.getReadableDatabase().query(false, DB_TABLE_GROUP, projection_groups,
			    COL_GROUPS_ID + "=" + id + "", null, null, null, null,
			    null);
			if (c.moveToNext()) {
				return new Group(c.getLong(0), c.getString(1), c.getInt(2));
			}
		} catch (Exception e) {
			Log.e(TAG, "error querying data", e);
		} finally {
			if (c != null && !c.isClosed()) {
				c.close();
			}
		}
		
		return null;
	}
	
	/**
	 * Returns all groups from the database.
	 */
	public List<Group> getAllGroups() {
		List<Group> groups = new ArrayList<Group>();
		Cursor c = null;
		try {
			c = mDatabaseOpenHelper.getWritableDatabase().query(false, DB_TABLE_GROUP, projection_groups,
					null, null, null, null, null, null);
			while (c.moveToNext()) {
				groups.add(new Group(c.getLong(0), c.getString(1), c.getInt(2)));
			}
		} catch (Exception e) {
			Log.e(TAG, "error querying data", e);
		} finally {
			if (c != null && !c.isClosed()) {
				c.close();
			}
		}
		
		return groups;
	}

	private static ContentValues buildContentValues(Group group) {
		ContentValues values = new ContentValues();
		values.put(COL_GROUPS_LABEL, group.getLabel());
		values.put(COL_GROUPS_COLOR, group.getColor());
		return values;
	}
	
	private static ContentValues buildContentValuesForTag(String tag,
			String frequency, String coOccurings) {
		ContentValues values = new ContentValues();
		values.put(COL_TAGS_TAG, tag);
		values.put(COL_TAGS_FREQUENCY, frequency);
		values.put(COL_TAGS_COOCURING, coOccurings);
		return values;
	}
	
	/**
	 * Returns true if database for tag was created so that tag suggestion is available.
	 */
	public synchronized static boolean isTagSuggestionReady() {
		return tagSuggestionReady;
	}
	
	/**
	 * Inserts a tag into database.
	 * 
	 * <p> If this tag is not in the vocabulary, if will be inserted anew; otherwise the old tag
	 * will be replaced by this tag.
	 * 
	 * @param tag
	 * @param frequency
	 * @param coOccurings
	 */
	public void insertTag(String tag, String frequency, String coOccurings) {
		try {
			if (getTag(tag) == null) {
				mDatabaseOpenHelper.getWritableDatabase().insert(DB_TABLE_TAGS, null,
						buildContentValuesForTag(tag, frequency, coOccurings));
			} else {
				mDatabaseOpenHelper.getWritableDatabase().update(DB_TABLE_TAGS,
						buildContentValuesForTag(tag, frequency, coOccurings),
						COL_TAGS_TAG + " MATCH ?", new String[] { tag });
			}
		} catch (Exception e) {
			Log.e(TAG, "error inserting tag", e);
		}
	}

	/**
	 * Inserts {@code tag} into database.
	 * 
	 * <p> If this tag is not in the vocabulary, if will be inserted anew; otherwise the old tag
	 * will be replaced by this tag.
	 * 
	 * @param tag
	 */
	public void insertTag(Tag tag) {
		insertTag(tag.getTag(), String.valueOf(tag.getFrequency()), tag.getCoOccuringString());
	}
	
	/**
	 * Gets the {@link Tag} with value {@code tag}.
	 * 
	 * @param tag
	 * @return a {@link Tag} if it exists in database, null otherwise
	 */
	public Tag getTag(String tag) {
		Tag res = null;
		try {
			Cursor c = mDatabaseOpenHelper.getReadableDatabase().query(DB_TABLE_TAGS, projection_tags,
					COL_TAGS_TAG + " MATCH ?", new String[] { tag }, null, null, null);
			if (c.moveToNext()) {
				res = new Tag(c.getString(0), Integer.parseInt(c.getString(1)), c.getString(2));
			}
			c.close();
		} catch (Exception e) {
			Log.e(TAG, "error readding tag", e);
		}
		
		return res;
	}
	
	/**
	 * Returns the list of {@link FrequentTag} in decreasing order of times that a tag was used.
	 * 
	 * @return
	 */
	public ArrayList<FrequentTag> getFrequentTags() {
		ArrayList<FrequentTag> frequentTags = new ArrayList<FrequentTag>();
		try {
			Cursor c = mDatabaseOpenHelper.getReadableDatabase().query(DB_TABLE_FREQUENT_TAGS,
					projection_frequent, null, null, null, null, COL_FREQUENT_TIMES + " DESC");
			while (c.moveToNext()) {
				frequentTags.add(new FrequentTag(c.getString(0), c.getInt(1)));
			}
			c.close();
			// get additional data from the TAGS table because we need this for computing suggestion
			Tag tag;
			for (FrequentTag frequentTag : frequentTags) {
				tag = getTag(frequentTag.getTag());
				if (tag != null) {
					frequentTag.setFrequency(tag.getFrequency());
					frequentTag.setCoOccuring(tag.getCoOccuring());
				}
			}
			close();
		} catch (Exception e) {
			Log.e(TAG, "error getting frequent tags", e);
		}
		
		return frequentTags;
	}
	
	/**
	 * Gets a {@link FrequentTag} with tag value given by {@code tag}.
	 * 
	 * @param tag
	 * @return
	 * 			a {@link FrequentTag}, null if it does not exist
	 */
	public FrequentTag getFrequentTag(String tag) {
		FrequentTag retVal = null;
		try {
			Cursor c = mDatabaseOpenHelper.getReadableDatabase().query(DB_TABLE_FREQUENT_TAGS,
					projection_frequent, COL_FREQUENT_TAG + "=?", new String[] { tag }, null, null, null);
			if (c.moveToNext()) {
				retVal = new FrequentTag(c.getString(0), c.getInt(1));
			}
			c.close();
		} catch (Exception e) {
			Log.e(TAG, "error getting frequent tag", e);
		}
		
		return retVal;
	}
	
	/**
	 * Inserts {@code frequentTag} into database.
	 * 
	 * <p> If this tag is not in the database of frequent tag, if will be inserted anew; otherwise
	 * the old tag will be replaced by this tag.
	 * 
	 * @param frequentTag
	 */
	public void insertFrequentTag(FrequentTag frequentTag) {
		ContentValues values = new ContentValues();
		values.put(COL_FREQUENT_TAG, frequentTag.getTag());
		values.put(COL_FREQUENT_TIMES, frequentTag.getTimes());

		try {
			if (getFrequentTag(frequentTag.getTag()) == null) {
				mDatabaseOpenHelper.getWritableDatabase().insert(DB_TABLE_FREQUENT_TAGS, null, values);
			} else {
				mDatabaseOpenHelper.getWritableDatabase().update(DB_TABLE_FREQUENT_TAGS, values,
						COL_FREQUENT_TAG + "=?", new String[] { frequentTag.getTag() });
			}
		} catch (Exception e) {
			Log.e(TAG, "error inserting frequent tag", e);
		}
	}
	
	/**
	 * Inserts {@code query} into the database.
	 * 
	 * <p> If {@code query} already exists in the database, its frequency will
	 * be increased by 1 instead of an insert.
	 * 
	 * @param query
	 */
	public void insertQuery(String query) {
		try {
			Cursor c = mDatabaseOpenHelper.getReadableDatabase().query(
					DB_TABLE_SEARCH,
					projection_search,
					COL_SEARCH_QUERY + "=?",
					new String[] { query },
					null,
					null,
					null
			);
			ContentValues values = new ContentValues();
			values.put(COL_SEARCH_QUERY, query);
			if (c.moveToFirst()) {
				// update the search query
				values.put(COL_SEARCH_TIMES, c.getInt(1) + 1);
				mDatabaseOpenHelper.getWritableDatabase().update(DB_TABLE_SEARCH, values,
						COL_SEARCH_QUERY + "=?", new String[] { query });
			} else {
				values.put(COL_SEARCH_TIMES, 0);
				mDatabaseOpenHelper.getWritableDatabase().insert(
						DB_TABLE_SEARCH, null, values);
			}
			c.close();
		} catch (Exception e) {
			Log.e(TAG, "error inserting query", e);
		}
	}
	
	/**
	 * Returns at most {@value MAX_QUERY} top search queries on this phone.
	 * 
	 * @return
	 * 			an empty list if no search query is recorded yet
	 */
	public List<String> getTopSearchQueries() {
		List<String> results = new ArrayList<String>();
		try {
			Cursor c = mDatabaseOpenHelper.getReadableDatabase().query(
					DB_TABLE_SEARCH, projection_search, null, null, null, null,
					COL_SEARCH_TIMES + " DESC"
			);
			int numResults = c.getCount();
			if (numResults > MAX_LOCAL_QUERY) {
				numResults = MAX_LOCAL_QUERY;
			}
			for (int i = 0; i < numResults; i++) {
				c.moveToNext();
				results.add(c.getString(0));
			}
			c.close();
		} catch (Exception e) {
			Log.e(TAG, "error reading search query", e);
		}
		
		return results;
	}
	
	/**
	 * A helper class for creating and opening the database.
	 */
  private static class DBOpenHelper extends SQLiteOpenHelper {
  	
  	private Context mContext;
  	private SQLiteDatabase mDatabase;
  	
  	private static final String DB_CREATE_GROUPS = "CREATE TABLE "
  		+ DB_TABLE_GROUP
  		+ " (" + COL_GROUPS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
  		+ COL_GROUPS_LABEL + " TEXT UNIQUE,"
  		+ COL_GROUPS_COLOR + " INTEGER);";

  	private static final String DB_CREATE_TAGS = "CREATE VIRTUAL TABLE "
  		+ DB_TABLE_TAGS + " USING fts3("
  		+ COL_TAGS_TAG + ","
  		+ COL_TAGS_FREQUENCY + ","
  		+ COL_TAGS_COOCURING + ");";
  	
  	private static final String DB_CREATE_FREQUENTS = "CREATE TABLE "
  		+ DB_TABLE_FREQUENT_TAGS
  		+ " (" + COL_FREQUENT_TAG + " TEXT UNIQUE,"
  		+ COL_FREQUENT_TIMES + " INTEGER);";
  	
  	private static final String DB_CREATE_SEARCH = "CREATE TABLE "
  		+ DB_TABLE_SEARCH
  		+ " (" + COL_SEARCH_QUERY + " TEXT UNIQUE,"
  		+ COL_SEARCH_TIMES + " INTEGER);";
  	
  	public DBOpenHelper(Context context, String dbName, CursorFactory factory,
  			int version) {
  		super(context, dbName, factory, version);
  		mContext = context;
  	}
  
  	/**
  	 * Inserts tags into database.
  	 */
  	private void insertTags() {
      new Thread(new Runnable() {
        public void run() {
	    		try {
	    			InputStream inputStream = mContext.getResources().openRawResource(R.raw.eng_tags);
	    			BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
	    			String line;
	    			while ((line = in.readLine()) != null) {
	    				mDatabase.insert(DB_TABLE_TAGS, null, buildContentValuesForTag(line, in.readLine(),
	    						in.readLine()));
	    			}
	    			in.close();
	    			Log.i(TAG, "finish inserting english tags");
	    			if ("kor".equals(mContext.getResources().getConfiguration()
	    					.locale.getISO3Language())) {
		    			// insert tags for korean users
	    				inputStream = mContext.getResources().openRawResource(R.raw.kor_tags);
		    			in = new BufferedReader(new InputStreamReader(inputStream));
		    			while ((line = in.readLine()) != null) {
		    				mDatabase.insert(DB_TABLE_TAGS, null, buildContentValuesForTag(line,
		    						in.readLine(), in.readLine()));
		    			}
		    			in.close();
		    			Log.i(TAG, "finish inserting korean tags");
	    			}
	    			tagSuggestionReady = true;
	    			
	    		} catch (Exception e) {
	    			e.printStackTrace();
	    		}
        }
      }).start();
  	}
  	
  	@Override
  	public void onCreate(SQLiteDatabase db) {
  		ServerConnector.sendEntityLog(mContext);
  		mDatabase = db;
  		try {
  			createGroupTable();
  			// if re-create database, delete all groups because group tables do not exist
  			ContactsHelper.deleteAllGroups(mContext);
  			createTablesForSuggestion();
  			createSearchTable(db);
  		} catch (Exception e) {
  			Log.e(TAG, DBHelper.CLASSNAME, e);
  		}
  	}

  	/**
  	 * Create table for groups.
  	 */
  	private void createGroupTable() {
			// create table groups
			Log.i(TAG, "creating table groups...");
			mDatabase.execSQL(DB_CREATE_GROUPS);
			mDatabase.insert(DB_TABLE_GROUP, null, buildContentValues(
					new Group(mContext.getString(R.string.family), Group.DEFAULT_COLOR)));
			mDatabase.insert(DB_TABLE_GROUP, null, buildContentValues(
					new Group(mContext.getString(R.string.friend), Group.DEFAULT_COLOR)));
  	}
  	
  	/**
  	 * Creates table for tag suggestions.
  	 *  
  	 * @param db
  	 */
  	private void createTablesForSuggestion() {
			Log.i(TAG, "Creating table frequents...");
			mDatabase.execSQL(DB_CREATE_FREQUENTS);
			Log.i(TAG, "Creating table tags...");
			mDatabase.execSQL(DB_CREATE_TAGS);
			tagSuggestionReady = false;
			insertTags();
  	}

  	/**
  	 * Creates table for search query.
  	 */
  	private void createSearchTable(SQLiteDatabase db) {
  		Log.i(TAG, "Creating table search...");
  		mDatabase = db;
  		mDatabase.execSQL(DB_CREATE_SEARCH);
  	}
  	
  	@Override
  	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
  		createSearchTable(db);
  	}
  }
}
