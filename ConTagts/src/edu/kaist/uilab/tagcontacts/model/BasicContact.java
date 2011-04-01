// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.model;

import java.util.ArrayList;
import java.util.List;

import edu.kaist.uilab.tagcontacts.ServerConnector;

/**
 * Class for modeling a contact with basic information including id,
 * name, and phones.
 * 
 * <p> This class is designed for efficiency, i.e., the data enclosed is the most
 * frequently needed data only. Other information about a contact is only obtained
 * from the database if user requires. 
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class BasicContact extends RawContact {
	List<PhoneEntity> phones;
	
	/**
	 * Constructs a new instance with given data.
	 * 
	 * @param name
	 * @param tag
	 */
	public BasicContact(long rawContactId, String name) {
		super(rawContactId, name);
		this.phones = new ArrayList<PhoneEntity>();
	}

	public void addPhone(PhoneEntity phone) {
		phones.add(phone);
	}
	
	public List<PhoneEntity> getPhones() {
		return phones;
	}
	
	/**
	 * Returns true if this contact has phone number that matches {@code number}.
	 * 
	 * @param number
	 * @return
	 */
	public boolean hasPhoneNumber(String number) {
		for (PhoneEntity entity : phones) {
			if (ServerConnector.formatPhone(entity.getNumber()).equals(number)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "\nid: " + getRawContactId() + "; Name: " + getName() + "; Phones: " + phones;
	}
}
