// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.view.widget;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import edu.kaist.uilab.tagcontacts.Constants;
import edu.kaist.uilab.tagcontacts.ContactUtils;
import edu.kaist.uilab.tagcontacts.R;

/**
 * Activity for adding new web site for a contact.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 *
 */
public class EditWebsiteView extends Activity {
	
	private Button mSaveBtn;
	private EditText mTxt1;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_website);

		mSaveBtn = (Button) findViewById(R.id.btn_bar_right);
		
		// make top bar's title appropriate for this view
		TextView bar = (TextView) findViewById(R.id.top_bar_title);
		mTxt1 = (EditText) findViewById(R.id.txt_website);
		ContactUtils.setEditTextListener(mSaveBtn, mTxt1, null, null, null);
		String oldTxt1 = getIntent().getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA1);
		if (oldTxt1 != null) { // edit
			mSaveBtn.setEnabled(true);
			bar.setText(R.string.title_edit_website);
			mTxt1.setText(oldTxt1);
		} else { // add
			bar.setText(R.string.title_add_website);
		}	
		
		Button cancel = (Button) findViewById(R.id.btn_bar_left);
		cancel.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		mSaveBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = getIntent();
				intent.putExtra(Constants.INTENT_TEXT_EXTRA1,	mTxt1.getText().toString());
				intent.putExtra(Constants.INTENT_INT_REQUEST_CODE, NewContactView.EDIT_WEBSITE_REQUEST);
				setResult(RESULT_OK, intent);
				finish();
			}
		});
	}	
}
