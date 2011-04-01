// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.view.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import edu.kaist.uilab.tagcontacts.Constants;
import edu.kaist.uilab.tagcontacts.ContactUtils;
import edu.kaist.uilab.tagcontacts.R;
import edu.kaist.uilab.tagcontacts.database.DBHelper;
import edu.kaist.uilab.tagcontacts.tagsuggestion.FrequentTag;
import edu.kaist.uilab.tagcontacts.tagsuggestion.SuggestionsProvider;
import edu.kaist.uilab.tagcontacts.tagsuggestion.Tag;

/**
 * Activity for adding new tags for a contact.
 *
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class EditTagsView extends Activity {
	
	private String mName; // name is also used for suggestion
	private Button mSaveBtn;
	private EditText mTxt1;
	private GridView mSuggestion;
	private DBHelper mHelper;
	private SuggestionsProvider mProvider;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_tags);
		
		mSaveBtn = (Button) findViewById(R.id.btn_bar_right);
		mSuggestion = (GridView) findViewById(R.id.suggestion);
		mHelper = new DBHelper(this);
		mProvider = new SuggestionsProvider(mHelper.getFrequentTags());
		
		// make top bar's title appropriate for this view
		TextView bar = (TextView) findViewById(R.id.top_bar_title);
		mTxt1 = (EditText) findViewById(R.id.txt_tags);
		ContactUtils.setEditTextListener(mSaveBtn, mTxt1, null, null, null);
		mTxt1.addTextChangedListener(new TagSuggestionWatcher());
		mName = getIntent().getStringExtra(Constants.INTENT_OLD_TEXT_NAME);
		if (mName == null) {
			mName = "";
		}
		String oldTxt1 = getIntent().getStringExtra(Constants.INTENT_OLD_TEXT_EXTRA1);
		if (oldTxt1 != null) { // edit
			mSaveBtn.setEnabled(true);
			bar.setText(R.string.title_edit_tags);
			mTxt1.setText(oldTxt1);
		} else { // add
			bar.setText(R.string.title_add_tags);
			provideSuggestion(mTxt1.getEditableText());
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
				updateDatabase();
				Intent intent = getIntent();
				intent.putExtra(Constants.INTENT_TEXT_EXTRA1, mTxt1.getText().toString());
				setResult(RESULT_OK, intent);
				finish();
			}
		});
	}

	@Override
	public void onStop() {
		super.onStop();
		mHelper.close();
	}
	
	private String[] getTokens(String s) {
		StringTokenizer tokenizer = new StringTokenizer(s, ",");
		String[] ret = new String[tokenizer.countTokens()];
		int idx = 0;
		while (tokenizer.hasMoreTokens()) {
			ret[idx++] = tokenizer.nextToken();
		}
		
		return ret;
	}
	
	/**
	 * Updates database of tags and frequent tags.
	 */
	private void updateDatabase() {
		new Thread(new Runnable() {
			public void run() {
				String[] tags = getTokens(mTxt1.getText().toString());
				for (String tag : tags) {
					if (tag.length() > 0) {
						// update tags database
						Tag t = mHelper.getTag(tag);
						if (t == null) {
							t = new Tag(tag);
						}
						for (int i = 0; i < tags.length; i++) {
							if (tags[i].length() > 0 && !tags[i].equals(tag)) {
								// add all to co-occuring except the tag itself
								t.addCoOccuring(tags[i]);
							}
						}
						if (tags.length > 1) { // only insert a tag if it has co-occuring
							mHelper.insertTag(t);
						}	
						
						// update frequent tags database
						FrequentTag frequentTag = mHelper.getFrequentTag(tag);
						if (frequentTag == null) {
							frequentTag = new FrequentTag(tag, 1);
						} else {
							frequentTag.setTimes(frequentTag.getTimes() + 1);
						}
						mHelper.insertFrequentTag(frequentTag);
					}
				}
			}
		}).start();
	}
	
	/**
	 * Provides suggestions for the tags that user entered.
	 * 
	 * @param s
	 */
	private void provideSuggestion(final Editable s) {
		if (DBHelper.isTagSuggestionReady()) {
			// generate suggestions for entered tags
			StringTokenizer tokenizer = new StringTokenizer(mName + "," + s.toString(), ",");
			List<Tag> userTags = new ArrayList<Tag>();
			Tag userTag;
			while (tokenizer.hasMoreTokens()) {
				userTag = mHelper.getTag(tokenizer.nextToken().trim());
				if (userTag != null) {
					userTags.add(userTag);
				}
			}
			List<String> suggestion = mProvider.getSuggestions(userTags);
			// add view for suggestion box
			mSuggestion.setAdapter(new ArrayAdapter<String>(EditTagsView.this,
					R.layout.tag_suggestion_item, suggestion));
			mSuggestion.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position,
						long id) {
					if (s.length() > 0) {
						s.append("," + ((TextView) view).getText());
					} else {
						s.append(((TextView) view).getText());
					}
				}
			});
			mSuggestion.invalidate();
		}
	}
	
	/**
	 * Watcher for providing tag suggestion.
	 */
	private class TagSuggestionWatcher implements TextWatcher {
		@Override
		public void afterTextChanged(final Editable s) {
			provideSuggestion(s);
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
		}
	}
}
