// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.database;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import edu.kaist.uilab.tagcontacts.ContactUtils;
import edu.kaist.uilab.tagcontacts.model.BasicContact;
import edu.kaist.uilab.tagcontacts.model.Contact;
import edu.kaist.uilab.tagcontacts.model.Group;
import edu.kaist.uilab.tagcontacts.model.RawContact;

/**
 * Class for sharing application data among tab activities.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class ApplicationData {

	private static List<Contact> cContacts;
	private static List<BasicContact> cFavoriteContacts;
	private static List<Group> cGroups;
	private static boolean cChanged = false;
	
	/**
	 * Returns true if data has changed from last access time. 
	 */
	public synchronized static boolean isDataChanged() {
		return cChanged;
	}
	
	/**
	 * Sets the changed status of data.
	 * 
	 * @param changed
	 */
	public synchronized static void setDataChanged(boolean changed) {
		cChanged = changed;
	}
	
	/**
	 * Sets the list of {@link Contact}s.
	 * 
	 * <p> The list is to be shared across the application and can be obtained via
	 *  {@link #getContacts()}.
	 *  
	 * @param contacts
	 */
	public static void setContacts(List<Contact> contacts) {
		cContacts = contacts;
		ContactUtils.sort(cContacts);
	}
	
	/**
	 * Returns the list of {@link Contact}s.
	 */
	public static List<Contact> getContacts() {
		return cContacts;
	}
	
	/**
	 * Adds {@code newContact} to the list of {@link Contact}s.
	 * 
	 * <p> This only affects the list of {@link Contact}s because no favorite contact is added.
	 * 
	 * @param newContact
	 */
	public static void addContact(Contact newContact) {
		setDataChanged(true);
		ContactUtils.insert(cContacts, newContact);
	}

	/**
	 * Updates the contact whose id is {@code rawContactId} to the new {@code contact}.
	 * 
	 * <p> This involves update for both the list of {@link Contact}s and the list of
	 *  {@link FavoriteContact}s.
	 *  
	 * @param rawContactId
	 * @param contact
	 */
	public static void updateContact(long rawContactId, Contact contact) {
		setDataChanged(true);
		update(cContacts, rawContactId, contact);
		updateFavoriteContact(rawContactId, contact);
	}

	/**
	 * Remove {@code contact} from the list of {@link Contact}s.
	 * 
	 * <p> If {@code contact} is a favorite contact, it will also be removed from the list of
	 *  {@link FavoriteContact}s.
	 *   
	 * @param contact the contact to remove
	 */
	public static void removeContact(Contact contact) {
		boolean retVal = cContacts.remove(contact); 
		removeFavoriteContact(contact);
		setDataChanged(retVal);
	}

	/**
	 * Remove contact with {@code rawContactId} from the list of {@link Contact}s.
	 * 
	 * <p> If {@code contact} is a favorite contact, it will also be removed from the list of
	 *  {@link FavoriteContact}s.
	 *   
	 * @param contact the contact to remove
	 */
	public static void removeContact(long rawContactId) {
		int pos = 0;
		for (; pos < cContacts.size(); pos++) {
			if (cContacts.get(pos).getRawContactId() == rawContactId) {
				break;
			}
		}
		if (pos < cContacts.size()) {
			Contact contact = cContacts.get(pos);
			cContacts.remove(pos);
			removeFavoriteContact(contact);
			setDataChanged(true);
		}
	}
	
	/**
	 * Sets the list of {@link Group}s.
	 * 
	 * <p> The list is to be shared across the application and can be obtained via
	 *  {@link #getGroups()}.
	 * 
	 * @param groups
	 */
	public static void setGroups(List<Group> groups) {
		cGroups = groups;
	}
	
	/**
	 * Returns the list of {@link Group}s.
	 * 
	 * @return
	 */
	public static List<Group> getGroups() {
		return cGroups;
	}

	/**
	 * Sets the list of favorite contacts.
	 * 
	 * <p> The list is to be shared across the application and can be obtained via
	 * {@link #getFavoriteContacts()}.
	 * 
	 * @param contacts
	 */
	public static void setFavoriteContacts(List<BasicContact> contacts) {
		cFavoriteContacts = contacts;
	}
	
	/**
	 * Gets the list of favorite contacts.
	 * 
	 * @return
	 */
	public static List<BasicContact> getFavoriteContacts() {
		return cFavoriteContacts;
	}
	
	/**
	 * Adds {@code contact} to the list of favorite contacts.
	 * 
	 * @param contact
	 */
	public static void addFavoriteContact(BasicContact contact) {
		ContactUtils.insert(cFavoriteContacts, contact);
	}
	
	/**
	 * Updates the contact whose id is {@code rawContactId} to {@code contact}.
	 * 
	 * @param rawContactId
	 * @param contact
	 */
	private static void updateFavoriteContact(long rawContactId, BasicContact contact) {
		update(cFavoriteContacts, rawContactId, contact);
	}
	
	/**
	 * Removes {@code contact} from the list of favorite contacts.
	 * @param contact
	 */
	public static void removeFavoriteContact(BasicContact contact) {
		cFavoriteContacts.remove(contact);
	}
	
	/**
	 * Helper method for updating a contact.
	 * 
	 * @param <T>
	 * @param contacts
	 * @param rawContactId
	 * @param contact
	 */
	private static <T extends RawContact> void update(List<T> contacts,
			long rawContactId, T contact) {
		int pos = 0;
		int size = contacts.size();
		for (; pos < size; pos++) {
			if (contacts.get(pos).getRawContactId() == rawContactId) {
				break;
			}
		}
		if (pos < size) {
			contacts.remove(pos);
			contacts.add(pos, contact);
		}
	}

	/**
	 * Gets the corresponding list of {@link Contact}s for the given list of contact ids.
	 * 
	 * @return
	 */
	public static List<BasicContact> getBasicContacts(HashSet<Long> rawContactIds) {
		List<BasicContact> contacts = new ArrayList<BasicContact>();
		for (Contact c : cContacts) {
			if (rawContactIds.contains(c.getRawContactId())) {
				contacts.add(c);
			}
		}
		
		return contacts;
	}
}
