// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.view.tab;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import edu.kaist.uilab.tagcontacts.ContactUtils;
import edu.kaist.uilab.tagcontacts.R;
import edu.kaist.uilab.tagcontacts.model.BasicContact;

/**
 * Adapter which provides view for each favorite contact in the {@link FavoriteView}
 * view.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class FavoriteContactAdapter extends ArrayAdapter<BasicContact> {
	private LayoutInflater mInflater;
	private Context mContext;
	
	/**
	 * Constructor
	 * 
	 * @param context the current context
	 * @param contacts the data that backs this adapter
	 */
	public FavoriteContactAdapter(Context context,
			List<BasicContact> contacts) {
		super(context, R.layout.favorite_contact, contacts);
		mContext = context;
		mInflater = (LayoutInflater) context.getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
	}
	
	/**
	 * Returns a View for each row at given position.
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		FavoriteContactWrapper wrapper = null;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.favorite_contact, null);
			View name = convertView.findViewById(R.id.txt_favorite_name);
			View smsContact = convertView.findViewById(R.id.btn_sms_contact);			
			View callContact = convertView.findViewById(R.id.btn_call_contact);
			wrapper = new FavoriteContactWrapper(name, smsContact, callContact);
			convertView.setTag(wrapper);
		} else {
			wrapper = (FavoriteContactWrapper) convertView.getTag();
		}

		// get the current contact
		final BasicContact contact = getItem(position);
		wrapper.getName().setText(contact.getName());
		wrapper.getCallContact().setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ContactUtils.callContact(mContext, contact);
			}
		});
		wrapper.getSmsContact().setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ContactUtils.smsContact(mContext, contact);
			}
		});
		
		return convertView;
	}
	
	/**
	 * Wrapper for the views of a favorite contact.
	 */
	private static class FavoriteContactWrapper {
		private TextView mName;
		private ImageView mSmsContact;
		private ImageView mCallContact;
		
		public FavoriteContactWrapper(View name, View smsContact, View callContact) {
			mName = (TextView) name;
			mSmsContact = (ImageView) smsContact;
			mCallContact = (ImageView) callContact;
		}
		
		public TextView getName() {
			return mName;
		}
		
		public ImageView getSmsContact() {
			return mSmsContact;
		}
		
		public ImageView getCallContact() {
			return mCallContact;
		}
	}
}
