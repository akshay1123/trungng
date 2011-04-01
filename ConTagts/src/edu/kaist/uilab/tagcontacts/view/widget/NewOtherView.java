// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.view.widget;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import edu.kaist.uilab.tagcontacts.R;

/**
 * View for adding other data.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class NewOtherView extends Activity {
	
	@SuppressWarnings("unused")
	private static final String TAG = "NewOtherView";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_other);
			
		// make top bar's title appropriate for this view
		TextView tv = (TextView) findViewById(R.id.top_bar_title);
		tv.setText(R.string.title_add_other);
		
		((Button) findViewById(R.id.btn_bar_right)).setVisibility(View.INVISIBLE);
		
		// set listener for the cancel button
		Button cancel = (Button) findViewById(R.id.btn_bar_left);
		cancel.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
		
		// set listeners for add buttons
		((Button) findViewById(R.id.btn_add_job)).setOnClickListener(
				new AddButtonListener());
		((Button) findViewById(R.id.btn_add_address)).setOnClickListener(
				new AddButtonListener());
		((Button) findViewById(R.id.btn_add_notes)).setOnClickListener(
				new AddButtonListener());
		((Button) findViewById(R.id.btn_add_nickname)).setOnClickListener(
				new AddButtonListener());
		((Button) findViewById(R.id.btn_add_website)).setOnClickListener(
				new AddButtonListener());
		AddButtonListener listener = new AddButtonListener();
		findViewById(R.id.left_btn_add_job).setOnClickListener(listener);
		findViewById(R.id.left_btn_add_address).setOnClickListener(listener);
		findViewById(R.id.left_btn_add_notes).setOnClickListener(listener);
		findViewById(R.id.left_btn_add_nickname).setOnClickListener(listener);
		findViewById(R.id.left_btn_add_website).setOnClickListener(listener);
	}
	
	private class AddButtonListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			int id = v.getId();
			Intent intent = null;
			switch (id) {
				case R.id.btn_add_job:
				case R.id.left_btn_add_job:
					intent = new Intent(v.getContext(), EditJobView.class);
					startActivityForResult(intent, NewContactView.EDIT_JOB_REQUEST);
					return;
				case R.id.btn_add_address:
				case R.id.left_btn_add_address:
					intent = new Intent(v.getContext(), EditAddressView.class);
					startActivityForResult(intent, NewContactView.EDIT_ADDRESS_REQUEST);
					return;
				case R.id.btn_add_notes:
				case R.id.left_btn_add_notes:
					intent = new Intent(v.getContext(), EditNotesView.class);
					startActivityForResult(intent, NewContactView.EDIT_NOTES_REQUEST);
					return;
				case R.id.btn_add_nickname:
				case R.id.left_btn_add_nickname:
					intent = new Intent(v.getContext(), EditNicknameView.class);
					startActivityForResult(intent, NewContactView.EDIT_NAME_REQUEST);
					return;
				case R.id.btn_add_website:
				case R.id.left_btn_add_website:	
					intent = new Intent(v.getContext(), EditWebsiteView.class);
					startActivityForResult(intent, NewContactView.EDIT_WEBSITE_REQUEST);
					return;
				default:
					break;
			}
		}
	}

	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		if (resultCode == RESULT_OK) {
			// propagates data back to NewContactView
			setResult(resultCode, data);
			finish();
		}
	}
}
