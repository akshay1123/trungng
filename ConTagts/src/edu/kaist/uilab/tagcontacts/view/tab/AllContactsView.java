// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.view.tab;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import edu.kaist.uilab.tagcontacts.Constants;
import edu.kaist.uilab.tagcontacts.ContactUtils;
import edu.kaist.uilab.tagcontacts.R;
import edu.kaist.uilab.tagcontacts.ServerConnector;
import edu.kaist.uilab.tagcontacts.database.ApplicationData;
import edu.kaist.uilab.tagcontacts.database.ContactsHelper;
import edu.kaist.uilab.tagcontacts.database.DBHelper;
import edu.kaist.uilab.tagcontacts.model.Contact;
import edu.kaist.uilab.tagcontacts.view.widget.DetailedContactView;
import edu.kaist.uilab.tagcontacts.view.widget.NewContactView;

/**
 * View for displaying all contacts.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class AllContactsView extends Activity {
	
	@SuppressWarnings("unused")
	private static final String TAG = "AllContactsView";
	private static final String FIRST_VISIBLE = "AllContactsView.FirstVisible";
	private static final String KOREAN_LOCALE = "kor";
	private static final int LAST_KOREAN_CODEPOINT = 52203;
	
	private DBHelper mDbHelper;
	private EditText mSearch;
	private ListView mListView;
	private String[] mEnglishAlphabet;
	private String[] mKorean;
	private String[] mKoreanAlphabet;
	private boolean mIsKorean;
	private Editor mEditor;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tab_all);
    // sends new call logs every time the user runs this app
    ServerConnector.sendCallLogs(this);

		mEnglishAlphabet = getResources().getStringArray(R.array.english_alphabet);
		mIsKorean = KOREAN_LOCALE.equals(getResources().getConfiguration().locale.getISO3Language());
		if (mIsKorean) {
			mKorean = getResources().getStringArray(R.array.korean);
			mKoreanAlphabet = getResources().getStringArray(R.array.korean_alphabet);
		}
		mEditor = getSharedPreferences(Constants.SAVED_STATES_PREFERENCE, 0).edit();
		
		// add new contact
		ImageButton addBtn = (ImageButton) findViewById(R.id.btn_add);
		addBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
	    	Intent i = new Intent(AllContactsView.this, NewContactView.class);
	    	startActivity(i);
			}
		});
		
		// initiate search section
		mSearch = (EditText) findViewById(R.id.txt_search);
		mSearch.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_UP) {
					performSearch();
				}
				return false;
			}
		});
		mSearch.addTextChangedListener(new SearchWatcher());
		
		ImageButton searchBtn = (ImageButton) findViewById(R.id.btn_search);
		searchBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				performSearch();
			}
		});
		
		// get data
		mDbHelper = new DBHelper(this);
		ApplicationData.setGroups(mDbHelper.getAllGroups());
		ApplicationData.setContacts(ContactsHelper.getContacts(this));
		// set layout
		mListView = (ListView) findViewById(R.id.list_all_contact);
  	mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				Intent intent = new Intent(AllContactsView.this, DetailedContactView.class);
				intent.putExtra(Constants.INTENT_LONG_RAW_CONTACT_ID,
						((Contact) mListView.getAdapter().getItem(position)).getRawContactId());
				startActivity(intent);
			}
		});
  	
		registerForContextMenu(mListView);
		invalidateListView(getAdapterForContacts(ApplicationData.getContacts()));
		mSearch.clearFocus();
	}

	@Override
	public void onPause() {
		super.onPause();
		
		mEditor.putInt(FIRST_VISIBLE, mListView.getFirstVisiblePosition());
		mEditor.commit();
	}
	
  @Override
  public void onResume() {
  	super.onResume();
  	
 		if (ApplicationData.isDataChanged()) {
 			invalidateListView(getAdapterForContacts(ApplicationData.getContacts()));
 		}
  }
  
  @Override
  public void onDestroy() {
  	super.onDestroy();
  	mDbHelper.close();
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
  	MenuInflater inflater = getMenuInflater();
  	inflater.inflate(R.menu.all_contacts_menu, menu);
  	
  	return true;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
  	super.onOptionsItemSelected(item);
  	if (item.getItemId() == R.id.menu_new_contact) {
    	Intent i = new Intent(this, NewContactView.class);
    	startActivity(i);
  	}
  	
  	return true;
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo info) {
    super.onCreateContextMenu(menu, v, info);
    MenuInflater inflater = getMenuInflater();
    Contact contact = (Contact) mListView.getItemAtPosition(
    		((AdapterContextMenuInfo) info).position);
    ContactUtils.inflateContextMenuForContact(this, inflater, menu, contact);
  }
  
  @Override
  public boolean onContextItemSelected(MenuItem item) {
  	// save first visible position
  	mEditor.putInt(FIRST_VISIBLE, mListView.getFirstVisiblePosition()).commit();
  	
    final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    Contact selectedContact = (Contact) mListView.getItemAtPosition(info.position);
    ContactUtils.onContextContactSelected(this, item, selectedContact,
    		new DeleteListener(selectedContact));
    
    return true;
  }

	/**
	 * Returns a {@link SeparatedListAdapter} for the list of contacts {@code contacts}.
	 * 
	 * @param contacts
	 * @return
	 */
	private SeparatedListAdapter<Contact> getAdapterForContacts(List<Contact> contacts) {
		SeparatedListAdapter<Contact> res = new SeparatedListAdapter<Contact>(this,
				R.layout.list_header_right);
		ArrayList<Contact> list;
		if (mIsKorean) { // If locale language is Korean, adds Korean alphabet
			for (int i = 0; i < mKoreanAlphabet.length; i++) {
				list = getKoreanContactsWithTitle(contacts, i);
				if (!list.isEmpty()) {
					res.addSection(mKoreanAlphabet[i], new GroupAndTagContactAdapter(this, list,
							ApplicationData.getGroups()));
				}
			}
		}
		for (int i = 0; i < mEnglishAlphabet.length; i++) {
			 list = getContactsWithTitle(contacts, i);
			if (!list.isEmpty()) {
				res.addSection(mEnglishAlphabet[i], new GroupAndTagContactAdapter(this, list,
						ApplicationData.getGroups()));
			}
		}
		// remaining contacts (which do not start with one of the letters in TITLES
		list = new ArrayList<Contact>();
		for (Contact contact : contacts) {
			if (!isInAlphabet(contact.getName().substring(0, 1).toUpperCase())) {
			 list.add(contact);
			}	
		}
		if (!list.isEmpty()) {
			res.addSection("#", new GroupAndTagContactAdapter(this, list,
					ApplicationData.getGroups()));
		}
		
		return res;
	}

	/**
	 * Returns true if c is in the local alphabet.
	 * 
	 * @param c a string of length 1
	 */
	private boolean isInAlphabet(String c) {
		// English alphabet is available for both international & Korean
		if ('A' <= c.charAt(0) && c.charAt(0) <= 'Z') {
			return true;
		}
		if (mIsKorean && ContactUtils.isKoreanCodepoint(c.codePointAt(0))) {
			return true;
		}
		
		return false;
	}
  
	/**
	 * Gets all contact in the give {@code list} whose initial character of its
	 * name is {@code title}.
	 * 
	 * @return
	 * 		 	the sub-list of contacts which could be empty if no contact has its
	 * 			first name starts with {@code title}
	 */
	private ArrayList<Contact> getContactsWithTitle(List<Contact> list, int idx) {
		ArrayList<Contact> contacts = new ArrayList<Contact>();
		for (Contact iContact : list) {
			String s = iContact.getName().substring(0, 1).toUpperCase();
			if (mEnglishAlphabet[idx].equals(s)) {
				contacts.add(iContact);
			}
		}
		
		return contacts;
	}

	/**
	 * Gets all korean contacts in the give {@code list} whose initial character of its
	 * name is {@code title}.
	 * 
	 * @return
	 * 		 	the sub-list of contacts which could be empty if no contact has its
	 * 			first name starts with {@code title}
	 */
	private ArrayList<Contact> getKoreanContactsWithTitle(List<Contact> list, int idx) {
		ArrayList<Contact> contacts = new ArrayList<Contact>();
		for (Contact iContact : list) {
			String s = iContact.getName().substring(0, 1).toUpperCase();
			if (equals(s, idx)) {
				contacts.add(iContact);
			}
		}
		
		return contacts;
	}
	
	/**
	 * Returns true if {@code s} is the Korean alphabet character at {@code idx}.
	 * This method deals with the Korean alphabet.
	 */
	private boolean equals(String s, int idx) {
		if (idx >= mKorean.length) {
			return false;
		}
		if (idx < mKorean.length - 1) {
			return (mKorean[idx].codePointAt(0) <= s.codePointAt(0)
					&& s.codePointAt(0) < mKorean[idx + 1].codePointAt(0));
		} else {
			return (mKorean[idx].codePointAt(0) <= s.codePointAt(0)
					&& s.codePointAt(0) <= LAST_KOREAN_CODEPOINT);
		}
	}

	private void performSearch() {
		String query = mSearch.getText().toString();
		if (query.length() > 0) {
			HashSet<Long> rawContactIds = ContactsHelper.getRawContactIds(this, query);
			rawContactIds.addAll(ContactsHelper.getRawContactIds(this,
					mDbHelper.queryContains(query)));
			ArrayList<Contact> result = new ArrayList<Contact>();
			// we already read all raw contacts so we should take advantage of that to
			// avoid reading again
			List<Contact> contacts = ApplicationData.getContacts();
			for (Contact iContact : contacts) {				
				if (rawContactIds.contains(iContact.getRawContactId())) {
					result.add(iContact);
				}
			}
			invalidateListView(new GroupAndTagContactAdapter(this, result, ApplicationData.getGroups()));
		} else {
			// back to default (no search)
			invalidateListView(getAdapterForContacts(ApplicationData.getContacts()));
		}
	}
	
  /**
   * Invalidates the list view of this activity with the new {@code adapter}.
   * @param adapter
   */
  private void invalidateListView(BaseAdapter adapter) {
  	// changing the adapter causes list view to invalidate and its data to be up-to-date
		ApplicationData.setDataChanged(false);
  	mListView.setAdapter(adapter);
  	mListView.invalidate();
  	int pos = getSharedPreferences(Constants.SAVED_STATES_PREFERENCE, 0).getInt(
  			FIRST_VISIBLE, 0);
  	if (pos > adapter.getCount()) {
  		pos = adapter.getCount() - 1;
  	}
  	mListView.setSelection(pos);
  }

  /**
   * A text watcher for performing search.
   */
  private class SearchWatcher implements TextWatcher {
		@Override
		public void afterTextChanged(Editable s) {
			performSearch();
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
   * Listener for the delete confirmation dialog.
   */
  private class DeleteListener implements DialogInterface.OnClickListener {
  	private Contact mContact;
  	
  	/**
  	 * Constructor
  	 */
  	public DeleteListener(Contact contact) {
  		mContact = contact;
  	}
  	
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					ContactsHelper.deleteRawContact(AllContactsView.this,
							mContact.getRawContactId());
					ApplicationData.removeContact(mContact);
		    	invalidateListView(getAdapterForContacts(ApplicationData.getContacts()));
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					dialog.cancel();
					break;
			}
		}
  }
}
