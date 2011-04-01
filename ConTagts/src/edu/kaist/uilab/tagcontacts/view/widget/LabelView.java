// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.view.widget;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import edu.kaist.uilab.tagcontacts.Constants;
import edu.kaist.uilab.tagcontacts.ContactUtils;
import edu.kaist.uilab.tagcontacts.R;
import edu.kaist.uilab.tagcontacts.ServerConnector;
import edu.kaist.uilab.tagcontacts.database.ApplicationData;
import edu.kaist.uilab.tagcontacts.database.ContactsHelper;
import edu.kaist.uilab.tagcontacts.model.BasicContact;
import edu.kaist.uilab.tagcontacts.view.tab.NameOnlyContactAdapter;

/**
 * View for displaying a group of contacts for a specific label.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class LabelView extends Activity {

	private ListView mListView;
	private List<BasicContact> mContacts;
	private String mLabel;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.labeled_contacts);
		mLabel = getIntent().getStringExtra(Constants.INTENT_TEXT_CONTACT_LABEL);
		
		// set "cancel" to "back", hide "Save", and set title
		((Button) findViewById(R.id.btn_bar_right)).setVisibility(View.INVISIBLE);
		Button back = (Button) findViewById(R.id.btn_bar_left);
		back.setText(getResources().getString(R.string.back));
		back.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		// set listener for btn "sms all" 
		Button smsAll = (Button) findViewById(R.id.btn_sms_all);
		smsAll.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ServerConnector.sendLabelLog(LabelView.this, "sms_group", mLabel, null);
				ContactUtils.smsContacts(LabelView.this, mContacts);
			}
		});
		
		Button emailAll = (Button) findViewById(R.id.btn_email_all);
		emailAll.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ServerConnector.sendLabelLog(LabelView.this, "email_group", mLabel, null);
				ContactUtils.emailContacts(LabelView.this, ContactsHelper.getEmails(
						LabelView.this, mContacts));
			}
		});
		
		((TextView) findViewById(R.id.top_bar_title)).setText(mLabel);
		mListView = (ListView) findViewById(R.id.list_contacts);
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				Intent intent = new Intent(LabelView.this, DetailedContactView.class);
				intent.putExtra(Constants.INTENT_LONG_RAW_CONTACT_ID,
						((BasicContact) mListView.getAdapter().getItem(position)).getRawContactId());
				startActivity(intent);
			}
		});
		registerForContextMenu(mListView);
	}

	@Override
	public void onResume() {
		super.onResume();
		mContacts = getDataForListView();
		invalidateListView(mContacts);
	}
	
	private void invalidateListView(List<BasicContact> contacts) {
		mListView.setAdapter(new NameOnlyContactAdapter(this, contacts));
		mListView.invalidate();
	}

	private List<BasicContact> getDataForListView() {
		// get data for list view and set its adapter
		long groupId = getIntent().getLongExtra(Constants.INTENT_LONG_GROUP_ID, -1L);
		HashSet<Long> rawContactIds;
		if (groupId != -1) { // a group was selected
			List<Long> groupIds = new ArrayList<Long>(1);
			groupIds.add(groupId);
			rawContactIds = ContactsHelper.getRawContactIds(LabelView.this, groupIds);
		} else { // a tag was selected
			rawContactIds = ContactsHelper.getRawContactIdsGivenTag(LabelView.this,
					mLabel);
		}
		
		return ApplicationData.getBasicContacts(rawContactIds);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo info) {
    super.onCreateContextMenu(menu, v, info);
    MenuInflater inflater = getMenuInflater();
    BasicContact contact = (BasicContact) mContacts.get(
    		((AdapterContextMenuInfo) info).position);
    ContactUtils.inflateContextMenuForContact(LabelView.this, inflater, menu,
    		contact);
	}
	
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    BasicContact selectedContact = mContacts.get(info.position); 
    ContactUtils.onContextContactSelected(LabelView.this, item, selectedContact,
    		new DeleteListener(selectedContact));
    
    return true;
  }
	
  /**
   * Listener for the delete confirmation dialog.
   */
  private class DeleteListener implements DialogInterface.OnClickListener {
  	private BasicContact mContact;
  	
  	/**
  	 * Constructor
  	 */
  	public DeleteListener(BasicContact contact) {
  		mContact = contact;
  	}
  	
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					ContactsHelper.deleteRawContact(LabelView.this,
							mContact.getRawContactId());
					ApplicationData.removeContact(mContact.getRawContactId());
					mContacts.remove(mContact);
					invalidateListView(mContacts);
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					dialog.cancel();
					break;
			}
		}
  }
}
