// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.view.tab;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import edu.kaist.uilab.tagcontacts.R;
import edu.kaist.uilab.tagcontacts.model.BasicContact;

/**
 * Adapter for a list view which only displays name of a contact for each list item.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class NameOnlyContactAdapter extends ArrayAdapter<BasicContact> {

	/**
	 * Constructor
	 */
	public NameOnlyContactAdapter(Context context, List<BasicContact> contacts) {
		super(context, R.layout.name_only_contact, contacts);
	}
	
	/**
	 * Returns a View for each row at given position.
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = super.getView(position, convertView, parent);
		}
		((TextView) convertView).setText(((BasicContact) getItem(position)).getName()); 
		return convertView;
	}
}
