// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.view.tab;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import edu.kaist.uilab.tagcontacts.Constants;
import edu.kaist.uilab.tagcontacts.ContactUtils;
import edu.kaist.uilab.tagcontacts.R;
import edu.kaist.uilab.tagcontacts.database.ApplicationData;
import edu.kaist.uilab.tagcontacts.database.ContactsHelper;
import edu.kaist.uilab.tagcontacts.model.BasicContact;
import edu.kaist.uilab.tagcontacts.view.widget.DetailedContactView;
import edu.kaist.uilab.tagcontacts.view.widget.NewContactView;

/**
 * View for displaying favorite contacts.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class FavoriteView extends Activity {

	private static final int MAX_FREQUENT_CONTACTS = 5;

	private ListView mListView;
	private List<BasicContact> mFrequentContacts;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tab_favorite);
		
		ApplicationData.setFavoriteContacts(ContactsHelper.getFavoriteContacts(
				FavoriteView.this));
		ContactUtils.sort(ApplicationData.getFavoriteContacts());
		mFrequentContacts = ContactsHelper.getFrequentContacts(FavoriteView.this,
				MAX_FREQUENT_CONTACTS);
		mListView = (ListView) findViewById(R.id.list_favorites);
		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				Intent intent = new Intent(FavoriteView.this,
						DetailedContactView.class);
				intent.putExtra(Constants.INTENT_LONG_RAW_CONTACT_ID,
						((BasicContact) mListView.getAdapter().getItem(position)).getRawContactId());
				;
				startActivity(intent);
			}
		});
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		List<BasicContact> favorites = ApplicationData.getFavoriteContacts();
		if (favorites == null) {
			// needs to read from database if data is gone
			favorites = ContactsHelper.getFavoriteContacts(FavoriteView.this);
			ContactUtils.sort(favorites);
			ApplicationData.setFavoriteContacts(favorites);
		}
		mListView.setAdapter(getListAdapter(favorites, mFrequentContacts));
		mListView.invalidate();
	}
	
	/**
	 * Builds the adapter for the underlying list view of this activity.
	 */
	private SeparatedListAdapter<BasicContact> getListAdapter(
			List<BasicContact> favorites, List<BasicContact> frequents) {
		SeparatedListAdapter<BasicContact> adapter = new SeparatedListAdapter<BasicContact>(
				this, R.layout.list_header_left);
		adapter.addSection(getResources().getString(R.string.favorites),
				new FavoriteContactAdapter(this, favorites));
		adapter.addSection(getResources().getString(R.string.frequent_contacts),
				new FavoriteContactAdapter(this, frequents)); 
		return adapter;
	}
	
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
  	MenuInflater inflater = getMenuInflater();
  	inflater.inflate(R.menu.favorite_contacts_menu, menu);
  	
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
}
