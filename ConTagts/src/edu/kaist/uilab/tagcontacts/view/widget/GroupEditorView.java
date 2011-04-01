// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.view.widget;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import edu.kaist.uilab.tagcontacts.R;
import edu.kaist.uilab.tagcontacts.database.ApplicationData;
import edu.kaist.uilab.tagcontacts.database.ContactsHelper;
import edu.kaist.uilab.tagcontacts.database.DBHelper;
import edu.kaist.uilab.tagcontacts.model.Group;

/**
 * Editor for modifying existing groups (labels).
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class GroupEditorView extends Activity {
	
	protected static final String TAG = "GroupEditorView";
	private LayoutInflater mInflater;
	private DBHelper mDbHelper;
	
	private EditText mActive;
	private TableLayout mTable;
	private TableRow mActiveRow;
	private boolean mDeleted = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_groups);
		mActive = null;
		mActiveRow = null;
		
		// change the title bar
		TextView v = (TextView) findViewById(R.id.top_bar_title);
		v.setText(R.string.title_edit_groups);
		
		// set listener for the cancel button
		Button cancel = (Button) findViewById(R.id.btn_bar_left);
		cancel.setVisibility(View.INVISIBLE);
		
		Button save = (Button) findViewById(R.id.btn_bar_right);
		save.setEnabled(true);
		save.setText(R.string.done);
		save.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				persistToDatabase();
				// update our cache
				ApplicationData.setGroups(mDbHelper.getAllGroups());
				ApplicationData.setDataChanged(true);
				if (mDeleted) {
					// the only case that causes contacts to be changed
					ApplicationData.setContacts(ContactsHelper.getContacts(
							GroupEditorView.this));
				}
				setResult(RESULT_OK);
				finish();
			}
		});
		
		mDbHelper = new DBHelper(this);
		mInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		
		// load content for the table
		mTable = (TableLayout) findViewById(R.id.table_groups);
		List<Group> groups = mDbHelper.getAllGroups();
		for (Group group : groups) {
			mTable.addView(getRow(group));
		}
		
		// adds a default row for new group
		addNewGroup();
		
		// load content for the color palette
    GridView gridView = (GridView) findViewById(R.id.color_palette);
    final ColorPaletteAdapter adapter = new ColorPaletteAdapter(this);
    gridView.setAdapter(adapter);

    gridView.setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View v, int position,
      		long id) {
      	// change the text color of the focusing textview
      	if (mActive != null) {
      		int newColor = (Integer) adapter.getItem(position);
      		mActive.setTextColor(newColor);
      	}
      }
    });
	}
	
	@Override
	public void onStop() {
		super.onStop();
		mDbHelper.close();
	}
	
	/**
	 * Persists any change made to the database.
	 */
	private void persistToDatabase() {
		int rows = mTable.getChildCount();
		TableRow row;
		for (int i = 0; i < rows; i++) {
			row = (TableRow) mTable.getChildAt(i);
			EditText group = (EditText) row.getChildAt(1); 
			String label = group.getText().toString();
			int color = group.getTextColors().getDefaultColor();
			long groupId = (Long) row.getTag();
			if (groupId != Group.NO_GROUP_ID) {
				// row is a not a new group
				if (label.length() == 0) {
					// user makes the group label become empty so delete the group
					mDeleted = true;
					mDbHelper.deleteGroup(groupId);
				} else {
					// otherwise, update the group
					mDbHelper.updateGroup(groupId, new Group(groupId, label, color));
				}
			} else {
				// row is a new group, insert to database if name <> ""
				if (label.length() > 0) {
					mDbHelper.insertGroup(new Group(label, color));
				}
			}
		}
	}
	
	/**
	 * Returns a {@link TableRow} view for the given group.
	 * 
	 * @param group
	 * @return
	 */
	private TableRow getRow(Group group) {
		TableRow row = (TableRow) mInflater.inflate(R.layout.edit_groups_item, null);
		row.setTag(group.getId());
		
		// inflate the delete button
		Button delete = (Button) row.findViewById(R.id.btn_delete_group);
		delete.setOnClickListener(new DeleteListener());
		
		// inflate EditText for group label
		EditText name = (EditText) row.findViewById(R.id.group_name);
		name.setText(group.getLabel());
		name.setTextColor(group.getColor());
		name.setOnFocusChangeListener(new OnFocusChangeListener() {
			/**
			 * Saves the row and text field that has focus.
			 */
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					mActive = (EditText) v;
					mActiveRow = (TableRow) mActive.getParent();
				}
			}
		});
		
		return row;
	}

	/**
	 * Adds a row for creating new group to the table.
	 */
	private void addNewGroup() {
		TableRow row = getRow(new Group("", Group.DEFAULT_COLOR));
		EditText name = (EditText) row.findViewById(R.id.group_name);
		name.setHint(R.string.new_group_name);
		Button create = (Button) row.findViewById(R.id.btn_delete_group);
		create.setText(" + ");
		create.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// assume that the group was created, it is now subjected to deletion
				Button b = (Button) v;
				b.setText(" - ");
				b.setOnClickListener(new DeleteListener());
				addNewGroup();
			}
		});
		mTable.addView(row);
	}
	
	/**
	 * Listener for the delete button.
	 */
	private class DeleteListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			mActiveRow = (TableRow) v.getParent();
			long id = (Long) mActiveRow.getTag();
			mTable.removeView(mActiveRow);
			mTable.invalidate();
			mDbHelper.deleteGroup(id);
			ContactsHelper.deleteGroup(GroupEditorView.this, id);
			mDeleted = true;
		}
	}
}
