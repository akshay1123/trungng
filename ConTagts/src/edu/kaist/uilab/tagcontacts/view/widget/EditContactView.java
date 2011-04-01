// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.view.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
public class EditContactView extends Activity {
	
	@SuppressWarnings("unused")
	private static final String TAG = "EditContactView";
	
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
	static final int EDIT_EXISTING_PHONE_REQUEST = 23;
	static final int EDIT_EXISTING_GROUP_REQUEST = 24;
	static final int EDIT_EXISTING_TAGS_REQUEST = 25;
	static final int EDIT_EXISTING_EMAIL_REQUEST = 26;
	static final int EDIT_EXISTING_JOB_REQUEST = 27;
	static final int EDIT_EXISTING_ADDRESS_REQUEST = 28;
	static final int EDIT_EXISTING_NOTES_REQUEST = 29;
	static final int EDIT_EXISTING_NICKNAME_REQUEST = 30;
	static final int EDIT_EXISTING_WEBSITE_REQUEST = 31;
	
	private LinearLayout phoneRegion, tagsRegion, emailRegion;
	private LinearLayout jobRegion, addressRegion, notesRegion, nicknameRegion;
	private LinearLayout websiteRegion;
	private LayoutInflater inflater;

	private BatchOperation batch;
	private long rawContactId = 0;
	private View source; // source of the calling activity

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_contact);

		inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		
		// make top bar's title appropriate for this view
		TextView tv = (TextView) findViewById(R.id.top_bar_title);
		tv.setText(R.string.title_edit_contact);

		// hide the cancel button
		((Button) findViewById(R.id.btn_bar_left)).setVisibility(View.INVISIBLE);
		
		Button save = (Button) findViewById(R.id.btn_bar_right);
		save.setText(R.string.done);
		save.setEnabled(true);
		save.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (batch.size() > 0) {
					Intent intent = getIntent();
					intent.putExtra(Constants.INTENT_LONG_RAW_CONTACT_ID, rawContactId);
					setResult(Constants.RESULT_EDITTED, intent);
					batch.execute();
					// update our cache
					Contact contact = ContactsHelper.getContact(EditContactView.this,
							rawContactId);
					if (contact.getTags().size() > 0) {
						ServerConnector.sendLabelLog(EditContactView.this, "tag", contact.getTags(), contact);
					}	
					ApplicationData.updateContact(rawContactId, contact);
				} else {
					setResult(RESULT_CANCELED);
				}
				finish();
			}
		});
		
		// set listeners for add buttons
		NewEntityListener newEntityListener = new NewEntityListener();
		((Button) findViewById(R.id.btn_add_phone)).setOnClickListener(newEntityListener);
		findViewById(R.id.left_btn_add_phone).setOnClickListener(newEntityListener);		
		((Button) findViewById(R.id.btn_add_tags)).setOnClickListener(newEntityListener);
		findViewById(R.id.left_btn_add_tags).setOnClickListener(newEntityListener);
		((Button) findViewById(R.id.btn_add_email)).setOnClickListener(newEntityListener);
		findViewById(R.id.left_btn_add_email).setOnClickListener(newEntityListener);
		((Button) findViewById(R.id.btn_add_other)).setOnClickListener(newEntityListener);
		findViewById(R.id.left_btn_add_other).setOnClickListener(newEntityListener);

		// add existing data to layout regions
		rawContactId = getIntent().getLongExtra(Constants.INTENT_LONG_RAW_CONTACT_ID,
				Constants.INVALID_ID);
		HashMap<String, List<Entity>> map = ContactsHelper.getEntitiesForContact(
				EditContactView.this, rawContactId);
		
		NameEntity name = (NameEntity) map.get(NameEntity.MIMETYPE).get(0);
		Button btnName = (Button) findViewById(R.id.btn_name);
		btnName.setOnClickListener(new EditEntityListener(name,	EntityType.NAME_ENTITY, false));
		btnName.setText(name.toString());
		
		phoneRegion = (LinearLayout) findViewById(R.id.layout_region_phone);
		addLayoutsForExistingEntities(phoneRegion, map.get(PhoneEntity.MIMETYPE), 
				EntityType.PHONE_ENTITY);
		tagsRegion = (LinearLayout) findViewById(R.id.layout_region_tags);
		addLayoutsForExistingEntities(tagsRegion, map.get(TagEntity.MIMETYPE),
				EntityType.TAG_ENTITY);
		emailRegion = (LinearLayout) findViewById(R.id.layout_region_email);
		addLayoutsForExistingEntities(emailRegion, map.get(EmailEntity.MIMETYPE),
				EntityType.EMAIL_ENTITY);
		jobRegion = (LinearLayout) findViewById(R.id.layout_region_job);
		addLayoutsForExistingEntities(jobRegion, map.get(JobEntity.MIMETYPE),
				EntityType.JOB_ENTITY);
		addressRegion = (LinearLayout) findViewById(R.id.layout_region_address);
		addLayoutsForExistingEntities(addressRegion, map.get(AddressEntity.MIMETYPE),
				EntityType.ADDRESS_ENTITY);
		notesRegion = (LinearLayout) findViewById(R.id.layout_region_notes);
		addLayoutsForExistingEntities(notesRegion, map.get(NoteEntity.MIMETYPE),
				EntityType.NOTES_ENTITY);
		nicknameRegion = (LinearLayout) findViewById(R.id.layout_region_nickname);
		addLayoutsForExistingEntities(nicknameRegion, map.get(NicknameEntity.MIMETYPE),
				EntityType.NICKNAME_ENTITY);
		websiteRegion = (LinearLayout) findViewById(R.id.layout_region_website);
		addLayoutsForExistingEntities(websiteRegion, map.get(
				WebsiteEntity.MIMETYPE), EntityType.WEBSITE_ENTITY);
		
		batch = new BatchOperation(this, getContentResolver());
	}
	
	/**
	 * Adds layouts for existing entities, i.e., entities that were persisted to
	 * database before
	 */
	private void addLayoutsForExistingEntities(LinearLayout region,
			List<Entity> entities, EntityType type) {
		if (type == EntityType.TAG_ENTITY) {
			StringBuilder tags = new StringBuilder();
			for (Entity entity : entities) {
				tags.append(((TagEntity) entity).getLabel()).append(",");
			}
			if (tags.length() > 1) {
				tags.deleteCharAt(tags.length() - 1);
				addLayoutForTags(tags.toString(), false);
			}
		} else {
			for (Entity entity : entities) {
				addLayoutForEntity(region, entity, type, false);
			}
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
				case EDIT_NAME_REQUEST:
					editName(data);
					break;
				case EDIT_PHONE_REQUEST:
					editPhone(data, true);
					break;
				case EDIT_TAGS_REQUEST:
					editTags(data, true);
					break;
				case EDIT_EMAIL_REQUEST:
					editEmail(data, true);
					break;
				case EDIT_EXISTING_PHONE_REQUEST:
					editPhone(data, false);
					break;
				case EDIT_EXISTING_TAGS_REQUEST:
					editTags(data, false);
					break;
				case EDIT_EXISTING_EMAIL_REQUEST:
					editEmail(data, false);
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
				editJob(data, true);
				break;
			case EDIT_ADDRESS_REQUEST:
				editAddress(data, true);
				break;
			case EDIT_NOTES_REQUEST:
				editNotes(data, true);
				break;
			case EDIT_NICKNAME_REQUEST:
				editNickname(data, true);
				break;
			case EDIT_WEBSITE_REQUEST:
				editWebsite(data, true);
				break;
			case EDIT_EXISTING_JOB_REQUEST:
				editJob(data, false);
				break;
			case EDIT_EXISTING_ADDRESS_REQUEST:
				editAddress(data, false);
				break;
			case EDIT_EXISTING_NOTES_REQUEST:
				editNotes(data, false);
				break;
			case EDIT_EXISTING_NICKNAME_REQUEST:
				editNickname(data, false);
				break;
			case EDIT_EXISTING_WEBSITE_REQUEST:
				editWebsite(data, false);
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

	private void editWebsite(Intent data, boolean isNew) {
		String oldTxt1 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA1);
		WebsiteEntity oldEntity = null;
		if (oldTxt1 != null) {
			oldEntity = new WebsiteEntity(rawContactId, oldTxt1);
		}
		WebsiteEntity newEntity = new WebsiteEntity(rawContactId,
				data.getStringExtra(Constants.INTENT_TEXT_EXTRA1));
		editEntity(websiteRegion, oldEntity, newEntity, EntityType.WEBSITE_ENTITY, isNew);
	}

	private void editNickname(Intent data, boolean isNew) {
		String oldTxt1 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA1);
		NicknameEntity oldEntity = null;
		if (oldTxt1 != null) {
			oldEntity = new NicknameEntity(rawContactId, oldTxt1);
		}
		NicknameEntity newEntity = new NicknameEntity(rawContactId,
				data.getStringExtra(Constants.INTENT_TEXT_EXTRA1));
		editEntity(nicknameRegion, oldEntity, newEntity, EntityType.NICKNAME_ENTITY, isNew);
	}

	private void editNotes(Intent data, boolean isNew) {
		String oldTxt1 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA1);
		NoteEntity oldEntity = null;
		if (oldTxt1 != null) {
			oldEntity = new NoteEntity(rawContactId, oldTxt1);
		}
		NoteEntity newEntity = new NoteEntity(rawContactId,
				data.getStringExtra(Constants.INTENT_TEXT_EXTRA1));
		editEntity(notesRegion, oldEntity, newEntity, EntityType.NOTES_ENTITY, isNew);
	}

	private void editAddress(Intent data, boolean isNew) {
		String oldTxt1 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA1);
		String oldTxt2 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA2);
		String oldTxt3 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA3);
		String oldTxt4 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA4);
		AddressEntity oldEntity = null;
		if (oldTxt1 != null || oldTxt2 != null || oldTxt3 != null
				|| oldTxt4 != null) {
			oldEntity = new AddressEntity(rawContactId, oldTxt1, oldTxt2, oldTxt3,
					oldTxt4);
		}
		AddressEntity newEntity = new AddressEntity(rawContactId,
				data.getStringExtra(Constants.INTENT_TEXT_EXTRA1),
				data.getStringExtra(Constants.INTENT_TEXT_EXTRA2),
				data.getStringExtra(Constants.INTENT_TEXT_EXTRA3),
				data.getStringExtra(Constants.INTENT_TEXT_EXTRA4));
		editEntity(addressRegion, oldEntity, newEntity, EntityType.ADDRESS_ENTITY, isNew);
	}

	private void editEmail(Intent data, boolean isNew) {
		String oldTxt1 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA1);
		EmailEntity oldEntity = null;
		if (oldTxt1 != null) {
			oldEntity = new EmailEntity(rawContactId, oldTxt1);
		}
		EmailEntity newEntity = new EmailEntity(rawContactId, data.getStringExtra(
				Constants.INTENT_TEXT_EXTRA1));
		editEntity(emailRegion, oldEntity, newEntity, EntityType.EMAIL_ENTITY, isNew);
	}

	private void editJob(Intent data, boolean isNew) {
		String oldTxt1 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA1);
		String oldTxt2 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA2);
		String oldTxt3 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA3);
		JobEntity oldEntity = null;
		if (oldTxt1 != null || oldTxt2 != null || oldTxt3 != null) {
			oldEntity = new JobEntity(rawContactId, oldTxt1, oldTxt2, oldTxt3);
		}
		JobEntity newEntity = new JobEntity(rawContactId,
				data.getStringExtra(Constants.INTENT_TEXT_EXTRA1),
				data.getStringExtra(Constants.INTENT_TEXT_EXTRA2),
				data.getStringExtra(Constants.INTENT_TEXT_EXTRA3));
		editEntity(jobRegion, oldEntity, newEntity, EntityType.JOB_ENTITY, isNew);
	}

	private void editPhone(Intent data, boolean isNew) {
		String oldTxt1 = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA1);
		PhoneEntity oldEntity = null;
		if (oldTxt1 != null) {
			oldEntity = new PhoneEntity(rawContactId, oldTxt1);
		}
		PhoneEntity newEntity = new PhoneEntity(rawContactId, data.getStringExtra(
				Constants.INTENT_TEXT_EXTRA1));
		editEntity(phoneRegion, oldEntity, newEntity, EntityType.PHONE_ENTITY, isNew);
	}
	
	private void editEntity(LinearLayout region, Entity oldEntity, Entity newEntity,
			EntityType type, boolean isNew) {
		if (oldEntity != null) {
			View entityLayout = source; // get the source of action
			((TextView) entityLayout.findViewById(R.id.txt_entity_content)).setText(newEntity.toString());
			entityLayout.setOnClickListener(new EditEntityListener(newEntity, type, isNew));			
			if (isNew) {
  			// remove the previously added batch
				batch.remove(oldEntity);
			}
		} else {
			// this is an add_action so add a new layout to the region
			addLayoutForEntity(region, newEntity, type, true);
		}
		
		if (isNew) {
			// new data so adds insert operation
			batch.add(ContactsHelper.createInsertOp(newEntity), newEntity);
		} else {
			// existing data so adds update operation
			batch.add(ContactsHelper.createUpdateOp(oldEntity, newEntity), newEntity);
		}
	}

	/**
	 * Adds layout for tag entities.
	 */
	private void addLayoutForTags(final String tags, final boolean isNew) {
		// inflate the view
		final LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.edit_entity, null);
		View entityLayout = layout.findViewById(R.id.layout_data);
		((TextView) entityLayout.findViewById(R.id.txt_entity_type)).setText(
				ContactUtils.getStringForType(getResources(), EntityType.TAG_ENTITY));
		((TextView) entityLayout.findViewById(R.id.txt_entity_content)).setText(tags);
		entityLayout.setOnClickListener(new EditTagsListener(isNew));
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
				if (isNew) {
					removeTagOperations(batch, tags);
				} else {
					deleteTagEntities(tags);
				}
			}
		});
	}
	
	/**
	 * Adds layout for an entity other than tag.
	 */
	private void addLayoutForEntity(final LinearLayout region,
			final Entity entity, EntityType type, final boolean isNew) {
		// inflate the view
		final LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.edit_entity, null);
		View entityLayout = layout.findViewById(R.id.layout_data);
		((TextView) entityLayout.findViewById(R.id.txt_entity_type)).setText(
				ContactUtils.getStringForType(getResources(), type));
		((TextView) entityLayout.findViewById(R.id.txt_entity_content)).setText(
				entity.toString());
		
		entityLayout.setOnClickListener(new EditEntityListener(entity, type, isNew));
		region.addView(layout, region.getChildCount() - 1);
		
		// set actions
		Button btnDelete = (Button) layout.findViewById(R.id.btn_delete);
		btnDelete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// remove the layout
				region.removeView(layout);
				region.invalidate();
				if (isNew) {
					// new data so just need to cancel the operation
					batch.remove(entity);
				} else {
					// existing data so need to delete from database
					batch.add(ContactsHelper.createDeleteOp(entity), entity);
				}
			}
		});
	}

	/**
	 * Edit tags. We need this specific method for tag because users can enter
	 * multiple tag entities at a time! 
	 */
	private void editTags(Intent data, boolean isNew) {
		String oldTags = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA1);
		String newTags = data.getStringExtra(Constants.INTENT_TEXT_EXTRA1);

		if (oldTags != null) {
			View entityLayout = source;
			((TextView) entityLayout.findViewById(R.id.txt_entity_content)).setText(newTags);
			entityLayout.setOnClickListener(new EditTagsListener(isNew));
			// remove or delete old tags
			if (isNew) {
				removeTagOperations(batch, oldTags);
			} else {
				deleteTagEntities(oldTags);
			}
		} else {
			// this is an add_action so add a new layout to the region
			addLayoutForTags(newTags, true);
		}
		
		// add a new batch operation for both cases (edit or add)
		ArrayList<TagEntity> entities = ContactUtils.stringToTagEntities(newTags, rawContactId);
		for (TagEntity entity : entities) {
			batch.add(ContactsHelper.createInsertOp(entity), entity);
		}
	}

	/**
	 * Helper method for removing {@link TagEntity} operations from batch.
	 */
	private void removeTagOperations(BatchOperation batch, String tags) {
		ArrayList<TagEntity> entities = ContactUtils.stringToTagEntities(tags, rawContactId);
		for (TagEntity entity : entities) {
			batch.remove(entity);
		}
	}

	/**
	 * Helper method for deleting {@link TagEntity}s from database.
	 * @param tags
	 */
	private void deleteTagEntities(String tags) {
		ArrayList<TagEntity> entities = ContactUtils.stringToTagEntities(tags, rawContactId);
		for (TagEntity entity : entities) {
			batch.add(ContactsHelper.createDeleteOp(entity), entity);
		}
	}
	
	private void editName(Intent data) {
		String given = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA1);
		String middle = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA2);
		String family = data.getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA3);
		NameEntity oldEntity = new NameEntity(rawContactId, given, middle, family);
		// remove the old batch operation (if any)
		batch.remove(oldEntity);
		// then add the new batch
		given = data.getStringExtra(Constants.INTENT_TEXT_EXTRA1);
		middle = data.getStringExtra(Constants.INTENT_TEXT_EXTRA2);
		family = data.getStringExtra(Constants.INTENT_TEXT_EXTRA3);
		Entity newEntity = new NameEntity(rawContactId, given, middle, family);
		batch.add(ContactsHelper.createUpdateOp(oldEntity, newEntity), newEntity);
		// also update the view
		Button button = (Button) findViewById(R.id.btn_name); 
		button.setText(newEntity.toString());
		button.setOnClickListener(new EditEntityListener(newEntity, EntityType.NAME_ENTITY, false));
	}

	/**
	 * Listener for buttons which allow adding new entity.
	 */
	class NewEntityListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			int id = v.getId();
			Intent intent = null;
			switch (id) {
				case R.id.btn_add_phone:
				case R.id.left_btn_add_phone:
					intent = new Intent(EditContactView.this, EditPhoneView.class);
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
				default:
					break;
			}
		}	
	}
	
	/**
	 * Listener for edit buttons other than tags.
	 */
	class EditEntityListener implements OnClickListener {
		private Entity mEntity;
		private EntityType mType;
		private boolean mIsNew;
		
		/**
		 * Constructor for edit buttons.
		 * 
		 * @param entity an entity
		 * @param type type of the entity
		 */
		public EditEntityListener(Entity entity, EntityType type, boolean isNew) {
			this.mEntity = entity;
			this.mType = type;
			this.mIsNew = isNew;
		}
		
		@Override
		public void onClick(View v) {
			int id = v.getId();
			Intent intent = null;
			switch (id) {
				case R.id.btn_name:
					intent = new Intent(v.getContext(), EditNameView.class);
					NameEntity name = (NameEntity) mEntity;
					intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA1, name.getGiven());
					intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA2, name.getMiddle());
					intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA3, name.getFamily());
					startActivityForResult(intent, EDIT_NAME_REQUEST);
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
			switch (mType) {
				case ADDRESS_ENTITY:
					AddressEntity address = (AddressEntity) mEntity;
					intent = new Intent(EditContactView.this, EditAddressView.class);
					intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA1, address.getStreet());
					intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA2, address.getCity());
					intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA3, address.getRegion());
					intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA4, address.getPostcode());
					if (mIsNew) {
						startActivityForResult(intent, EDIT_ADDRESS_REQUEST);
					} else {
						startActivityForResult(intent, EDIT_EXISTING_ADDRESS_REQUEST);
					}
					break;
				case EMAIL_ENTITY:
					EmailEntity email = (EmailEntity) mEntity;
					intent = new Intent(EditContactView.this, EditEmailView.class);
					intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA1, email.getData());
					if (mIsNew) {
						startActivityForResult(intent, EDIT_EMAIL_REQUEST);
					} else {
						startActivityForResult(intent, EDIT_EXISTING_EMAIL_REQUEST);
					}
					break;
				case JOB_ENTITY:
					JobEntity job = (JobEntity) mEntity;
					intent = new Intent(EditContactView.this, EditJobView.class);
					intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA1, job.getCompany());
					intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA2, job.getTitle());
					intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA3, job.getDepartment());
					if (mIsNew) {
						startActivityForResult(intent, EDIT_JOB_REQUEST);
					} else {
						startActivityForResult(intent, EDIT_EXISTING_JOB_REQUEST);
					}
					break;
				case NICKNAME_ENTITY:
					NicknameEntity nickname = (NicknameEntity) mEntity;
					intent = new Intent(EditContactView.this, EditNicknameView.class);
					intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA1, nickname.getName());
					if (mIsNew) {
						startActivityForResult(intent, EDIT_NICKNAME_REQUEST);
					} else {
						startActivityForResult(intent, EDIT_EXISTING_NICKNAME_REQUEST);
					}
					break;
				case NOTES_ENTITY:
					NoteEntity notes = (NoteEntity) mEntity;
					intent = new Intent(EditContactView.this, EditNotesView.class);
					intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA1, notes.getNote());
					if (mIsNew) {
						startActivityForResult(intent, EDIT_NOTES_REQUEST);
					} else {
						startActivityForResult(intent, EDIT_EXISTING_NOTES_REQUEST);
					}
					break;
				case WEBSITE_ENTITY:
					WebsiteEntity website = (WebsiteEntity) mEntity;
					intent = new Intent(EditContactView.this, EditWebsiteView.class);
					intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA1, website.getUrl());
					if (mIsNew) {
						startActivityForResult(intent, EDIT_WEBSITE_REQUEST);
					} else {
						startActivityForResult(intent, EDIT_EXISTING_WEBSITE_REQUEST);
					}
					break;
				case PHONE_ENTITY:
					intent = new Intent(EditContactView.this, EditPhoneView.class);
					intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA1,
							((PhoneEntity) mEntity).getNumber());
					if (mIsNew) {
						startActivityForResult(intent, EDIT_PHONE_REQUEST);
					} else {
						startActivityForResult(intent, EDIT_EXISTING_PHONE_REQUEST);
					}
					break;
			}
		}
	}
	
	/**
	 * Listener for edit tags button.
	 */
	class EditTagsListener implements OnClickListener {
		
		private boolean isNew;
		
		public EditTagsListener(boolean isNew) {
			this.isNew = isNew;
		}
		
		@Override
		public void onClick(View v) {
			source = v;
			Intent intent = new Intent(v.getContext(), EditTagsView.class);
			intent.putExtra(Constants.INTENT_OLD_TEXT_EXTRA1,
					((TextView) v.findViewById(R.id.txt_entity_content)).getText());
			intent.putExtra(Constants.INTENT_OLD_TEXT_NAME,
					((Button) findViewById(R.id.btn_name)).getText());
			if (isNew) {
				startActivityForResult(intent, EDIT_TAGS_REQUEST);
			} else {
				startActivityForResult(intent, EDIT_EXISTING_TAGS_REQUEST);
			}
		}
	}
}
