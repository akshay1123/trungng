// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts.view.tab;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import edu.kaist.uilab.tagcontacts.Constants;
import edu.kaist.uilab.tagcontacts.ContactUtils;
import edu.kaist.uilab.tagcontacts.R;
import edu.kaist.uilab.tagcontacts.ServerConnector;
import edu.kaist.uilab.tagcontacts.database.ApplicationData;
import edu.kaist.uilab.tagcontacts.database.BatchOperation;
import edu.kaist.uilab.tagcontacts.database.ContactsHelper;
import edu.kaist.uilab.tagcontacts.database.DBHelper;
import edu.kaist.uilab.tagcontacts.model.AddressEntity;
import edu.kaist.uilab.tagcontacts.model.Contact;
import edu.kaist.uilab.tagcontacts.model.EmailEntity;
import edu.kaist.uilab.tagcontacts.model.Entity;
import edu.kaist.uilab.tagcontacts.model.NameEntity;
import edu.kaist.uilab.tagcontacts.model.PhoneEntity;
import edu.kaist.uilab.tagcontacts.view.widget.DetailedContactView;

/**
 * View for displaying recent calls log.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class PublicContactsView extends Activity {
	
	private static final String UTF8 = "utf-8";
	private static final String ELEMENT_RESULT = "result";
	private static final int MAX_QUERY = 18;
	
	private ListView mListView;
	private GridView mGridView;
	private ViewSwitcher mSwitcher;
	private View mNoResult;
	private DBHelper mHelper;
	private TextView mInput;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tab_public_contacts);
		mNoResult = findViewById(R.id.txt_no_result);
		mSwitcher = (ViewSwitcher) findViewById(R.id.switcher);
		mGridView = (GridView) View.inflate(
				this, R.layout.view_search_suggestions, null).findViewById(R.id.popular_search);
		initGridViewContent();
		mListView = (ListView) View.inflate(
				this, R.layout.view_search_results, null).findViewById(R.id.list_search_results);
		mSwitcher.addView(mGridView);
		mSwitcher.addView(mListView);
		mSwitcher.setDisplayedChild(0);
		Toast.makeText(this, getString(R.string.see_menu), Toast.LENGTH_LONG).show();

		/**
		((TextView) findViewById(R.id.txt_public_search)).setText("doctor");
		mSwitcher.setDisplayedChild(1);
		seedMockData();
		/** temp **/
		
		mInput = (TextView) findViewById(R.id.txt_public_search);
		mInput.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void afterTextChanged(Editable s) {
				if (s.length() == 0) {
					mSwitcher.setDisplayedChild(0);
					mNoResult.setVisibility(View.GONE);
				}
			}
		});
		ImageButton search = (ImageButton) findViewById(R.id.btn_public_search);
		search.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String query = mInput.getText().toString().trim();
				if (query.length() > 0) {
					ProgressDialog dialog = ProgressDialog.show(PublicContactsView.this,
							"", getString(R.string.waiting), true);
					new SearchThread(dialog, query).start();
					mHelper.insertQuery(query);
				}	
			}
		});
		registerForContextMenu(mListView);
	}

	/**
	 * Seeds data for presentation.
	 * 
	 * TODO(trung): remove when no longer needed
	 */
	@SuppressWarnings("unused")
	private void seedMockData() {
		ArrayList<Result> results = new ArrayList<Result>();
		Result result1 = new Result();
		result1.mName = "Wilton Jones";
		result1.mEmail = "xx";
		result1.mPhone = "010-0000-0000";
		result1.mLabels = "doctor, cardiologist";
		ArrayList<String> people1 = new ArrayList<String>();
		people1.add("8809985655");
		people1.add("647698248");
		result1.mPeople = people1;
		
		Result result2 = new Result();
		result2.mName = "Henry Williams";
		result2.mPhone = "010-0000-0000";
		result2.mLabels = "doctor, plastic surgeons";
		ArrayList<String> people2 = new ArrayList<String>();
		result2.mPeople = people2;
		
		Result result3 = new Result();
		result3.mName = "James Miller";
		result3.mPhone = "010-0000-0000";
		result3.mEmail = "xx";
		result3.mLabels = "doctor";
		ArrayList<String> people3 = new ArrayList<String>();
		result3.mPeople = people3;
		ResultAdapter adapter = new ResultAdapter(this, results);
		adapter.add(result1);
		adapter.add(result2);
		adapter.add(result3);
		mListView.setAdapter(adapter);
	}
	
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
  	MenuInflater inflater = getMenuInflater();
  	inflater.inflate(R.menu.public_contact_menu, menu);
  	
  	return true;
  }
	
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
  	super.onOptionsItemSelected(item);
  	
 		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
 		AlertDialog.Builder builder = new AlertDialog.Builder(this);
 		builder.setView(inflater.inflate(R.layout.share_dialog, null))
 				.setTitle(getString(R.string.title_sharing));
 		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
 		builder.create().show();
  	return true;
  }
  
	@Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo info) {
		super.onCreateContextMenu(menu, v, info);
		getMenuInflater().inflate(R.menu.result_context_menu, menu);
		Result result = (Result) mListView.getItemAtPosition(
				((AdapterContextMenuInfo) info).position);
		menu.setHeaderTitle(result.mName);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Result result = (Result) mListView.getItemAtPosition(
				((AdapterContextMenuInfo) info).position);
		int id = item.getItemId();
		switch (id) {
			case R.id.menu_call_contact:
				ContactUtils.callContact(PublicContactsView.this, result.mPhone);
				break;
			case R.id.menu_email_contact:
				ContactUtils.emailContact(PublicContactsView.this, result.mEmail);
				break;
			default:
				bookmarkContact(result);
				break;
		}
		return true;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mHelper.close();
	}
	
	/**
	 * Saves a search result to the phone's contact.
	 * 
	 * @param result
	 */
	private void bookmarkContact(final Result result) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				BatchOperation batch = new BatchOperation(PublicContactsView.this,
						getContentResolver());
				batch.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
						.withValue(RawContacts.ACCOUNT_NAME, null)
						.withValue(RawContacts.ACCOUNT_TYPE, null)
						.withValue(RawContacts.TIMES_CONTACTED, 0)
						.build());
				ArrayList<Entity> entities = new ArrayList<Entity>();
				entities.add(new NameEntity(result.mName, null, null));
				entities.add(new EmailEntity(result.mEmail));
				entities.add(new PhoneEntity(result.mPhone));
				entities.add(new AddressEntity(result.mAddress, null, null, null));
				int mRawId = 0;
				for (Entity entity : entities) {
					ContentProviderOperation op = ContentProviderOperation.newInsert(Data.CONTENT_URI)
							.withValueBackReference(Data.RAW_CONTACT_ID, mRawId)
							.withValues(entity.buildContentValuesWithNoId())
							.build();
					batch.add(op, entity);
				}
				Uri uri = batch.execute();
				ApplicationData.addContact(ContactsHelper.getContact(
						PublicContactsView.this, uri));
				handler.sendEmptyMessage(0);				
			}
			
			private Handler handler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					Toast.makeText(PublicContactsView.this,
							getString(R.string.contact_saved), Toast.LENGTH_SHORT).show();
				}
			};
		}).start();
	}
	
	/**
	 * Initiates the content of grid view which contains popular search query from
	 * the local phone as well as from the server.
	 */
	private void initGridViewContent() {
		mHelper = new DBHelper(PublicContactsView.this);
		final List<String> queries = mHelper.getTopSearchQueries();
		new Thread(new Runnable() {
			@Override
			public void run() {
				Message msg = new Message();
				msg.obj = ServerConnector.getPublicQueries(PublicContactsView.this,
						MAX_QUERY - queries.size());
				handler.sendMessage(msg);
			}
			
			private Handler handler = new Handler() {
				@SuppressWarnings("unchecked")
				@Override
				public void handleMessage(Message msg) {
					queries.addAll((List<String>) msg.obj);
					mGridView.setAdapter(new ArrayAdapter<String>(PublicContactsView.this,
							R.layout.tag_suggestion_item, queries));
					mGridView.postInvalidate();
				}
			};
		}).start();
		mGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				mInput.setText(((TextView) view).getText());
			}
		});
	}
	
	/**
	 * Displays the search result.
	 * 
	 * @param xmlContent xml document as returned by the server
	 */
	private void displaySearchResult(String xmlContent) {
		if (xmlContent != null) {
			try {
				DocumentBuilder builder =
					DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document document = builder.parse(new ByteArrayInputStream(
						xmlContent.getBytes(UTF8)));
				NodeList list = document.getElementsByTagName(ELEMENT_RESULT);
				List<Result> results = new ArrayList<Result>();
				int numResults = list.getLength();
				for (int i = 0; i < numResults; i++) {
					results.add(new Result(list.item(i)));
				}
				if (numResults == 0) {
					mNoResult.setVisibility(View.VISIBLE);
				} else {
					mNoResult.setVisibility(View.GONE);
				}
				mListView.setAdapter(new ResultAdapter(PublicContactsView.this, results));
				mListView.postInvalidate();
				mSwitcher.setDisplayedChild(1);
			} catch (Exception e) {
				Toast.makeText(PublicContactsView.this, getString(R.string.error),
						Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	/**
	 * Threads for getting search results.
	 */
	private class SearchThread extends Thread {
		private ProgressDialog mDialog;
		private String mQuery;
		
		public SearchThread(ProgressDialog dialog, String query) {
			mDialog = dialog;
			mQuery = query;
		}
		
		@Override
		public void run() {
			Message msg = new Message();
			msg.obj = ServerConnector.sendSearchQuery(PublicContactsView.this, mQuery);
			handler.sendMessage(msg);
		}
		
		private Handler handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				mDialog.dismiss();
				displaySearchResult((String) msg.obj);
			}
		};
	}
	
	/**
	 * A search result.
	 */
	private static class Result {
		String mName;
		String mPhone;
		String mEmail;
		String mAddress;
		String mLabels;
		List<String> mPeople;

		/**
		 * Default constructor
		 */
		public Result() { }
		
		/**
		 * Constructor
		 * 
		 * @param resultNode the result node which conforms to the DTD provided by
		 * 			the server
		 */
		public Result(Node resultNode) {
			NodeList childNodes = resultNode.getChildNodes();
			mName = getText(childNodes.item(0));
			mPhone = getText(childNodes.item(1));
			mEmail = getText(childNodes.item(2));
			mAddress = getText(childNodes.item(3));
			mLabels = getText(childNodes.item(4));
			int childs = childNodes.getLength();
			mPeople = new ArrayList<String>();
			if (childs > 5) {
				// if one or more "person" element presents
				for (int i = 5; i < childs; i++) {
					mPeople.add(getText(childNodes.item(i)));
				}
			}
		}
		
		/**
		 * Gets text value of {@code node}.
		 * 
		 * @param node
		 * @return
		 */
		private String getText(Node node) {
			if (node.hasChildNodes()) {
				return node.getFirstChild().getNodeValue();
			}
			return null;
		}
	}
	
	/**
	 * Adapter for the list view.
	 */
	private static class ResultAdapter extends ArrayAdapter<Result> {
		
		private LayoutInflater mInflater;
		private Context mContext;
		
		/**
		 * Constructor
		 * 
		 * @param context
		 * @param results
		 */
		public ResultAdapter(Context context, List<Result> results) {
			super(context, R.layout.search_result_item, results);
			mInflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
			mContext = context;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = mInflater.inflate(R.layout.search_result_item, null);
			final Result result = getItem(position);
			((TextView) view.findViewById(R.id.result_name)).setText(result.mName);
			TextView tags = (TextView) view.findViewById(R.id.result_tags);
			tags.setText(result.mLabels);
			if (result.mPhone != null && result.mPhone.length() > 0) {
				View v = view.findViewById(R.id.result_btn_call);
				v.setTag(result.mPhone);
				v.setVisibility(View.VISIBLE);
				v.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						ContactUtils.callContact(mContext, result.mPhone);
					}
				});
			}
			if (result.mEmail != null && result.mEmail.length() > 0) {
				View v = view.findViewById(R.id.result_btn_email);
				v.setTag(result.mEmail);
				v.setVisibility(View.VISIBLE);
				v.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						ContactUtils.emailContact(mContext, result.mEmail);
					}
				});
			}
			if (result.mAddress != null && result.mAddress.length() > 0) {
				TextView v = (TextView) view.findViewById(R.id.result_address); 
				v.setText(result.mAddress);
				v.setVisibility(View.VISIBLE);
			}
			List<Contact> people = numbersToContacts(result.mPeople);
			if (people.size() > 0) {
				View holder = view.findViewById(R.id.result_holder_people);
				LinearLayout layout = (LinearLayout) holder.findViewById(R.id.result_people);
				for (Contact contact : people) {
					layout.addView(inflateChildView(contact));
				}
				holder.setVisibility(View.VISIBLE);
			}
			return view;
		}
		
		private TextView inflateChildView(final Contact contact) {
			TextView child = (TextView) View.inflate(
					mContext, R.layout.clickable_inline_text, null);
			child.setText(contact.getName() + ",");
			child.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(mContext, DetailedContactView.class);
					intent.putExtra(Constants.INTENT_LONG_RAW_CONTACT_ID,
							contact.getRawContactId());
					mContext.startActivity(intent);
				}
			});
			
			return child;
		}
		/**
		 * Converts the list of numbers {@code numbers} to contacts.
		 * 
		 * @param numbers
		 * @return
		 */
		private List<Contact> numbersToContacts(List<String> numbers) {
			List<Contact> contacts = ApplicationData.getContacts();
			List<Contact> results = new ArrayList<Contact>();
			for (String number : numbers) {
				for (Contact contact : contacts) {
					if (contact.hasPhoneNumber(number)) {
						results.add(contact);
					}
				}
			}
			return results;
		}
	}
}
