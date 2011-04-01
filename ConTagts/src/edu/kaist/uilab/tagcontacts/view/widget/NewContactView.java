// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.view.widget;

import java.util.ArrayList;
import java.util.StringTokenizer;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import edu.kaist.uilab.tagcontacts.Constants;
import edu.kaist.uilab.tagcontacts.ContactUtils;
import edu.kaist.uilab.tagcontacts.R;
import edu.kaist.uilab.tagcontacts.ServerConnector;
import edu.kaist.uilab.tagcontacts.database.ApplicationData;
import edu.kaist.uilab.tagcontacts.database.BatchOperation;
import edu.kaist.uilab.tagcontacts.database.ContactsHelper;
import edu.kaist.uilab.tagcontacts.model.AddressEntity;
import edu.kaist.uilab.tagcontacts.model.Contact;
import edu.kaist.uilab.tagcontacts.model.EmailEntity;
import edu.kaist.uilab.tagcontacts.model.Entity;
import edu.kaist.uilab.tagcontacts.model.JobEntity;
import edu.kaist.uilab.tagcontacts.model.NameEntity;
import edu.kaist.uilab.tagcontacts.model.NicknameEntity;
import edu.kaist.uilab.tagcontacts.model.NoteEntity;
import edu.kaist.uilab.tagcontacts.model.PhoneEntity;
import edu.kaist.uilab.tagcontacts.model.TagEntity;
import edu.kaist.uilab.tagcontacts.model.WebsiteEntity;
import edu.kaist.uilab.tagcontacts.model.Entity.EntityType;

/**
 * Editor for adding a new contact.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class NewContactView extends Activity {
	
	@SuppressWarnings("unused")
	private static final String TAG = "NewContactView";
	
	static final int EDIT_NAME_REQUEST = 12;
	static final int EDIT_PHONE_REQUEST = 13;
	static final int EDIT_GROUP_REQUEST = 14;
	static final int EDIT_TAGS_REQUEST = 15;
	static final int EDIT_EMAIL_REQUEST = 16;
	static final int EDIT_OTHER_REQUEST = 17;
	static final int EDIT_JOB_REQUEST = 18;
	static final int EDIT_ADDRESS_REQUEST = 19;
	static final int EDIT_NOTES_REQUEST = 20;
	static final int EDIT_NICKNAME_REQUEST = 21;
	static final int EDIT_WEBSITE_REQUEST = 22;
	
	private LinearLayout phoneRegion, tagsRegion, emailRegion;
	private LinearLayout jobRegion, addressRegion, notesRegion, nicknameRegion;
	private LinearLayout websiteRegion;
	private LayoutInflater inflater;
	private Button btnSave;

	private BatchOperation batch;
	private int newlyInsertedId = 0;
	private View source; // source of the calling activity

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_contact);
		
		// make top bar's title appropriate for this view
		TextView tv = (TextView) findViewById(R.id.top_bar_title);
		tv.setText(R.string.title_new_contact);

		// set listener for the cancel button
		Button cancel = (Button) findViewById(R.id.btn_bar_left);
		cancel.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		
		btnSave = (Button) findViewById(R.id.btn_bar_right);
		btnSave.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (batch.size() > 1) {
					// only add a contact if some info was added
					Uri uri = batch.execute();
					Intent intent = getIntent();
					intent.setData(uri);
					// update our cache
					Contact contact = ContactsHelper.getContact(NewContactView.this, uri);
					if (contact.getTags().size() > 0) {
						ServerConnector.sendLabelLog(NewContactView.this, "tag",
								contact.getTags(), contact);
					}	
					ApplicationData.addContact(contact);
				}
				finish();
			}
		});
		
		// set listeners for buttons
		final Button btnName = (Button) findViewById(R.id.btn_name);
		btnName.setOnClickListener(new EditEntityListener(null, EntityType.NAME_ENTITY));
		((Button) findViewById(R.id.btn_add_phone)).setOnClickListener(
				new EditEntityListener(null, EntityType.PHONE_ENTITY));
		findViewById(R.id.left_btn_add_phone).setOnClickListener(new EditEntityListener(
				null, EntityType.PHONE_ENTITY));		
		((Button) findViewById(R.id.btn_add_tags)).setOnClickListener(
				new EditEntityListener(null, EntityType.TAG_ENTITY));
		findViewById(R.id.left_btn_add_tags).setOnClickListener(new EditEntityListener(
				null, EntityType.TAG_ENTITY));
		((Button) findViewById(R.id.btn_add_email)).setOnClickListener(
				new EditEntityListener(null, EntityType.EMAIL_ENTITY));
		findViewById(R.id.left_btn_add_email).setOnClickListener(new EditEntityListener(
				null, EntityType.EMAIL_ENTITY));
		((Button) findViewById(R.id.btn_add_other)).setOnClickListener(
				new EditEntityListener(null, null));
		findViewById(R.id.left_btn_add_other).setOnClickListener(new EditEntityListener(
				null, null));

		// get layout regions
		phoneRegion = (LinearLayout) findViewById(R.id.layout_region_phone);
		tagsRegion = (LinearLayout) findViewById(R.id.layout_region_tags);
		emailRegion = (LinearLayout) findViewById(R.id.layout_region_email);
		jobRegion = (LinearLayout) findViewById(R.id.layout_region_job);
		addressRegion = (LinearLayout) findViewById(R.id.layout_region_address);
		notesRegion = (LinearLayout) findViewById(R.id.layout_region_notes);
		nicknameRegion = (LinearLayout) findViewById(R.id.layout_region_nickname);
		websiteRegion = (LinearLayout) findViewById(R.id.layout_region_website);
		
		batch = new BatchOperation(this, getContentResolver());
		// assuming we are going to create a new contact (which might be canceled) later
		batch.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
				.withValue(RawContacts.ACCOUNT_NAME, null)
				.withValue(RawContacts.ACCOUNT_TYPE, null)
				.withValue(RawContacts.TIMES_CONTACTED, 0)
				.build());
		inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
				case EDIT_NAME_REQUEST:
					editName(data);
					break;
				case EDIT_PHONE_REQUEST:
					editPhone(data);
					break;
				case EDIT_TAGS_REQUEST:
					editTags(data);
					break;
				case EDIT_EMAIL_REQUEST:
					editEmail(data);
					break;
				case EDIT_OTHER_REQUEST:
					editOther(data);
					break;
				default:
					editOther(requestCode, data);
					break;
			}
		}
	}

	private void editOther(int requestCode, Intent data) {
		switch (requestCode) {
			case EDIT_JOB_REQUEST:
				editJob(data);
				break;
			case EDIT_ADDRESS_REQUEST:
				editAddress(data);
				break;
			case EDIT_NOTES_REQUEST:
				editNotes(data);
				break;
			case EDIT_NICKNAME_REQUEST:
				editNickname(data);
				break;
			case EDIT_WEBSITE_REQUEST:
				editWebsite(data);
				break;
			default:
				break;
		}
	}
	
	/**
	 * For activity that was not initiated directly from this activity.
	 */
	private void editOther(Intent data) {
		int requestCode = data.getIntExtra(Constants.INTENT_INT_REQUEST_CODE, -1);
		editOther(requestCode, data);
	}

	private void editWebsite(Intent data) {
		String oldTxt1 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA1);
		WebsiteEntity oldEntity = null;
		if (oldTxt1 != null) {
			oldEntity = new WebsiteEntity(oldTxt1);
		}
		WebsiteEntity newEntity = new WebsiteEntity(data.getStringExtra(
				Constants.INTENT_TEXT_EXTRA1));
		editEntity(websiteRegion, oldEntity, newEntity,
				EntityType.WEBSITE_ENTITY);
	}

	private void editNickname(Intent data) {
		String oldTxt1 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA1);
		NicknameEntity oldEntity = null;
		if (oldTxt1 != null) {
			oldEntity = new NicknameEntity(oldTxt1);
		}
		NicknameEntity newEntity = new NicknameEntity(data.getStringExtra(
				Constants.INTENT_TEXT_EXTRA1));
		editEntity(nicknameRegion, oldEntity, newEntity,
				EntityType.NICKNAME_ENTITY);
	}

	private void editNotes(Intent data) {
		String oldTxt1 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA1);
		NoteEntity oldEntity = null;
		if (oldTxt1 != null) {
			oldEntity = new NoteEntity(oldTxt1);
		}
		NoteEntity newEntity = new NoteEntity(data.getStringExtra(
				Constants.INTENT_TEXT_EXTRA1));
		editEntity(notesRegion, oldEntity, newEntity,
				EntityType.NOTES_ENTITY);
	}

	private void editAddress(Intent data) {
		String oldTxt1 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA1);
		String oldTxt2 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA2);
		String oldTxt3 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA3);
		String oldTxt4 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA4);
		AddressEntity oldEntity = null;
		if (oldTxt1 != null || oldTxt2 != null || oldTxt3 != null
				|| oldTxt4 != null) {
			oldEntity = new AddressEntity(oldTxt1, oldTxt2, oldTxt3, oldTxt4);
		}
		AddressEntity newEntity = new AddressEntity(
				data.getStringExtra(Constants.INTENT_TEXT_EXTRA1),
				data.getStringExtra(Constants.INTENT_TEXT_EXTRA2),
				data.getStringExtra(Constants.INTENT_TEXT_EXTRA3),
				data.getStringExtra(Constants.INTENT_TEXT_EXTRA4));
		editEntity(addressRegion, oldEntity, newEntity,
				EntityType.ADDRESS_ENTITY);
	}

	/**
	 * Edit tags. We need this specific method for tag because users can enter
	 * multiple tag entities at a time! 
	 */
	private void editTags(Intent data) {
		String oldTags = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA1);
		String newTags = data.getStringExtra(Constants.INTENT_TEXT_EXTRA1);

		if (oldTags != null) {
			View entityLayout = source; // get the source of action
			entityLayout.setOnClickListener(new EditTagsListener());
			((TextView) entityLayout.findViewById(R.id.txt_entity_content)).setText(newTags);
  		// remove the previously added batch (tags)
			removeTagOperations(batch, oldTags);
		} else {
			// this is an add_action so add a new layout to the region
			addTagLayoutForTags(newTags);
		}
		
		// add a new batch operation for both cases (edit or add)
		ArrayList<TagEntity> entities = stringToTagEntities(newTags);
		for (TagEntity entity : entities) {
			ContentProviderOperation op = ContentProviderOperation.newInsert(
					Data.CONTENT_URI)
					.withValueBackReference(Data.RAW_CONTACT_ID, newlyInsertedId)
					.withValues(entity.buildContentValuesWithNoId())
					.build();
			batch.add(op, entity);
		}
	}

	/**
	 * Parses a string and returns {@link TagEntity}s.
	 *
	 * <p> The assumption is that each {@link TagEntity} is separated by a comma.
	 */
	private ArrayList<TagEntity> stringToTagEntities(String tags) {
		ArrayList<TagEntity> tagEntities = new ArrayList<TagEntity>();
		StringTokenizer tokenizer = new StringTokenizer(tags, ",");
		while (tokenizer.hasMoreTokens()) {
			String tag = tokenizer.nextToken().trim();
			tagEntities.add(new TagEntity(tag));
		}
		
		return tagEntities;
	}
	
	/**
	 * Helper method for removing TagEntity operations.
	 */
	private void removeTagOperations(BatchOperation batch, String tags) {
		ArrayList<TagEntity> entities = stringToTagEntities(tags);
		for (TagEntity entity : entities) {
			batch.remove(entity);
		}
	}
	
	private void editEmail(Intent data) {
		String oldTxt1 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA1);
		EmailEntity oldEntity = null;
		if (oldTxt1 != null) {
			oldEntity = new EmailEntity(oldTxt1);
		}
		EmailEntity newEntity = new EmailEntity(data.getStringExtra(
				Constants.INTENT_TEXT_EXTRA1));
		editEntity(emailRegion, oldEntity, newEntity,
				EntityType.EMAIL_ENTITY);
	}

	private void editJob(Intent data) {
		String oldTxt1 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA1);
		String oldTxt2 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA2);
		String oldTxt3 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA3);
		JobEntity oldEntity = null;
		if (oldTxt1 != null || oldTxt2 != null || oldTxt3 != null) {
			oldEntity = new JobEntity(oldTxt1, oldTxt2, oldTxt3);
		}
		JobEntity newEntity = new JobEntity(
				data.getStringExtra(Constants.INTENT_TEXT_EXTRA1),
				data.getStringExtra(Constants.INTENT_TEXT_EXTRA2),
				data.getStringExtra(Constants.INTENT_TEXT_EXTRA3));
		editEntity(jobRegion, oldEntity, newEntity, EntityType.JOB_ENTITY);
	}

	private void editPhone(Intent data) {
		String oldTxt1 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA1);
		PhoneEntity oldEntity = null;
		if (oldTxt1 != null) {
			oldEntity = new PhoneEntity(oldTxt1);
		}
		PhoneEntity newEntity = new PhoneEntity(data.getStringExtra(
				Constants.INTENT_TEXT_EXTRA1));
		editEntity(phoneRegion, oldEntity, newEntity,
				EntityType.PHONE_ENTITY);
	}
	
	private void editEntity(LinearLayout region, Entity oldEntity,
			Entity newEntity, EntityType type) {
		if (oldEntity != null) {
			View entityLayout = source; // get the source of action
			((TextView) entityLayout.findViewById(R.id.txt_entity_content)).setText(
					newEntity.toString());
			entityLayout.setOnClickListener(new EditEntityListener(newEntity, type));			
  		// remove the previously added batch
			batch.remove(oldEntity);
		} else {
			// this is an add_action so add a new layout to the region
			addLayoutForEntity(region, newEntity, type);
		}
		
		// add a new batch operation for both cases (edit or add)
		ContentProviderOperation op = ContentProviderOperation.newInsert(
				Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, newlyInsertedId)
				.withValues(newEntity.buildContentValuesWithNoId())
				.build();
		batch.add(op, newEntity);
	}

	private void addTagLayoutForTags(final String tags) {
		// inflate the view
		final LinearLayout layout = (LinearLayout) inflater.inflate(
				R.layout.edit_entity, null);
		View tagLayout = layout.findViewById(R.id.layout_data);
		((TextView) tagLayout.findViewById(R.id.txt_entity_type)).setText(
				ContactUtils.getStringForType(getResources(), EntityType.TAG_ENTITY));
		((TextView) tagLayout.findViewById(R.id.txt_entity_content)).setText(tags);
		tagLayout.setOnClickListener(new EditTagsListener());
		tagsRegion.addView(layout, tagsRegion.getChildCount() - 1);
		
		// set actions
		Button btnDelete = (Button) layout.findViewById(R.id.btn_delete);
		btnDelete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// remove the layout
				tagsRegion.removeView(layout);
				tagsRegion.invalidate();
				// and cancel the operation
				removeTagOperations(batch, tags);
			}
		});
	}
	
	private void addLayoutForEntity(final LinearLayout region, final Entity entity,
			EntityType type) {
		// inflate the view
		final LinearLayout layout = (LinearLayout) inflater.inflate(
				R.layout.edit_entity, null);
		Button btnDelete = (Button) layout.findViewById(R.id.btn_delete);
		View entityLayout = layout.findViewById(R.id.layout_data);
		((TextView) entityLayout.findViewById(R.id.txt_entity_type)).setText(
				ContactUtils.getStringForType(getResources(), type));
		((TextView) entityLayout.findViewById(R.id.txt_entity_content)).setText(
				entity.toString());
		region.addView(layout, region.getChildCount() - 1);
		
		// set actions
		btnDelete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// remove the layout
				region.removeView(layout);
				region.invalidate();
				// and cancel the operation
				batch.remove(entity);
			}
		});
		
		entityLayout.setOnClickListener(new EditEntityListener(entity, type));
	}
	
	private void editName(Intent data) {
		// remove the old batch operation (if any)
		String given = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA1);
		String middle = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA2);
		String family = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA3);
		NameEntity entity = new NameEntity(given, middle, family);
		batch.remove(entity);
		// then add the new batch
		given = data.getStringExtra(Constants.INTENT_TEXT_EXTRA1);
		middle = data.getStringExtra(Constants.INTENT_TEXT_EXTRA2);
		family = data.getStringExtra(Constants.INTENT_TEXT_EXTRA3);
		entity = new NameEntity(given, middle, family);
		ContentProviderOperation op = ContentProviderOperation.newInsert(
				Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, newlyInsertedId)
				.withValues(entity.buildContentValuesWithNoId())
				.build();
		batch.add(op, entity);
		// also update the view
		Button btnName = (Button) findViewById(R.id.btn_name);
		if (btnName.getText().length() > 0) {
			btnSave.setEnabled(true);
		} else {
			btnSave.setEnabled(false);
		}
		btnName.setText(entity.toString());
		btnName.setOnClickListener(new EditEntityListener(entity, EntityType.NAME_ENTITY));
	}
	
	/**
	 * Listener for edit buttons other than tags.
	 */
	class EditEntityListener implements OnClickListener {
		private Entity entity;
		private EntityType type;
		
		/**
		 * Constructor for edit buttons.
		 * 
		 * @param entity an entity
		 * @param type type of the entity
		 */
		public EditEntityListener(Entity entity, EntityType type) {
			this.entity = entity;
			this.type = type;
		}
		
		@Override
		public void onClick(View v) {
			int id = v.getId();
			Intent intent = null;
			switch (id) {
				case R.id.btn_name:
					intent = new Intent(v.getContext(), EditNameView.class);
					if (entity != null) {
						NameEntity name = (NameEntity) entity;
						intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA1, name.getGiven());
						intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA2, name.getMiddle());
						intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA3, name.getFamily());
					}
					startActivityForResult(intent, EDIT_NAME_REQUEST);
					return;
				case R.id.btn_add_phone:
				case R.id.left_btn_add_phone:
					intent = new Intent(NewContactView.this, EditPhoneView.class);
					startActivityForResult(intent, EDIT_PHONE_REQUEST);
					return;
				case R.id.btn_add_tags:
				case R.id.left_btn_add_tags:
					intent = new Intent(v.getContext(), EditTagsView.class);
					intent.putExtra(Constants.INTENT_OLD_TEXT_NAME,
							((Button) findViewById(R.id.btn_name)).getText());
					startActivityForResult(intent, EDIT_TAGS_REQUEST);
					return;
				case R.id.btn_add_email:
				case R.id.left_btn_add_email:
					intent = new Intent(v.getContext(), EditEmailView.class);
					startActivityForResult(intent, EDIT_EMAIL_REQUEST);
					return;
				case R.id.btn_add_other:
				case R.id.left_btn_add_other:
					intent = new Intent(v.getContext(), NewOtherView.class);
					startActivityForResult(intent, EDIT_OTHER_REQUEST);
					return;
				case R.id.layout_data:
					onEdit(v);
					break;
				default:
					break;
			}
		}
		
		private void onEdit(View v) {
			source = v;
			Intent intent = null;
			switch (type) {
			case ADDRESS_ENTITY:
				AddressEntity address = (AddressEntity) entity;
				intent = new Intent(NewContactView.this, EditAddressView.class);
				intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA1, address.getStreet());
				intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA2, address.getCity());
				intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA3, address.getRegion());
				intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA4, address.getPostcode());
				startActivityForResult(intent, EDIT_ADDRESS_REQUEST);
				break;
			case EMAIL_ENTITY:
				EmailEntity email = (EmailEntity) entity;
				intent = new Intent(NewContactView.this, EditEmailView.class);
				intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA1, email.getData());
				startActivityForResult(intent, EDIT_EMAIL_REQUEST);
				break;
			case JOB_ENTITY:
				JobEntity job = (JobEntity) entity;
				intent = new Intent(NewContactView.this, EditJobView.class);
				intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA1, job.getCompany());
				intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA2, job.getTitle());
				intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA3, job.getDepartment());
				startActivityForResult(intent, EDIT_JOB_REQUEST);
				break;
			case NICKNAME_ENTITY:
				NicknameEntity nickname = (NicknameEntity) entity;
				intent = new Intent(NewContactView.this, EditNicknameView.class);
				intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA1, nickname.getName());
				startActivityForResult(intent, EDIT_NICKNAME_REQUEST);
				break;
			case NOTES_ENTITY:
				NoteEntity notes = (NoteEntity) entity;
				intent = new Intent(NewContactView.this, EditNotesView.class);
				intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA1, notes.getNote());
				startActivityForResult(intent, EDIT_NOTES_REQUEST);
				break;
			case WEBSITE_ENTITY:
				WebsiteEntity website = (WebsiteEntity) entity;
				intent = new Intent(NewContactView.this, EditWebsiteView.class);
				intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA1, website.getUrl());
				startActivityForResult(intent, EDIT_WEBSITE_REQUEST);
				break;
			case PHONE_ENTITY:
				intent = new Intent(NewContactView.this, EditPhoneView.class);
				intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA1,
						((PhoneEntity) entity).getNumber());
				startActivityForResult(intent, EDIT_PHONE_REQUEST);
				break;
			}
		}
	}
	
	/**
	 * Listener for edit tags button.
	 */
	class EditTagsListener implements OnClickListener {
		
		@Override
		public void onClick(View v) {
			source = v;
			Intent intent = new Intent(v.getContext(), EditTagsView.class);
			intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA1,
					((TextView) v.findViewById(R.id.txt_entity_content)).getText());
			intent.putExtra(Constants.INTENT_OLD_TEXT_NAME,
					((Button) findViewById(R.id.btn_name)).getText());
			startActivityForResult(intent, EDIT_TAGS_REQUEST);
		}
	}
}
