// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContactsEntity;
import edu.kaist.uilab.tagcontacts.ContactUtils;
import edu.kaist.uilab.tagcontacts.model.AddressEntity;
import edu.kaist.uilab.tagcontacts.model.BasicContact;
import edu.kaist.uilab.tagcontacts.model.Contact;
import edu.kaist.uilab.tagcontacts.model.EmailEntity;
import edu.kaist.uilab.tagcontacts.model.Entity;
import edu.kaist.uilab.tagcontacts.model.GroupEntity;
import edu.kaist.uilab.tagcontacts.model.JobEntity;
import edu.kaist.uilab.tagcontacts.model.NameEntity;
import edu.kaist.uilab.tagcontacts.model.NicknameEntity;
import edu.kaist.uilab.tagcontacts.model.NoteEntity;
import edu.kaist.uilab.tagcontacts.model.PhoneEntity;
import edu.kaist.uilab.tagcontacts.model.TagEntity;
import edu.kaist.uilab.tagcontacts.model.WebsiteEntity;

/**
 * Class for dealing with contacts data.
 * 
 * <p> All actions related to data like reading or writing should be done via
 * this class.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public final class ContactsHelper {

	/**
	 * Convenient method for getting a contact from {@code uri}.
	 * 
	 * @param context
	 * @param uri
	 * @return
	 */
	public static Contact getContact(Context context, Uri uri) {
		long rawContactId = -1L;
		Cursor c = context.getContentResolver().query(uri,
				new String[] { RawContacts._ID }, null, null, null);
		if (c.moveToNext()) {
			rawContactId = c.getLong(0);
			c.close();
			c = context.getContentResolver().query(RawContactsEntity.CONTENT_URI,
		  			getProjectionForContact(),
		  			getSelectionForContact(),
		  			new String[] {String.valueOf(rawContactId)},
		  			null);
			return getContact(context, rawContactId, c);
		}
		return null;
	}
	
	/**
	 * Returns selection for data which constituted a contact.
	 */
	private static String getSelectionForContact() {
		return RawContactsEntity._ID + "=?" + " AND " 
				+ RawContactsEntity.DELETED + "='0'" + " AND "
				+ Data.MIMETYPE + " IN ("
				+ "'" + NameEntity.MIMETYPE + "',"
				+ "'" + TagEntity.MIMETYPE + "',"
				+ "'" + GroupEntity.MIMETYPE + "',"
				+ "'" + PhoneEntity.MIMETYPE + "'"
				+ ")"; 
	}

	/**
	 * Returns projection for data which constituted a contact.
	 */
	private static String[] getProjectionForContact() {
		return new String[] { RawContactsEntity.DATA_ID,
				RawContactsEntity._ID, RawContactsEntity.MIMETYPE,
				RawContactsEntity.DATA1, RawContactsEntity.DATA2, RawContactsEntity.DATA3,
				RawContactsEntity.DATA4, RawContactsEntity.DATA5, RawContactsEntity.DATA6};
	}
	
	/**
	 * Gets all contacts.
	 * 
	 * @return
	 */
	public static ArrayList<Contact> getContacts(Context context) {
 		Cursor c = context.getContentResolver().query(RawContacts.CONTENT_URI,
				new String[] { RawContacts._ID, RawContacts.CONTACT_ID },
				RawContacts.DELETED + "='0'",
				null,
				null);
		HashMap<Long, Long> map = new HashMap<Long, Long>();
		while (c.moveToNext()) {
			long contactId = c.getLong(1);
			if (map.get(contactId) == null) { // only get one raw contact id for each contact id
				map.put(contactId, c.getLong(0));
			}
		}
		c.close();
		// read all contacts from RawContactsEntity table
		ArrayList<Contact> res = new ArrayList<Contact>();
		Collection<Long> rawContactIds = map.values();
		Contact contact;
		for (Long rawId : rawContactIds) {
			c = context.getContentResolver().query(RawContactsEntity.CONTENT_URI,
	  			getProjectionForContact(), getSelectionForContact(),
	  			new String[] { String.valueOf(rawId) },
	  			null);
			contact = getContact(context, rawId, c);
			if (contact != null) {
				res.add(contact); 
			}
		}
		
		return res;
	}
	
	/**
	 * Returns a contact whose raw id is {@code rawContactId}.
	 * 
	 * @return
	 * 			a {@link Contact}, null if no such contact exists
	 */
	public static Contact getContact(Context context, long rawContactId) {
		Cursor c = context.getContentResolver().query(RawContactsEntity.CONTENT_URI,
	  			getProjectionForContact(),
	  			getSelectionForContact(),
	  			new String[] {String.valueOf(rawContactId)},
	  			null);

		return getContact(context, rawContactId, c);
	}
	
	/**
	 * Returns a contact from the cursor c.
	 * 
	 * <p> The cursor will be handled completely by this method, i.e., this method is responsible for
	 * moving cursor to the first position as well as closing the cursor.
	 * 
	 * @param c cursor which must be the result of a query against the RawContactsEntity table
	 */
	private static Contact getContact(Context context, long rawContactId, Cursor c) {
		String mime;
		Contact contact = new Contact(rawContactId, "");
		while (c.moveToNext()) {
			if (!c.isNull(0)) {
	  		// determine the data type and get corresponding data
	  		mime = c.getString(2);
	  		if (NameEntity.MIMETYPE.equals(mime)) {
	  		  // 3 - data1, 4 - data2, 5 - data3, 6 - data4, 7 - data5, 8 - data6
	  			contact.setName(ContactUtils.concatenateStrings(" ", c.getString(4),
	  		  		c.getString(5), null, null));
	  		} else if (TagEntity.MIMETYPE.equals(mime)) {
	  			contact.addTag(c.getString(3));
	  		} else if (GroupEntity.MIMETYPE.equals(mime)) {
	  			contact.addGroup(new GroupEntity(rawContactId, c.getLong(3)));
	  		} else {
	  			// mime is of type PhoneEntity
	  			contact.addPhone(new PhoneEntity(rawContactId, c.getString(3)));
	  		}
			}	
  	}
		c.close();
		
		if (contact.getName().length() > 0) {
			return contact;
		} else {
			return null;
		}	
	}
	
	/**
	 * Returns true if the contact with id {@code rawContactId} is a favorite contact.
	 * 
	 * @param rawContactId
	 * @return
	 */
	public static boolean isStarred(Context context, long rawContactId) {
		// get starred (favorite) for each contact
		Cursor c = context.getContentResolver().query(RawContacts.CONTENT_URI,
				new String[] { RawContacts.STARRED },
				RawContacts._ID + "=?",
				new String [] { String.valueOf(rawContactId)}, null);
		int starred = 0;
		if (c.moveToNext()) {
			starred = c.getInt(0);
		}
		c.close();
		
		return (starred == 1);
	}
	
	/**
	 * Sets the contact with id {@code rawContactId} according to the value of {@code starred}.
	 * 
	 * @param rawContactId id of the contact
	 * @param starred whether this contact should be set to starred (favorite) or not
	 */
	public static void setStarred(Context context, long rawContactId,
			boolean starred) {
		ContentValues values = new ContentValues();
		int favorite = 1;
		if (!starred) {
			favorite = 0;
		}
		values.put(RawContacts.STARRED, favorite);
		context.getContentResolver().update(RawContacts.CONTENT_URI, values, RawContacts._ID + "=?",
				new String [] { String.valueOf(rawContactId)}); 
	}
	
	/**
	 * Returns raw contact ids of all contacts that matches the given {@code query}.
	 * Comparison is case-insensitive.
	 * 
	 * <p> A contact matches a query if one of the following is true:
	 * <br> 1. The query matches one of its first, middle or last name.
	 * <br> 2. The query matches one of its tags.
	 * </p>
	 * 
	 * @param query the search query
	 * 
	 * @return
	 * 			a set of raw contact ids which will be empty if no contact is matched
	 */
	public static HashSet<Long> getRawContactIds(Context context, String query) {
		HashSet<Long> rawContactIds = new HashSet<Long>();
		ArrayList<Long> contactIds = new ArrayList<Long>();
		String[] projection = null;
		String selection = null;
		
		// search for name first (result is a list of contact ids)
		projection = new String [] { Contacts._ID };
		Cursor c = context.getContentResolver().query(
				Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, query),
				projection, null, null, null);
		while (c.moveToNext()) {
			contactIds.add(c.getLong(0));
		}
		c.close();
		
		// get the corresponding list of raw contact ids from RawContacts table
		projection = new String [] { RawContacts._ID };
		selection = RawContacts.CONTACT_ID + "=?";
		for (Long contactId : contactIds) {
			c = context.getContentResolver().query(RawContacts.CONTENT_URI,
					projection, selection, new String[] { String.valueOf(contactId) },
					null);
			if (c.moveToFirst()) {
				rawContactIds.add(c.getLong(0));
			}
			c.close();
		}

		projection = new String [] { Data.RAW_CONTACT_ID };
		selection = Data.MIMETYPE + "='" + TagEntity.MIMETYPE + "'" + " AND "
				+ TagEntity.LABEL + " LIKE ?";
		// select data.raw_contact_id from data
		// where mimetype = 'tagentity' and tag.data like '%query%'
		c = context.getContentResolver().query(Data.CONTENT_URI, projection,
				selection, new String[] { String.valueOf("%" + query + "%")}, null);
		while (c.moveToNext()) {
			rawContactIds.add(c.getLong(0));
		}
		c.close();
		
		return rawContactIds;
	}

	/**
	 * Returns the list of ids of all contacts that contains given {@code tag}.
	 * 
	 * @param tag the tag
	 * @return
	 * 			the list of all ids, an empty list if no such contact is found
	 */
	public static HashSet<Long> getRawContactIdsGivenTag(Context context,
			String tag) {
		HashSet<Long> rawContactIds = new HashSet<Long>();
		String[] projection = new String [] { Data.RAW_CONTACT_ID };
		String selection = Data.MIMETYPE + "='" + TagEntity.MIMETYPE + "'" + " AND "
				+ TagEntity.LABEL + "=?";
		Cursor c = context.getContentResolver().query(Data.CONTENT_URI, projection,
				selection, new String[] { String.valueOf(tag) }, null);
		while (c.moveToNext()) {
				rawContactIds.add(c.getLong(0));
		}
		c.close();
		
		return rawContactIds;
	}
	
	/**
	 * Returns contact ids of all contact that belong to one of the groups
	 * whose id is in the given list of {@code groupIds}.
	 * 
	 * @param groupIds a list of group ids
	 * 
	 * @return
	 * 			a set of raw contact ids
	 */
	public static HashSet<Long> getRawContactIds(Context context,
			List<Long> groupIds) {
		HashSet<Long> rawContactIds = new HashSet<Long>();
		String[] projection = new String [] { Data.RAW_CONTACT_ID };
		String selection = Data.MIMETYPE + "='" + GroupEntity.MIMETYPE + "'"
				+ " AND " + GroupEntity.ID + "=?";
		Cursor c = null;
		for (Long groupId : groupIds) {
			c = context.getContentResolver().query(Data.CONTENT_URI,
					projection, selection, new String[] { String.valueOf(groupId) },
					null);
			while (c.moveToNext()) {
				rawContactIds.add(c.getLong(0));
			}
			c.close();
		}
		
		return rawContactIds;
	}
	
	/**
	 * Returns all entities associated with the contact whose raw id is {@code rawContactId}.
	 * 
	 * @param rawContactId
	 * @return
	 */
	public static HashMap<String, List<Entity>> getEntitiesForContact(
			Context context, long rawContactId) {
		// we only need maximum of 10 data
		String[] projection = new String[] { RawContactsEntity.DATA_ID,
				RawContactsEntity._ID, RawContactsEntity.MIMETYPE,
				RawContactsEntity.DATA1, RawContactsEntity.DATA2, RawContactsEntity.DATA3,
				RawContactsEntity.DATA4, RawContactsEntity.DATA5, RawContactsEntity.DATA6,
				RawContactsEntity.DATA7, RawContactsEntity.DATA8, RawContactsEntity.DATA9,
				RawContactsEntity.DATA10,
				};
		String selection = RawContactsEntity._ID + "=?" + " AND "
				+ RawContactsEntity.DELETED + "='0'";;
		
  	// execute query
		Cursor c = context.getContentResolver().query(
  			RawContactsEntity.CONTENT_URI,
  			projection,
  			selection,
  			new String[] { String.valueOf(rawContactId) } ,
  			null);

		HashMap<String, List<Entity>> map = new HashMap<String, List<Entity>>();
		map.put(AddressEntity.MIMETYPE, new ArrayList<Entity>(5));
		map.put(EmailEntity.MIMETYPE, new ArrayList<Entity>(5));
		map.put(GroupEntity.MIMETYPE, new ArrayList<Entity>(5));
		map.put(JobEntity.MIMETYPE, new ArrayList<Entity>(5));
		map.put(NameEntity.MIMETYPE, new ArrayList<Entity>(5));
		map.put(NicknameEntity.MIMETYPE, new ArrayList<Entity>(5));
		map.put(NoteEntity.MIMETYPE, new ArrayList<Entity>(5));
		map.put(PhoneEntity.MIMETYPE, new ArrayList<Entity>(5));
		map.put(TagEntity.MIMETYPE, new ArrayList<Entity>(5));
		map.put(WebsiteEntity.MIMETYPE, new ArrayList<Entity>(5));
		
		List<Entity> list;
		String mime;
		
  	while (c.moveToNext()) {
  		if (!c.isNull(0)) {
  			 mime = c.getString(2);
  			 list = map.get(mime);
  			 Entity entity = null;
  			 // data1 = 3, data2 = 4; data(i) = i + 2
  			 if (AddressEntity.MIMETYPE.equals(mime)) {
  				 entity = new AddressEntity(rawContactId, c.getString(6), c.getString(9),
  						 c.getString(10), c.getString(11));
  			 } else if (EmailEntity.MIMETYPE.equals(mime)) {
  				 entity = new EmailEntity(rawContactId, c.getString(3));
  			 } else if (GroupEntity.MIMETYPE.equals(mime)) {
  				 entity = new GroupEntity(rawContactId, c.getLong(3));
  			 } else if (JobEntity.MIMETYPE.equals(mime)) {
  				 entity = new JobEntity(rawContactId, c.getString(3), c.getString(6),
  						 c.getString(7));
  			 } else if (NameEntity.MIMETYPE.equals(mime)) {
  				 entity = new NameEntity(rawContactId, c.getString(4), c.getString(7),
  						 c.getString(5));
  			 } else if (NicknameEntity.MIMETYPE.equals(mime)) {
  				 entity = new NicknameEntity(rawContactId, c.getString(3));
  			 } else if (NoteEntity.MIMETYPE.equals(mime)) {
  				 entity = new NoteEntity(rawContactId, c.getString(3));
  			 } else if (PhoneEntity.MIMETYPE.equals(mime)) {
  				 entity = new PhoneEntity(rawContactId, c.getString(3));
  			 } else if (TagEntity.MIMETYPE.equals(mime)) {
  				 entity = new TagEntity(rawContactId, c.getString(3));
  			 } else if (WebsiteEntity.MIMETYPE.equals(mime)) {
  				 entity = new WebsiteEntity(rawContactId, c.getString(3));
  			 }
				 if (entity != null && entity.toString().length() > 0) {
					 list.add(entity);
				 }
  		}
  	}
		c.close();
		
		return map; 
	}
	
	/**
	 * Gets all favorite contacts.
	 * 
	 * @return
	 */
	public static List<BasicContact> getFavoriteContacts(Context context) {
		Cursor c = context.getContentResolver().query(
  			RawContacts.CONTENT_URI,
  			new String[] { RawContacts._ID, RawContacts.CONTACT_ID },
  			RawContacts.STARRED + "='1'" + " AND " + RawContacts.DELETED + "='0'",
  			null,
  			null);
		HashSet<Long> set = new HashSet<Long>();
		List<Long> rawIds = new ArrayList<Long>();
		while (c.moveToNext()) {
			long contactId = c.getLong(1);
			if (!set.contains(contactId)) { // only get one raw contact id for each contact id
				set.add(contactId);
				rawIds.add(c.getLong(0));
			}
		}
		c.close();

		return getBasicContacts(context, rawIds);
	}

	/**
	 * Gets the list of up to {@code max} most frequent contacts. Only contacts
	 * that have been contacted more than once will be returned.
	 * 
	 * @return
	 */
	public static List<BasicContact> getFrequentContacts(Context context, int max) {
		Cursor c = context.getContentResolver().query(RawContacts.CONTENT_URI,
				new String[] { RawContacts._ID, RawContacts.TIMES_CONTACTED, RawContacts.CONTACT_ID },
				RawContacts.DELETED + "='0'",
  			null,
  			RawContacts.TIMES_CONTACTED + " DESC");
		HashSet<Long> set = new HashSet<Long>();
		List<Long> rawIds = new ArrayList<Long>();
		ArrayList<BasicContact> contacts = new ArrayList<BasicContact>();
		long contactId;
		while (c.moveToNext()) {
			contactId = c.getLong(2);
			if (set.add(contactId)) {
				if (c.getInt(1) == 0 || contacts.size() == max) {
					break;
				}
				rawIds.add(c.getLong(0));
			}	
		}
		c.close();

		return getBasicContacts(context, rawIds);
	}

	/**
	 * Gets the list of {@link BasicContact}s for the list of raw contact ids {@code rawIds}.
	 * 
	 * @param rawIds
	 * @return
	 */
	private static List<BasicContact> getBasicContacts(Context context,
			List<Long> rawIds) {
		ArrayList<BasicContact> results = new ArrayList<BasicContact>();
		for (Long rawId : rawIds) {
			BasicContact contact = getBasicContact(context, rawId);
			if (contact.getName().length() > 0) {
				results.add(contact);
			}
		}
		
		return results;
	}
	
	/**
	 * Gets content of the {@link BasicContact} with {@code rawContactId}.
	 * 
	 * @param contact
	 */
	private static BasicContact getBasicContact(Context context, long rawContactId) {
		BasicContact contact = new BasicContact(rawContactId, "");
		Cursor c = context.getContentResolver().query(Data.CONTENT_URI,
				new String[] { NameEntity.GIVEN_NAME, NameEntity.FAMILY_NAME },
				Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='" + NameEntity.MIMETYPE + "'",
				new String[] { String.valueOf(rawContactId) },
				null);
		// read name entity
		if (c.moveToNext()) {
			contact.setName(ContactUtils.concatenateStrings(" ", c.getString(0),
					 c.getString(1), null, null));
		}	
		c.close();
		// read phone entities
		c = context.getContentResolver().query(Data.CONTENT_URI,
					new String[] { PhoneEntity.NUMBER },
					Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
							+ PhoneEntity.MIMETYPE + "'",
					new String[] { String.valueOf(rawContactId) },
					null);
		while (c.moveToNext()) {
			contact.addPhone(new PhoneEntity(rawContactId, c.getString(0)));
		}
		c.close();
		
		return contact;
	}
	
	/**
	 * Creates a delete operation for the given {@code entity}.
	 * 
	 * @param entity
	 * @return
	 */
	public static ContentProviderOperation createDeleteOp(Entity entity) {
		return ContentProviderOperation.newDelete(Data.CONTENT_URI)
				.withSelection(entity.getSelectionForChange(),
						entity.getSelectionArgsForChange())
				.build();
	}
	
	/**
	 * Creates a update operation for the given {@code entity} with the updating
	 * values provided by {@code newEntity}.
	 * 
	 * @param entity
	 * @param newEntity
	 * @return
	 */
	public static ContentProviderOperation createUpdateOp(Entity entity,
			Entity newEntity) {
		return ContentProviderOperation.newUpdate(Data.CONTENT_URI)
				.withSelection(entity.getSelectionForChange(),
						entity.getSelectionArgsForChange())
				.withValues(newEntity.buildContentValuesWithNoId())
				.build();
	}
	
	public static ContentProviderOperation createInsertOp(Entity entity) {
		return ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValues(entity.buildContentValuesWithId())
				.build();
	}
	
	/**
	 * Deletes the contact with given contact id.
	 * 
	 * @param contactId
	 */
	public static void deleteContact(Context context, long contactId) {
		context.getContentResolver().delete(RawContacts.CONTENT_URI,
				RawContacts.CONTACT_ID + "=?",
				new String[] {String.valueOf(contactId)} );
	}
	
	/**
	 * Deletes all data from the RawContacts and Data table.
	 */
	public static void clear(Context context) {
		context.getContentResolver().delete(RawContacts.CONTENT_URI, null, null);
		context.getContentResolver().delete(Data.CONTENT_URI, null, null);
	}
	
	/**
	 * Deletes the contact with given raw contact id.
	 * 
	 * @param rawContactId
	 */
	public static void deleteRawContact(Context context, long rawContactId) {
		context.getContentResolver().delete(RawContacts.CONTENT_URI,
				RawContacts._ID + "=?",
				new String[] {String.valueOf(rawContactId)} );
	}

	/**
	 * Deletes the given entity from the Data table.
	 * 
	 * <p> The entity is deleted using the traditional method rather than the
	 * batch method.
	 * 
	 * @param entity
	 */
	public static void deleteEntity(Context context, Entity entity) {
		context.getContentResolver().delete(Data.CONTENT_URI,
				entity.getSelectionForChange(), entity.getSelectionArgsForChange());
	}
	
	/**
	 * Deletes the group with given {@code groupId} from the Data table.
	 * 
	 * <p> This delete operation is not a single entity delete. It removes all
	 * entity which contains {@code groupId} rather than a specific entity.
	 * 
	 * @param groupId
	 */
	public static void deleteGroup(Context context, long groupId) {
		String selection = Data.MIMETYPE + "='" + GroupEntity.MIMETYPE + "'" 
				+ " AND " + GroupEntity.ID + "=?";
		String[] selectionArgs = new String[] { String.valueOf(groupId) };
		context.getContentResolver().delete(Data.CONTENT_URI, selection, selectionArgs);
	}
	
	/**
	 * Deletes groups for all contacts.
	 */
	public static void deleteAllGroups(Context context) {
		String selection = Data.MIMETYPE + "='" + GroupEntity.MIMETYPE + "'"; 
		context.getContentResolver().delete(Data.CONTENT_URI, selection, null);
	}
	
	/**
	 * Inserts an entity using the traditional method.
	 */
	public static Uri insertEntity(Context context, Entity entity) {
		return context.getContentResolver().insert(Data.CONTENT_URI,
				entity.buildContentValuesWithId());
	}
	
	/**
	 * Gets emails of all given contacts.
	 * 
	 * @param contacts
	 * @return
	 */
	public static <T extends BasicContact> List<String> getEmails(Context context,
			List<T> contacts) {
		ArrayList<String> emails = new ArrayList<String>();
		Cursor c;
		String[] projection = new String[] { EmailEntity.DATA };
		String selection = Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE
				+ "='" + EmailEntity.MIMETYPE + "'";
		for (BasicContact contact : contacts) {
			c = context.getContentResolver().query(Data.CONTENT_URI, projection, selection,
					new String[] { String.valueOf(contact.getRawContactId()) }, null);
			if (c.moveToNext()) {
				emails.add(c.getString(0));
			}
			c.close();
		}
		
		return emails;
	}
}
