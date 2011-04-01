// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.view.widget;

import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.kaist.uilab.tagcontacts.Constants;
import edu.kaist.uilab.tagcontacts.ContactUtils;
import edu.kaist.uilab.tagcontacts.R;
import edu.kaist.uilab.tagcontacts.database.ApplicationData;
import edu.kaist.uilab.tagcontacts.database.ContactsHelper;
import edu.kaist.uilab.tagcontacts.model.AddressEntity;
import edu.kaist.uilab.tagcontacts.model.EmailEntity;
import edu.kaist.uilab.tagcontacts.model.Entity;
import edu.kaist.uilab.tagcontacts.model.JobEntity;
import edu.kaist.uilab.tagcontacts.model.NameEntity;
import edu.kaist.uilab.tagcontacts.model.NicknameEntity;
import edu.kaist.uilab.tagcontacts.model.NoteEntity;
import edu.kaist.uilab.tagcontacts.model.PhoneEntity;
import edu.kaist.uilab.tagcontacts.model.TagEntity;
import edu.kaist.uilab.tagcontacts.model.WebsiteEntity;

/**
 * View for displaying detailed information of a contact.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class DetailedContactView extends Activity {

	@SuppressWarnings("unused")
	private static final String TAG = "DetailedContactView";
	
	private TextView mTxtName;
	private LinearLayout mListEntity;
	
	private LayoutInflater mInflater;
	private long mRawContactId;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contact_detail);

		mInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		mRawContactId = getIntent().getLongExtra(Constants.INTENT_LONG_RAW_CONTACT_ID, -1L);
		
		// make top bar's title appropriate for this view
		TextView tv = (TextView) findViewById(R.id.top_bar_title);
		tv.setText(R.string.title_info);

		// set listener for the cancel button
		Button btnBack = (Button) findViewById(R.id.btn_bar_left);
		btnBack.setText(R.string.back);
		btnBack.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		
		Button btnEdit = (Button) findViewById(R.id.btn_bar_right);
		btnEdit.setText(R.string.edit);
		btnEdit.setEnabled(true);
		btnEdit.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				initiateEditContact();
			}
		});
		
		Button btnShare = (Button) findViewById(R.id.btn_share);
		btnShare.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ContactUtils.displayShareContactDialog(DetailedContactView.this,
						mTxtName.getText().toString(), mRawContactId);
			}
		});
		mTxtName = (TextView) findViewById(R.id.txt_first_last);
		mListEntity = (LinearLayout) findViewById(R.id.list_entity);
		inflateView();
	}
	
	/**
	 * Starts the activity {@link EditContactView} for result.
	 */
	private void initiateEditContact() {
  	Intent intent = new Intent(DetailedContactView.this, EditContactView.class);
  	intent.putExtra(Constants.INTENT_LONG_RAW_CONTACT_ID, mRawContactId);
  	startActivityForResult(intent, 0);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Constants.RESULT_EDITTED) {
			inflateView();
		}	
	}
	
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
  	MenuInflater inflater = getMenuInflater();
  	inflater.inflate(R.menu.detailed_contact_menu, menu);
  	
  	return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
  	super.onOptionsItemSelected(item);
  	
  	int id = item.getItemId();
  	switch (id) {
  		case R.id.menu_edit_contact:
  			initiateEditContact();
      	break;
  		case R.id.menu_delete_contact:
  			ContactUtils.createDeleteConfirmationDialog(this, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case DialogInterface.BUTTON_POSITIVE:
								ContactsHelper.deleteRawContact(DetailedContactView.this,
										mRawContactId);
								ApplicationData.removeContact(mRawContactId);
								finish();
								break;
							case DialogInterface.BUTTON_NEGATIVE:
								dialog.cancel();
								break;
						}
					}
				}).show();
  			break;
  	}
  	
  	return true;
  }
  
  private void inflateView() {
		HashMap<String, List<Entity>> map = ContactsHelper.getEntitiesForContact(
				DetailedContactView.this, mRawContactId);
		// set name
		NameEntity name = (NameEntity) map.get(NameEntity.MIMETYPE).get(0);
		mTxtName.setText(name.toString());
		mListEntity.removeAllViews();
		
		LinearLayout layout;
		// add phones
		List<Entity> entities = map.get(PhoneEntity.MIMETYPE);
		for (Entity entity : entities) {
			View v = inflateLayout(R.string.call_phone, entity);
			final String phoneNumber = entity.toString();
			v.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ContactUtils.callContact(DetailedContactView.this, phoneNumber);
				}
			});
			mListEntity.addView(v);
		}
		// add emails
		entities = map.get(EmailEntity.MIMETYPE);
		for (Entity entity : entities) {
			View v = inflateLayout(R.string.send_email, entity);
			final String email = entity.toString();
			v.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ContactUtils.emailContact(DetailedContactView.this, email);
				}
			});
			mListEntity.addView(v);
		}
		// add tags
		entities = map.get(TagEntity.MIMETYPE);
		StringBuilder tagsBuilder = new StringBuilder();
		for (Entity entity : entities) {
			tagsBuilder.append(((TagEntity) entity).getLabel()).append(",");
		}
		if (tagsBuilder.length() > 0) {
			tagsBuilder.deleteCharAt(tagsBuilder.length() - 1);
			layout = (LinearLayout) mInflater.inflate(R.layout.entity_item, null);
			((TextView) layout.findViewById(R.id.entity_label)).setText(R.string.c_tags);
			((TextView) layout.findViewById(R.id.entity_data)).setText(tagsBuilder.toString());
			layout.setClickable(true);
			mListEntity.addView(layout);
		}
		
		// add jobs
		entities = map.get(JobEntity.MIMETYPE);
		for (Entity entity : entities) {
			mListEntity.addView(inflateLayout(R.string.c_job, entity));
		}
		// add address
		entities = map.get(AddressEntity.MIMETYPE);
		for (Entity entity : entities) {
			mListEntity.addView(inflateLayout(R.string.c_address, entity));
		}
		// add nickname
		entities = map.get(NicknameEntity.MIMETYPE);
		for (Entity entity : entities) {
			mListEntity.addView(inflateLayout(R.string.c_nickname, entity));
		}
		// add website
		entities = map.get(WebsiteEntity.MIMETYPE);
		for (Entity entity : entities) {
			View v = inflateLayout(R.string.c_website, entity);
			final String url = entity.toString();
			v.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ContactUtils.openWeb(DetailedContactView.this, url);
				}
			});
			mListEntity.addView(v);
		}
		// add notes
		entities = map.get(NoteEntity.MIMETYPE);
		for (Entity entity : entities) {
			mListEntity.addView(inflateLayout(R.string.c_note, entity));
		}
		
		// if no information is available
		if (mListEntity.getChildCount() == 0) {
			mListEntity.addView(mInflater.inflate(R.layout.contact_empty, null));
			return;
		}
  }
  
  private LinearLayout inflateLayout(int resId, Entity entity) {
		LinearLayout layout = (LinearLayout) mInflater.inflate(R.layout.entity_item, null);
		((TextView) layout.findViewById(R.id.entity_label)).setText(getString(resId));
		((TextView) layout.findViewById(R.id.entity_data)).setText(entity.toString());
		layout.setClickable(true);
		return layout;
  }
}
