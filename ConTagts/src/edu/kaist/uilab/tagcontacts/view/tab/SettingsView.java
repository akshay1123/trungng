// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.view.tab;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import edu.kaist.uilab.tagcontacts.ContactUtils;
import edu.kaist.uilab.tagcontacts.R;
import edu.kaist.uilab.tagcontacts.ServerConnector;

/**
 * View for displaying recent calls log.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class SettingsView extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tab_settings);
		
		CheckBox b = (CheckBox) findViewById(R.id.btn_logging);
		b.setChecked(ContactUtils.isLoggingDisabled(this));
		b.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				ContactUtils.setLoggingPreference(SettingsView.this, isChecked);
				ServerConnector.sendLabelLog(SettingsView.this, "disable_logging", "", null);
			}
		});
	}
}
