package edu.kaist.uilab.tagcontacts.view.tab;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Wrapper for views that display a contact information.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class ContactWrapper {
	private TextView mName;
	private LinearLayout mHolder;

	/**
	 * Constructor
	 */
	public ContactWrapper(View name, View holder) {
		mName = (TextView) name;
		mHolder = (LinearLayout) holder;
	}
	
	public TextView getName() {
		return mName;
	}
	
	public LinearLayout getHolder() {
		return mHolder;
	}
}
