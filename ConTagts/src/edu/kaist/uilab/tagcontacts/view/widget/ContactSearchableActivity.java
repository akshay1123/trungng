package edu.kaist.uilab.tagcontacts.view.widget;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import edu.kaist.uilab.tagcontacts.R;

/**
 * Activity for searching contact.
 * 
 * <p> This activity will be launched when user performs a search with the
 * search dialog.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 *
 */
public class ContactSearchableActivity extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search);
		
		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			performSearch(query);
		}
	}
	
	private void performSearch(String query) {
		TextView view = (TextView) findViewById(R.id.query);
		view.setText(query);
	}
}
