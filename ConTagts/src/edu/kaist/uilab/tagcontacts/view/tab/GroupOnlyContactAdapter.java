// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.view.tab;

import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.kaist.uilab.tagcontacts.R;
import edu.kaist.uilab.tagcontacts.database.ApplicationData;
import edu.kaist.uilab.tagcontacts.database.ContactsHelper;
import edu.kaist.uilab.tagcontacts.model.Contact;
import edu.kaist.uilab.tagcontacts.model.Group;
import edu.kaist.uilab.tagcontacts.model.GroupEntity;

/**
 * Adapter for a customized list view which provides the view for each contact
 * with the contact name and groups.
 * 
 * <p> This adapter is intended to be used in the {@GroupsView} tab.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class GroupOnlyContactAdapter extends ArrayAdapter<Contact> {

	private LayoutInflater mInflater;
	// a map from group id to group
	private HashMap<Long, Group> mMap;
	private Context mContext;
	
	public GroupOnlyContactAdapter(Context context, List<Contact> contacts,
			List<Group> groups) {
		super(context, R.layout.basic_contact, contacts);
		mContext = context;
		mInflater = (LayoutInflater) context.getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		mMap = new HashMap<Long, Group>();
		for (Group group : groups) {
			mMap.put(group.getId(), group);
		}
	}
	
	/**
	 * Returns a View for each row at given position. Since we are not using
	 * the simple TextView, we have to override this method and returns a
	 * customized View inflated from basic_contact.xml layout.
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ContactWrapper wrapper = null;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.basic_contact, null);
			wrapper = new ContactWrapper(
					convertView.findViewById(R.id.txt_basic_name),
					convertView.findViewById(R.id.layout_labels_region)
			);
			convertView.setTag(R.id.wrapperId, wrapper);
			convertView.setTag(position);
		} else {
			wrapper = (ContactWrapper) convertView.getTag(R.id.wrapperId);
		}

		// get the current contact
		final Contact contact = getItem(position);
		wrapper.getName().setText(contact.getName());
		LinearLayout groupRegion = wrapper.getHolder();
		groupRegion.removeAllViews(); // remove invalid views first
		// add groups for this contact
		List<GroupEntity> groups = contact.getGroups();
		for (GroupEntity entity : groups) {
			groupRegion.addView(inflateTextView(groupRegion, contact,
					mMap.get(entity.getId())));
		}
		
		return convertView;
	}
	
	/**
	 * Inflates a text view for representing a group that belongs to a contact.
	 *  
	 * @param groupRegion the region which contains the new view
	 * @param contact the contact which belongs to {@code group}
	 * @param group a group
	 * 
	 * @return
	 * 			a {@link TextView}
	 */
	public TextView inflateTextView(final LinearLayout groupRegion,
			final Contact contact, final Group group) {
		final TextView view = (TextView) mInflater.inflate(
				R.layout.contact_label, null);
		view.setText(group.getLabel());
		view.setTextColor(group.getColor());
		// remove the group label for this contact when click
		view.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				groupRegion.removeView(v);
				groupRegion.invalidate();
				// update data in database and in adapter
				long rawContactId = contact.getRawContactId();
				GroupEntity entity = new GroupEntity(rawContactId, group.getId());
				ContactsHelper.deleteEntity(mContext, entity);
				contact.removeGroup(entity);
				ApplicationData.setDataChanged(true);
				return true;
			}
		});
		
		return view;
	}
}
