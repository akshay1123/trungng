// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.view.tab;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.SharedPreferences.Editor;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import edu.kaist.uilab.tagcontacts.Constants;
import edu.kaist.uilab.tagcontacts.ContactUtils;
import edu.kaist.uilab.tagcontacts.R;
import edu.kaist.uilab.tagcontacts.database.ApplicationData;
import edu.kaist.uilab.tagcontacts.database.ContactsHelper;
import edu.kaist.uilab.tagcontacts.database.DBHelper;
import edu.kaist.uilab.tagcontacts.model.Contact;
import edu.kaist.uilab.tagcontacts.model.Group;
import edu.kaist.uilab.tagcontacts.model.GroupEntity;
import edu.kaist.uilab.tagcontacts.view.widget.DetailedContactView;
import edu.kaist.uilab.tagcontacts.view.widget.GroupEditorView;

/**
 * View for displaying all contacts in groups.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class GroupsView extends Activity {

	@SuppressWarnings("unused")
	private static final String TAG = "GroupViews";

	private static final String FIRST_VISIBLE = "GroupsView.FirstVisible";
	private static final String LAST_SELECTED_GROUP = "GroupView.LastSelectedGroup";
	private static final int ALL_POSITION = -2;
	private static final int NONE_POSITION = -1;
	
	private ListView mGroupView;
	private ListView mContactView;
	private TouchEventListener mTouchListener;
	// the view that was touched and potentially the start of a drag & drop
	private TextView mTouchedView;
	private TextView mLastSelectedGroup;
	private TextView mGroupAll, mGroupNone;
	
	private List<Group> mGroups;
	private Editor mEditor;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tab_groups);
		mEditor = getSharedPreferences(Constants.SAVED_STATES_PREFERENCE, 0).edit();
		
		TextView newGroup = (TextView) findViewById(R.id.txt_group_add);
		newGroup.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
	  		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
	  		final View window = inflater.inflate(R.layout.new_group_dialog, null);
	  		AlertDialog.Builder builder = new AlertDialog.Builder(GroupsView.this);
	  		builder.setView(window).setTitle(getString(R.string.title_new_group));
	  		builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
	  		builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String groupName = ((EditText) window.findViewById(
								R.id.txt_group_name)).getText().toString();
						if (groupName.length() > 0) {
							DBHelper helper = new DBHelper(GroupsView.this);
							helper.insertGroup(new Group(groupName));
							ApplicationData.setGroups(helper.getAllGroups());
							helper.close();
							invalidate();
						}	
					}
				});
	  		builder.show();
			}
		});
		
		mGroupAll = (TextView) findViewById(R.id.txt_group_all);
		mGroupAll.setOnClickListener(new DefaultGroupsListener());
		mGroupNone = (TextView) findViewById(R.id.txt_group_none);
		mGroupNone.setOnClickListener(new DefaultGroupsListener());
		
		mTouchListener = new TouchEventListener();

		mGroupView = (ListView) findViewById(R.id.list_groups);
		mContactView = (ListView) findViewById(R.id.list_group_contacts);
		mContactView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
		  	mEditor.putInt(FIRST_VISIBLE, mContactView.getFirstVisiblePosition()).commit();

				Intent intent = new Intent(GroupsView.this, DetailedContactView.class);
				intent.putExtra(Constants.INTENT_LONG_RAW_CONTACT_ID,
						((Contact) mContactView.getAdapter().getItem(position)).getRawContactId());
				startActivity(intent);
			}	
		});
		registerForContextMenu(mContactView);
	}
	
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
  	MenuInflater inflater = getMenuInflater();
  	inflater.inflate(R.menu.groups_menu, menu);
  	
  	return true;
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo info) {
  	super.onCreateContextMenu(menu, v, info);
  	MenuInflater inflater = getMenuInflater();
  	Contact contact = (Contact) mContactView.getItemAtPosition(
  			((AdapterContextMenuInfo) info).position);
  	ContactUtils.inflateContextMenuForContact(GroupsView.this, inflater, menu,
  			contact);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
  	mEditor.putInt(FIRST_VISIBLE, mContactView.getFirstVisiblePosition()).commit();
  	
    final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    Contact selectedContact = (Contact) mContactView.getItemAtPosition(info.position);
    ContactUtils.onContextContactSelected(GroupsView.this, item, selectedContact,
    		new DeleteListener(selectedContact));
    
    return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
  	super.onOptionsItemSelected(item);
  	
  	if (item.getItemId() == R.id.menu_edit_groups) {
    	Intent i = new Intent(this, GroupEditorView.class);
    	startActivity(i);
  	} else {
  		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
  		AlertDialog.Builder builder = new AlertDialog.Builder(this);
  		builder.setView(inflater.inflate(R.layout.help_dialog, null))
  				.setTitle(getString(R.string.title_help));
  		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
  		builder.create().show();
  	}
  	return true;
  }

  @Override
  public void onResume() {
  	super.onResume();
  
  	invalidate();
  }
  
  /**
   * Invalidates the screen after the edit group activity.
   * 
   * <p> Since it is very likely that groups will be changed after the activity
   * and the operation cost for getting groups are not expensive, it is safe to
   * retrieve the groups every time the user comes back from the edit activity.
   */
	private void invalidate() {
		mGroups = ApplicationData.getGroups();
		invalidateGroupView(mGroups);
		int selectedGroup = getLastSelectedGroup();
		if (selectedGroup >= 0) {
			mGroupView.setSelection(selectedGroup);
			invalidateContactView(new GroupOnlyContactAdapter(GroupsView.this,
					getContactsOfGroup(mGroups.get(selectedGroup).getId()), mGroups));
		} else if (selectedGroup == ALL_POSITION) { // all contacts
			mLastSelectedGroup = mGroupAll;
			mGroupAll.setSelected(true);
			invalidateContactView(new GroupOnlyContactAdapter(GroupsView.this,
					ApplicationData.getContacts(), mGroups));
		} else { // none contacts
			mLastSelectedGroup = mGroupNone;
			mGroupNone.setSelected(true);
			invalidateContactView(new GroupOnlyContactAdapter(GroupsView.this,
					getNoneGroupContacts(), mGroups));
		}
	}
	
	/**
	 * Invalidates the contact view with new {@code adapter}.
	 * 
	 * @param adapter
	 */
	private void invalidateContactView(GroupOnlyContactAdapter adapter) {
		mContactView.setAdapter(adapter);
		mContactView.invalidate();
  	mContactView.setSelection(getLastVisibleContact(adapter));
	}

  /**
   * Gets position of the last visible view (contact) for the currently selected group.
   * 
   * @param adapter
   * @return
   */
  private int getLastVisibleContact(GroupOnlyContactAdapter adapter) {
  	int pos = getSharedPreferences(Constants.SAVED_STATES_PREFERENCE, 0).getInt(
  			FIRST_VISIBLE, 0);
  	if (pos > adapter.getCount()) {
  		pos = adapter.getCount() - 1;
  	}
  	return pos;
  }
  
	/**
	 * Invalidates the left panel which contains the list of {@code groups}.
	 */
	private void invalidateGroupView(List<Group> groups) {
		mGroupAll.setSelected(false);
		mGroupNone.setSelected(false);
		
		ArrayList<String> allGroups = new ArrayList<String>();
		// add groups from the database
		for (Group iGroup : groups) {
			allGroups.add(iGroup.getLabel());
		}
		
		ArrayAdapter<String> groupAdapter = new GroupListAdapter<String>(this,
				R.layout.group_item, allGroups, getLastSelectedGroup());
		GroupItemClickListener listener = new GroupItemClickListener();
		mGroupView.setAdapter(groupAdapter);
		mGroupView.setOnItemClickListener(listener);
		mGroupView.setOnItemLongClickListener(listener);
		mGroupView.setOnTouchListener(mTouchListener);
		mGroupView.invalidate();
	}

  /**
   * Gets position of the last visible view (contact) for the currently selected group.
   * 
   * @param adapter
   * @return
   */
  private int getLastSelectedGroup() {
  	int pos = getSharedPreferences(Constants.SAVED_STATES_PREFERENCE, 0).getInt(
  			LAST_SELECTED_GROUP, ALL_POSITION);
  	if (pos >= mGroups.size()) {
  		pos = ALL_POSITION;
  	}
  	
  	return pos;
  }
	
	/**
	 * Gets all contacts that belong to the group with given {@code id}.
	 */
	private List<Contact> getContactsOfGroup(long id) {
		List<Contact> list = new ArrayList<Contact>();
		List<Contact> contacts = ApplicationData.getContacts();
		for (Contact iContact : contacts) {
			if (iContact.hasGroup(id)) {
				list.add(iContact);
			}
		}
		
		return list;
	}

	/**
	 * Class for handling the events related to group items.
	 */
	private class GroupItemClickListener implements OnItemClickListener,
			OnItemLongClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			invalidateLastSelectedGroup();
			v.setBackgroundResource(R.color.group_selected_bg);
			mLastSelectedGroup = (TextView) v;
			
			GroupOnlyContactAdapter adapter = new GroupOnlyContactAdapter(GroupsView.this,
					getContactsOfGroup(mGroups.get(position).getId()), mGroups);
			mContactView.setAdapter(adapter);
			mContactView.invalidate();
			mEditor.putInt(FIRST_VISIBLE, 0).commit(); // previously visible contact becomes invalid
			mEditor.putInt(LAST_SELECTED_GROUP, position).commit();
		}

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View v, final int position,
				long id) {
			AlertDialog.Builder builder = new AlertDialog.Builder(GroupsView.this);
			builder.setTitle(((TextView) v).getText());
			builder.setItems(R.array.group_item_context, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					long groupId = mGroups.get(position).getId();
					switch (which) {
						case 0:
							ContactUtils.smsContacts(GroupsView.this, getContactsOfGroup(groupId));
							break;
						case 1:
							ContactUtils.emailContacts(GroupsView.this, ContactsHelper.getEmails(
									GroupsView.this, getContactsOfGroup(groupId)));
							break;
						default:
							displayEditGroupDialog(mGroups.get(position));
							break;
					}
				}
			});
			builder.show();
			
			return true;
		}
	}
	
	/**
	 * Displays the dialog for adding contacts to group.
	 * 
	 * @param group
	 */
	private void displayEditGroupDialog(final Group group) {
		AlertDialog.Builder builder = new AlertDialog.Builder(GroupsView.this);
		builder.setTitle(group.getLabel());
		final List<Contact> contacts = ApplicationData.getContacts();
		final CharSequence contactNames[] = new CharSequence[contacts.size()];
		final boolean checked[] = new boolean[contactNames.length];
		Contact contact;
		final long groupId = group.getId();
		for (int i = 0; i < contactNames.length; i++) {
			contact = contacts.get(i);
			contactNames[i] = contact.getName();
			checked[i] = contact.hasGroup(groupId);
		}
		builder.setMultiChoiceItems(contactNames, checked, new OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				Contact contact = contacts.get(which);
				long rawContactId = contact.getRawContactId();
				GroupEntity entity = new GroupEntity(rawContactId, groupId);
				if (isChecked) {
					ContactUtils.addGroupToContact(GroupsView.this, contact, entity, group.getLabel());
				} else {
					// delete contact from group
					ContactsHelper.deleteEntity(GroupsView.this, entity);
					contact.removeGroup(entity);
					ApplicationData.setDataChanged(true);
				}
			}
		});
		builder.setNeutralButton(R.string.yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				invalidate();
			}
		});
		@SuppressWarnings("unused")
		final AlertDialog dialog = builder.show();
	}
	
	/**
	 * 
	 */
	private class DeleteListener implements DialogInterface.OnClickListener {
		private Contact mContact;
		
		public DeleteListener(Contact contact) {
			mContact = contact;
		}
		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					ContactsHelper.deleteRawContact(GroupsView.this, mContact.getRawContactId());
					ApplicationData.removeContact(mContact);
					GroupOnlyContactAdapter adapter =
						(GroupOnlyContactAdapter) mContactView.getAdapter();
					adapter.remove(mContact);
					invalidateContactView(adapter);
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					dialog.cancel();
					break;
			}
		}	
	}

	/**
	 * Adapter for the list of group names (labels).
	 * @param <T>
	 */
	private class GroupListAdapter<T> extends ArrayAdapter<String> {
		private static final int INVALID_POSITION = -1;
		private int mLastPosition;
		
		public GroupListAdapter(Context context, int resource, List<String> values, int position) {
			super(context, resource, values);
			mLastPosition = position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView v = (TextView) super.getView(position, convertView, parent);
			v.setTextColor(mGroups.get(position).getColor());
			v.setTag(position);
			v.setOnTouchListener(mTouchListener);
			if (position == mLastPosition) {
				v.setBackgroundResource(R.color.group_selected_bg);
				mLastSelectedGroup = v;
				mLastPosition = INVALID_POSITION; // use only once
			}
			return v;
		}
	}

	/**
	 * Class for handling the touch event.
	 */
	private class TouchEventListener implements OnTouchListener {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();
			switch (action) {
				case MotionEvent.ACTION_DOWN:
					if (v instanceof TextView) {
						mTouchedView = (TextView) v;
					}
					break;
				case MotionEvent.ACTION_UP:
					return handleActionUp(event);
			}
			
			return false;
		}
		
		private boolean handleActionUp(MotionEvent event) {
			if (mTouchedView == null) {
				// the action did not start from a group label, so it is not a click on
				// the group label or a drag & drop
				return true;
			}
			
			// the move started from a group label (so this could be a item click or a drag & drop)
			int pos = -1;
			View view;
			Rect rect;
			
			// the coordinate of event.getX() is relative to groupView in that case
			// which means we need to convert the coordinates
			int x = (int) event.getX() - mContactView.getLeft();
			int y = (int) event.getY() + mGroupView.getTop();
			for (int i = 0; i <= mContactView.getChildCount(); i++) {
				 view = mContactView.getChildAt(i);
				if (view != null) {
					rect = new Rect(view.getLeft(), view.getTop(), view.getRight(),
							view.getBottom());
					if (rect.contains(x, y)) {
						pos = i;
						break;
					}
				}
			}
			
			TextView savedView = mTouchedView;
			mTouchedView = null; // set to null after consuming its value
			if (pos != -1) {
				// start from a label then stop at a contact
				LinearLayout groupRegion = (LinearLayout) mContactView.getChildAt(
						pos).findViewById(R.id.layout_labels_region);
				Contact contact = (Contact) mContactView.getItemAtPosition(
						(Integer) mContactView.getChildAt(pos).getTag());
				Group group = mGroups.get((Integer) savedView.getTag());
				if (!contact.hasGroup(group.getId())) {
					groupRegion.addView(((GroupOnlyContactAdapter) mContactView.getAdapter())
							.inflateTextView(groupRegion, contact, group));
					// update data in database and cache
					ContactUtils.addGroupToContact(GroupsView.this, contact, new GroupEntity(
							contact.getRawContactId(), group.getId()), group.getLabel());
				}	
				return true;
			}
			
			// the action started from a group label but did not stop at any contact
			// so it was a clicked on item (should be handle by onItemClick)
			return false;
		}
	}
	
	/**
	 * Listener for handling the click event of 2 buttons (all, none)
	 */
	private class DefaultGroupsListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			int id = v.getId();
			if (id == R.id.txt_group_all) {
				mEditor.putInt(LAST_SELECTED_GROUP, ALL_POSITION).commit();
				mContactView.setAdapter(new GroupOnlyContactAdapter(GroupsView.this,
						ApplicationData.getContacts(), mGroups));
				mContactView.invalidate();
			} else {
				mEditor.putInt(LAST_SELECTED_GROUP, NONE_POSITION).commit();
				// get all contacts that has no group
				mContactView.setAdapter(new GroupOnlyContactAdapter(GroupsView.this,
						getNoneGroupContacts(), mGroups));
				mContactView.invalidate();
			}
			invalidateLastSelectedGroup();
			v.setSelected(true);
			v.invalidate();
			mLastSelectedGroup = (TextView) v;
		}
	}
	
	/**
	 * Invalidates the background color of last selected group.
	 */
	private void invalidateLastSelectedGroup() {
		if (mLastSelectedGroup != null) {
			if (mLastSelectedGroup == mGroupAll || mLastSelectedGroup == mGroupNone) {
				mLastSelectedGroup.setSelected(false);
				mLastSelectedGroup.invalidate();
			} else {
				mLastSelectedGroup.setBackgroundResource(android.R.color.transparent);
			}
		}
	}
	
	/**
	 * Returns the list of all contacts that has no group.
	 * 
	 * @return
	 */
	private List<Contact> getNoneGroupContacts() {
		List<Contact> noneGroupContacts = new ArrayList<Contact>();
		List<Contact> contacts = ApplicationData.getContacts();
		for (Contact iContact : contacts) {
			if (iContact.getGroups().size() == 0) {
				noneGroupContacts.add(iContact);
			}
		}
		
		return noneGroupContacts;
	}
}
