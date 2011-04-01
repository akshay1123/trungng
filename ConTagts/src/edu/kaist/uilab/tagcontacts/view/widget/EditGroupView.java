// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.view.widget;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import edu.kaist.uilab.tagcontacts.R;

/**
 * Activity for adding new tags for a contact.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
@Deprecated
public class EditGroupView extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_group);
		
		// make top bar's title appropriate for this view
		TextView tv = (TextView) findViewById(R.id.top_bar_title);
		tv.setText(R.string.title_add_group);
		
		Button cancel = (Button) findViewById(R.id.btn_bar_left);
		cancel.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});
	}	
}
