// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.view.tab;

import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.kaist.uilab.tagcontacts.Constants;
import edu.kaist.uilab.tagcontacts.R;
import edu.kaist.uilab.tagcontacts.model.Contact;
import edu.kaist.uilab.tagcontacts.model.Group;
import edu.kaist.uilab.tagcontacts.model.GroupEntity;
import edu.kaist.uilab.tagcontacts.view.widget.LabelView;

/**
 * Adapter for a customized list view which provides the view for each contact
 * with the contact name, groups, and tags.
 * 
 * <p> This adapter is intended to be used in the {@link AllContactsView}
 * activity.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class GroupAndTagContactAdapter extends ArrayAdapter<Contact> {
	
	private LayoutInflater mInflater;
	private HashMap<Long, Group> mMap; // a map from group id to group
	
	public GroupAndTagContactAdapter(Context context, List<Contact> contacts,
			List<Group> groups) {
		super(context, R.layout.basic_contact, contacts);
		mInflater = (LayoutInflater) context.getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		mMap = new HashMap<Long, Group>();
		for (Group group : groups) {
			mMap.put(group.getId(), group);
		}
	}
	
	/**
	 * Returns a View for each row at given position.
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ContactWrapper wrapper = null;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.basic_contact,	null);
			wrapper = new ContactWrapper(
					convertView.findViewById(R.id.txt_basic_name),
					convertView.findViewById(R.id.layout_labels_region)
			);
			convertView.setTag(wrapper);
		} else {
			wrapper = (ContactWrapper) convertView.getTag();
		}
		// get the current contact
		Contact contact = getItem(position);
		wrapper.getName().setText(contact.getName());
		LinearLayout labelContainer = wrapper.getHolder();
		labelContainer.removeAllViews(); // clear invalid views first
		// add no more than 5 labels
		int count = 0;
		// add groups for this contact
		List<GroupEntity> groups = contact.getGroups();
		for (GroupEntity entity : groups) {
			labelContainer.addView(inflateLabel(entity));
			count++;
		}
		
		// add tags for this contact
		List<String> tags = contact.getTags();
		for (String tag : tags) {
			TextView tagView = (TextView) mInflater.inflate(R.layout.contact_label,
					null);
			tagView.setText(tag);
			tagView.setOnClickListener(new LabelClickListener());
			labelContainer.addView(tagView);
			count++;
			if (count == 5) {
				break;
			}
		}
		
		return convertView;
	}
	
	/**
	 * Inflates a view for the group entity {@code entity}.
	 *  
	 * @param entity
	 * @return
	 */
	private View inflateLabel(GroupEntity entity) {
		TextView view = (TextView) mInflater.inflate(R.layout.contact_label, null);
		Group group = mMap.get(entity.getId());
		// view for group has id as tag object
		view.setTag(entity.getId());
		view.setText(group.getLabel());
		view.setTextColor(group.getColor());
		view.setOnClickListener(new LabelClickListener());
		
		return view;
	}
	
	/**
	 * Listener for the event of a label being clicked. A label is either a group
	 * or a tag.
	 */
	private static class LabelClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(v.getContext(), LabelView.class);
			if (v.getTag() != null) {
				intent.putExtra(Constants.INTENT_LONG_GROUP_ID, (Long) v.getTag());
				intent.putExtra(Constants.INTENT_TEXT_CONTACT_LABEL, ((TextView) v).getText());
			} else {
				intent.putExtra(Constants.INTENT_TEXT_CONTACT_LABEL, ((TextView) v).getText());
			}
			v.getContext().startActivity(intent);
		}
	}
}