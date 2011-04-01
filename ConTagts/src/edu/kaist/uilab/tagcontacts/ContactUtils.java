// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.res.Resources;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;

import edu.kaist.uilab.tagcontacts.database.ApplicationData;
import edu.kaist.uilab.tagcontacts.database.ContactsHelper;
import edu.kaist.uilab.tagcontacts.model.BasicContact;
import edu.kaist.uilab.tagcontacts.model.Contact;
import edu.kaist.uilab.tagcontacts.model.Group;
import edu.kaist.uilab.tagcontacts.model.GroupEntity;
import edu.kaist.uilab.tagcontacts.model.PhoneEntity;
import edu.kaist.uilab.tagcontacts.model.RawContact;
import edu.kaist.uilab.tagcontacts.model.TagEntity;
import edu.kaist.uilab.tagcontacts.model.Entity.EntityType;
import edu.kaist.uilab.tagcontacts.view.widget.EditContactView;

/**
 * Class for various util functions.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class ContactUtils {

	private static final String SMS_TYPE = "vnd.android-dir/mms-sms";
	private static final String ADDRESS = "address";
	private static final String SCHEME_TEL = "tel:";
	
	private static final int LAST_KOREAN_CODEPOINT = 52203;
	private static final int FIRST_KOREAN_CODEPOINT = 44032;
	private static final String LOG_FILE = "log.preference";
	
	/**
	 * Returns a {@link String} describing the given entity {@code type}.
	 * 
	 * @param res the application's resources
	 * @param type an {@link EntityType}
	 */
	public static String getStringForType(Resources res, EntityType type) {
		switch (type) {
			case ADDRESS_ENTITY:
				return res.getString(R.string.address);
			case EMAIL_ENTITY:
				return res.getString(R.string.email);
			case GROUP_ENTITY:
				return res.getString(R.string.groups);
			case JOB_ENTITY:
				return res.getString(R.string.job);
			case NICKNAME_ENTITY:
				return res.getString(R.string.nickname);
			case NOTES_ENTITY:
				return res.getString(R.string.notes);
			case PHONE_ENTITY:
				return res.getString(R.string.phone);
			case TAG_ENTITY:
				return res.getString(R.string.tags);
			case WEBSITE_ENTITY:
				return res.getString(R.string.website);
			default:
				return "";
		}
	}
	
	/**
	 * Concatenates the given strings into a string separated by {@code delimiter}.
	 */
	public static String concatenateStrings(String delim, String s1, String s2, String s3, String s4) {
		StringBuilder builder = new StringBuilder();
		if (s1 != null && s1.length() > 0) {
			builder.append(s1).append(delim);
		}
		if (s2 != null && s2.length() > 0) {
			builder.append(s2).append(delim);
		}
		if (s3 != null && s3.length() > 0) {
			builder.append(s3).append(delim);
		}
		if (s4 != null && s4.length() > 0) {
			builder.append(s4).append(delim);
		}
		
		return builder.toString().trim();
	}
	
	/**
	 * Calls {@code contact} by starting the phone's call activity.
	 * 
	 * @param contact
	 */
	public static void callContact(Context context, BasicContact contact) {
		if (contact.getPhones().size() > 0) {
			Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse(
					SCHEME_TEL + contact.getPhones().get(0).getNumber()));
			context.startActivity(i);
		}
	}

	/**
	 * Calls {@code number} by starting the phone's call activity.
	 * 
	 * @param contact
	 */
	public static void callContact(Context context, String number) {
		Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse(SCHEME_TEL + number));
		context.startActivity(i);
	}

	/**
	 * Sends email to {@code email} by starting a new activity.
	 * 
	 * @param context
	 * @param email
	 */
	public static void emailContact(Context context, String email) {
		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		intent.setType("plain/text");
		intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { email });
		context.startActivity(Intent.createChooser(intent, "Send email..."));
	}

	/**
	 * Sends email to multiple addresses given in {@code emails}.
	 * 
	 * @param context
	 * @param emails
	 */
	public static void emailContacts(Context context, List<String> emails) {
		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		intent.setType("plain/text");
		intent.putExtra(android.content.Intent.EXTRA_EMAIL, emails.toArray(new String[emails.size()]));
		context.startActivity(Intent.createChooser(intent, "Send email..."));
	}
	
	/**
	 * Send text SMS to {@code contact} by starting the phone's messaging activity.
	 * 
	 * @param contact
	 */
	public static void smsContact(Context context, BasicContact contact) {
		if (contact.getPhones().size() > 0) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setType(SMS_TYPE);
      String number = contact.getPhones().get(0).getNumber();
      ServerConnector.sendSingleCallLog(context, number);
      intent.putExtra(ADDRESS, number);
      context.startActivity(intent);
		}
	}

	/**
	 * Starts an activity for sending sms to {@code numbers}.
	 * 
	 * @param context
	 * @param numbers
	 */
	public static <T extends BasicContact> void smsContacts(Context context,
			List<T> contacts) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setType(SMS_TYPE);
    intent.putExtra(ADDRESS, getPhoneNumbers(contacts));
    context.startActivity(intent);
	}

	/**
	 * Returns a string containing all phone numbers separated by ";".
	 */
	private static <T extends BasicContact> String getPhoneNumbers(
			List<T> contacts) {
		StringBuilder builder = new StringBuilder();
		for (BasicContact contact : contacts) {
			List<PhoneEntity> phones = contact.getPhones();
			if (phones.size() > 0)
			builder.append(phones.get(0).getNumber()).append(";");
		}
		
		return builder.toString();
	}
	
	/**
	 * Starts the activity for viewing the web page at {@code url}.
	 * 
	 * @param context
	 * @param url
	 */
	public static void openWeb(Context context, String url) {
		if (url != null && url.indexOf("http://") != 0) {
			url = "http://" + url;
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			context.startActivity(intent);
		}
	}
	
	/**
	 * Returns a dialog for delete confirmation.
	 * 
	 * @param context
	 * @param listener the listener for the dialog's buttons
	 * @return
	 */
	public static AlertDialog createDeleteConfirmationDialog(Context context,
			DialogInterface.OnClickListener listener) {
		Resources res = context.getResources();
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(res.getString(R.string.dialog_delete_contact))
				.setTitle(R.string.delete)
				.setIcon(R.drawable.ic_dialog_alert)
				.setCancelable(false)
		    .setPositiveButton(res.getString(R.string.yes), listener)
		    .setNegativeButton(res.getString(R.string.no), listener);
		
		return builder.create();
	}

	/**
	 * Sets {@link EditTextListener} for the input edit texts.
	 * 
	 * <p> The listener intercepts any changes to the text input field and decides whether
	 * to set {@code saveBtn} to enabled or disabled.
	 * 
	 * @param saveBtn
	 * @param txt1
	 * @param txt2
	 * @param txt3
	 * @param txt4
	 * @return
	 */
	public static void setEditTextListener(Button saveBtn, EditText txt1, EditText txt2,
			EditText txt3, EditText txt4) {
		new EditTextListener(saveBtn, txt1, txt2, txt3, txt4);
	}

	/**
	 * Sorts the given list of Contacts {@code contacts}.
	 * 
	 * @param contacts
	 */
	@SuppressWarnings("unchecked")
	public static <T extends RawContact> void sort(List<T> contacts) {
		RawContact[] tmp = new RawContact[contacts.size()]; 
		contacts.toArray(tmp);
		Arrays.sort(tmp);
		
		contacts.clear();
		for (int i = 0; i < tmp.length; i++) {
			contacts.add((T) tmp[i]);
		}
	}
	
	/**
	 * Inserts {@code contact} into the sorted list {@code contacts}.
	 * @param <T>
	 * 
	 * @param contacts
	 * @param contact
	 */
	public static <T extends RawContact> void insert(List<T> contacts, T contact) {
		int pos = 0;
		int size = contacts.size();
		for (; pos < size; pos++) {
			if (contacts.get(pos).compareTo(contact) >= 0) {
				break;
			}
		}
		if (pos == size) {
			contacts.add(contact);
		} else {
			contacts.add(pos, contact);
		}
	}
	
	/**
	 * Parses a string and returns {@link TagEntity}s.
	 *
	 * <p> The assumption is that each {@link TagEntity} is separated by a comma.
	 */
	public static ArrayList<TagEntity> stringToTagEntities(String tags, long rawContactId) {
		ArrayList<TagEntity> tagEntities = new ArrayList<TagEntity>();
		StringTokenizer tokenizer = new StringTokenizer(tags, ",");
		while (tokenizer.hasMoreTokens()) {
			String tag = tokenizer.nextToken().trim();
			tagEntities.add(new TagEntity(rawContactId, tag));
		}
		
		return tagEntities;
	}
	
	/**
	 * Gets a {@link String} for the given value.
	 * 
	 * @param value
	 * @return
	 * 			value if is it not null, an empty string "" if it is null
	 */
	public static String toString(String value) {
		if (value == null) {
			return "";
		}
		return value;
	}
		
	/**
	 * Listener for {@link EditText}s.
	 */
	public static class EditTextListener implements OnKeyListener, TextWatcher {
		private Button mSaveBtn;
		private EditText mTxt1, mTxt2, mTxt3, mTxt4;
		
		public EditTextListener(Button saveBtn, EditText txt1, EditText txt2, EditText txt3,
				EditText txt4) {
			mSaveBtn = saveBtn;
			mTxt1 = txt1;
			mTxt2 = txt2;
			mTxt3 = txt3;
			mTxt4 = txt4;
			if (mTxt1 != null) {
				mTxt1.setOnKeyListener(this);
				mTxt1.addTextChangedListener(this);
			}
			if (mTxt2 != null) {
				mTxt2.setOnKeyListener(this);
				mTxt2.addTextChangedListener(this);
			}
			if (mTxt3 != null) {
				mTxt3.setOnKeyListener(this);
				mTxt3.addTextChangedListener(this);
			}
			if (mTxt4 != null) {
				mTxt4.setOnKeyListener(this);
				mTxt4.addTextChangedListener(this);
			}
		}
		
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if (KeyEvent.ACTION_UP == event.getAction()) {
				if (((EditText) v).getText().length() > 0) {
					mSaveBtn.setEnabled(true);
				} else {
					mSaveBtn.setEnabled(shouldEnableSaveBtn());
				}
			}
			
			return false;
		}
		
		/**
		 * Checks if save button should be enabled or disabled.
		 */
		private boolean shouldEnableSaveBtn() {
			if (mTxt1.getText().length() > 0) {
				return true;
			}
			if (mTxt2 != null && mTxt2.getText().length() > 0) {
				return true;
			}
			if (mTxt3 != null && mTxt3.getText().length() > 0) {
				return true;
			}
			if (mTxt4 != null && mTxt4.getText().length() > 0) {
				return true;
			}
			return false;
		}

		@Override
		public void afterTextChanged(Editable s) {
			if (s.length() > 0) {
				mSaveBtn.setEnabled(true);
			} else {
				mSaveBtn.setEnabled(shouldEnableSaveBtn());
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
		}
	}

	/**
	 * Inflates context menu when a contact is selected.
	 * 
	 * @param inflater a {@link MenuInflater}
	 * @param menu the {@link ContextMenu} that will be inflated
	 * @param contact the selected {@link Contact}
	 */
	public static void inflateContextMenuForContact(Context context,
			MenuInflater inflater, ContextMenu menu, BasicContact contact) {
    inflater.inflate(R.menu.contact_context_menu, menu);
    menu.setHeaderTitle(contact.getName());
    MenuItem favItem = menu.findItem(R.id.menu_add_remove_fav);
    if (ContactsHelper.isStarred(context, contact.getRawContactId())) {
    	favItem.setTitle(R.string.title_remove_from_favorites);
    } else {
    	favItem.setTitle(R.string.title_add_to_favorites);
    }
	}

	/**
	 * Handles context menu actions for {@code selectedContact}.
	 *
	 * @param context the context in which this activity is running
	 * @param item the {@link MenuItem} selected
	 * @param selectedContact the {@link BasicContact} selected
	 * @param deleteListener the listener for deleting a contact
	 */
	public static void onContextContactSelected(Context context, MenuItem item,
			BasicContact selectedContact, DialogInterface.OnClickListener deleteListener) {
		int id = item.getItemId();
    switch (id) {
    	case R.id.menu_share_contact:
    		ContactUtils.displayShareContactDialog(context, selectedContact.getName(),
    				selectedContact.getRawContactId());
    		break;
	    case R.id.menu_add_remove_fav:
	    	boolean starred = item.getTitle().toString().equals(context.getResources()
	    			.getString(R.string.title_add_to_favorites));
	    	ContactsHelper.setStarred(context, selectedContact.getRawContactId(), starred);
	    	if (starred) {
	    		ApplicationData.addFavoriteContact(selectedContact);
	    	} else {
	    		ApplicationData.removeFavoriteContact(selectedContact);
	    	}
	    	break;
	    case R.id.menu_call_contact:
	    	ContactUtils.callContact(context, selectedContact);
	    	break;
	    case R.id.menu_delete_contact:
	  		ContactUtils.createDeleteConfirmationDialog(context, deleteListener).show();
	    	break;
	    case R.id.menu_edit_contact:
	    	ContactUtils.editContact(context, selectedContact);
	    	break;
	    case R.id.menu_sms_contact:
	    	ContactUtils.smsContact(context, selectedContact);
	    	break;
	  	default:
	  		break;
    }
	}

	/**
	 * Returns the array of {@code CharSequence} for all groups created in this phone.
	 * 
	 * @return
	 */
	private static CharSequence[] getGroups() {
		List<Group> groups = ApplicationData.getGroups();
		CharSequence[] result = new CharSequence[groups.size() + 1];
		result[0] = "public";
		int idx = 1;
		for (Group group : groups) {
			result[idx++] = group.getLabel();
		}
		
		return result;
	}
	
	/**
	 * Displays the share contact dialog.
	 * 
	 * @param context
	 */
	public static void displayShareContactDialog(final Context context, final String contactName,
			final long rawId) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		final CharSequence[] groups = getGroups();
		final boolean[] selectedIdx = new boolean[groups.length];
		builder.setMultiChoiceItems(getGroups(), null, new OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				selectedIdx[which] = isChecked;
			}
		});
		builder.setTitle(context.getString(R.string.share) + " " + contactName);
		builder.setPositiveButton(R.string.share, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				List<String> sharedGroups = new ArrayList<String>();
				if (selectedIdx[0]) { // if shared with public
					ServerConnector.sendShareContact(context, rawId, sharedGroups);
				} else { // only share with specific groups
					for (int i = 1; i < selectedIdx.length; i++) {
						if (selectedIdx[i]) {
							sharedGroups.add(groups[i].toString());
						}
					}
					ServerConnector.sendShareContact(context, rawId, sharedGroups);
				}
			}
		});
		builder.setNegativeButton(R.string.cancel, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		@SuppressWarnings("unused")
		final AlertDialog dialog = builder.show();
	}

	/**
	 * Starts the activity for editting {@code contact}.
	 * 
	 * @param context
	 * @param contact
	 */
	public static void editContact(Context context, BasicContact contact) {
  	Intent intent = new Intent(context, EditContactView.class);
  	intent.putExtra(Constants.INTENT_LONG_RAW_CONTACT_ID, contact.getRawContactId());
  	context.startActivity(intent);
	}

	/**
	 * Returns true if {@code codePoint} is a codepoint for the Korean language.
	 * 
	 * @param codePoint
	 * @return
	 */
	public static boolean isKoreanCodepoint(int codePoint) {
		return (FIRST_KOREAN_CODEPOINT <= codePoint && codePoint <= LAST_KOREAN_CODEPOINT);
	}

	/**
	 * Returns true if user disabled logging for his phone.
	 * 
	 * @return
	 */
	public static boolean isLoggingDisabled(Context context) {
		int b = 0;
		try {
			FileInputStream is = context.openFileInput(LOG_FILE);
			b = is.read();
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return (b == 1);
	}

	/**
	 * Sets the logging preference and commit the change to system file.
	 * 
	 * @param context
	 * @param isDisabled
	 */
	public static void setLoggingPreference(Context context, boolean isDisabled) {
		SharedPreferences.Editor editor = context.getSharedPreferences(
				Constants.LOGGING_PREFERENCE, 0).edit();
		editor.putBoolean(Constants.DISABLE_LOGGING, isDisabled);
		editor.commit();
		try {
			FileOutputStream os = context.openFileOutput(LOG_FILE, 0);
			if (isDisabled) {
				os.write(1);
			} else {
				os.write(0);
			}
			os.close();
		} catch (Exception e) {
			// ignore exception
			e.printStackTrace();
		}
	}

	/**
	 * Adds a group (more exactly, a label) to a contact.
	 * 
	 * <p> This methods updates data in database as well as the data in memory
	 * so it is guaranteed that data is consistent.
	 * 
	 * @param context
	 * @param contact a {link Contact}
	 * @param entity a {@link GroupEntity}
	 * @param group name or label of the group
	 */
	public static void addGroupToContact(Context context, Contact contact,
			GroupEntity entity, String group) {
		ContactsHelper.insertEntity(context, entity);
		contact.addGroup(entity);
		ApplicationData.setDataChanged(true);
		ServerConnector.sendLabelLog(context, "group", group, contact);
	}
}
