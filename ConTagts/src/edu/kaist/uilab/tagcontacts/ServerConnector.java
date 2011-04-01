package edu.kaist.uilab.tagcontacts;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.telephony.TelephonyManager;
import android.util.Log;
import edu.kaist.uilab.tagcontacts.database.ContactsHelper;
import edu.kaist.uilab.tagcontacts.model.AddressEntity;
import edu.kaist.uilab.tagcontacts.model.Contact;
import edu.kaist.uilab.tagcontacts.model.EmailEntity;
import edu.kaist.uilab.tagcontacts.model.Entity;
import edu.kaist.uilab.tagcontacts.model.NameEntity;
import edu.kaist.uilab.tagcontacts.model.PhoneEntity;
import edu.kaist.uilab.tagcontacts.model.TagEntity;

/**
 * Class for sending logging and search request to server.
 *
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class ServerConnector {

	private static final String URL_LABEL_LOG = "http://uicontacts.appspot.com/label?";
	private static final String URL_CALL_LOG = "http://uicontacts.appspot.com/call?";
	private static final String URL_SEARCH = "http://uicontacts.appspot.com/search?";
	private static final String URL_SHARE = "http://uicontacts.appspot.com/share?";
	private static final String URL_ENTITY = "http://uicontacts.appspot.com/entity?";
	private static final String URL_TOP_QUERIES = "http://uicontacts.appspot.com/topquery";
	
	protected static final String PARAM_DEVICE_ID = "deviceid";
	protected static final String PARAM_TIME = "time";
	protected static final String PARAM_TYPE = "type";
	protected static final String PARAM_CONTENT = "content";
	protected static final String PARAM_ID = "id";
	protected static final String PARAM_NUMBER = "number";
	protected static final String PARAM_DURATION = "duration";
	protected static final String PARAM_DATE = "date";
	protected static final String PARAM_QUERY = "query";
	protected static final String PARAM_NAME = "name";
	protected static final String PARAM_EMAIL = "email";
	protected static final String PARAM_ADDRESS = "address";
	protected static final String PARAM_LABELS = "labels";
	protected static final String PARAM_GROUPS = "groups";
	protected static final String PARAM_CITY = "city";
	
	private static final String DELIMITER = "*";
	private static final String LAST_LOGGED_DATE = "lastdate";
	private static final String UTF8 = "utf-8";
	protected static final String TAG = "ServerConnector";

	/**
	 * Sends search query to the server and gets back the result.
	 * 
	 * @param context
	 * @param query the query string
	 * @return
	 * 			an xml document containing search result, null if some error occurs
	 */
	public static String sendSearchQuery(final Context context, String query) {
		try {
			StringBuilder builder = new StringBuilder(URL_SEARCH);
			builder.append(PARAM_DEVICE_ID).append("=").append(getDeviceId(context));
			appendParam(builder, PARAM_QUERY, query);
			appendParam(builder, PARAM_CITY, getCity(context));
			return getResponse(builder.toString());
		} catch (UnsupportedEncodingException e) {
			// this exception should never happen
			Log.e(TAG, e.getMessage(), e);
		}
		
		return null;
	}
	
	/**
	 * Gets city (or user location that was cached before).
	 * @return
	 */
	private static String getCity(Context context) {
		return context.getSharedPreferences(Constants.LOCATION_PREFERENCE,
				0).getString(Constants.LAST_KNOWN_LOCATION, "");
	}
	
	/**
	 * Sends an entity log to server at the url "/entity" when the application
	 * is installed.
	 * 
	 * @param context
	 */
	public static void sendEntityLog(final Context context) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				StringBuilder builder = new StringBuilder(URL_ENTITY);
				TelephonyManager manager = (TelephonyManager) context.getSystemService(
						Context.TELEPHONY_SERVICE);
				builder.append(PARAM_DEVICE_ID).append("=").append(manager.getDeviceId());
				String number = manager.getLine1Number();
				if (number != null) {
					builder.append("&").append(PARAM_NUMBER).append("=").append(number);
				}
				try {
					execute(builder.toString());
					Log.i(TAG, "entity log sent");
				} catch (Exception e) {
					Log.e(TAG, e.getMessage(), e);
				}
			}
		}).start();
	}
	
	/**
	 * Sends all call logs after the previously sent time.
	 * 
	 * @param context
	 */
	public static void sendCallLogs(final Context context) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Cursor c = context.getContentResolver().query(
							CallLog.Calls.CONTENT_URI,
							new String[] { Calls._ID, Calls.NUMBER, Calls.DURATION, Calls.DATE },
							Calls.DURATION + "> ?" + " AND " + Calls.DATE + "> ?",
							new String[] { String.valueOf(0), getLastLoggedDate(context) },
							Calls.DATE
					);
					String lastDate = "0";
					while (c.moveToNext()) {
						if (lastDate.equals("0")) {
							lastDate = c.getString(3);
							saveLastLoggedDate(context, lastDate);
						}
						StringBuilder builder = new StringBuilder(URL_CALL_LOG);
						builder.append(PARAM_DEVICE_ID).append("=").append(getDeviceId(context));
						appendParam(builder, PARAM_NUMBER, formatPhone(c.getString(1)));
						appendParam(builder, PARAM_DURATION, c.getString(2));
						appendParam(builder, PARAM_DATE, c.getString(3));
						execute(builder.toString());
					}
					c.close();
					Log.i(TAG, "call logs sent");
				} catch (Exception e) {
					Log.e(TAG, e.getMessage(), e);
				}
			}
		}).start();
	}
	
	/**
	 * Sends a call log to the server.
	 * 
	 * <p> This is indeed a SMS which is considered equivalent of a call with
	 * duration of 3 minutes.
	 * 
	 * @param number
	 */
	public static void sendSingleCallLog(final Context context, final String number) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					StringBuilder builder = new StringBuilder(URL_CALL_LOG);
					builder.append(PARAM_DEVICE_ID).append("=").append(getDeviceId(context));
					appendParam(builder, PARAM_NUMBER, number);
					appendParam(builder, PARAM_DURATION, String.valueOf(180));
					appendParam(builder, PARAM_DATE, String.valueOf(
							System.currentTimeMillis()));
					execute(builder.toString());
				} catch (Exception e) {
					Log.e(TAG, e.getMessage(), e);
				}
			}
		}).start();
	}
	
	/**
	 * Saves the last time that call logs was sent.
	 */
	private static void saveLastLoggedDate(Context context, String lastDate) {
		try {
			FileOutputStream os = context.openFileOutput(LAST_LOGGED_DATE, 0);
			os.write(lastDate.getBytes());
			os.close();
		} catch (Exception e) {
			// ignore exception
			e.printStackTrace();
		}
	}
	
	/**
	 * Gets the last time that call logs was sent.
	 */
	private static String getLastLoggedDate(Context context) {
		String result = null;
		
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					context.openFileInput(LAST_LOGGED_DATE)));
			result = reader.readLine();
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (result == null) {
			// set last logged date to the first time that the app runs
			String lastDate = String.valueOf(System.currentTimeMillis());
			saveLastLoggedDate(context, lastDate);
			return lastDate;
		}
		
		return result;
	}
	
	/**
	 * Sends a label log to server at the url "/label".
	 * 
	 * @param context
	 * @param type
	 * @param tags
	 * @param id
	 */
	public static void sendLabelLog(final Context context, final String type, final List<String> tags,
			final Contact contact) {
		sendLabelLog(context, type, listToString(tags), contact);
	}

	/**
	 * Sends a label log to server at the url "/label".
	 * 
	 * @param context
	 * @param type
	 * @param content
	 * @param contact
	 */
	public static void sendLabelLog(final Context context, final String type, final String content,
			final Contact contact) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					StringBuilder builder = new StringBuilder(URL_LABEL_LOG);
					builder.append(PARAM_DEVICE_ID).append("=").append(getDeviceId(context));
					appendParam(builder, PARAM_TIME, String.valueOf(System.currentTimeMillis()));
					appendParam(builder, PARAM_TYPE, type);
					appendParam(builder, PARAM_CONTENT, content);
					if (contact != null) {
						List<PhoneEntity> list = contact.getPhones();
						if (list.size() > 0) {
							appendParam(builder, PARAM_ID, formatPhone(list.get(0).toString()));
						}
					}	
					execute(builder.toString());
					Log.i(TAG, "log message sent");
				} catch (Exception e) {
					Log.e(TAG, e.getMessage(), e);
				}
			}
		}).start();
	}

	/**
	 * Sends a share contact to server at the url "/share".
	 * 
	 * @param context
	 * @param contact
	 */
	public static void sendShareContact(final Context context, final long rawId,
			final List<String> groups) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					StringBuilder builder = new StringBuilder(URL_SHARE);
					builder.append(PARAM_DEVICE_ID).append("=").append(getDeviceId(context));
					HashMap<String, List<Entity>> map = ContactsHelper.getEntitiesForContact(
							context, rawId);
					// set "name" parameter
					List<Entity> list = map.get(NameEntity.MIMETYPE);
					if (list.size() > 0) {
						appendParam(builder, PARAM_NAME, list.get(0).toString());
					} else {
						return;	// contact without name cannot be shared
					}
					// set "labels" parameter
					list = map.get(TagEntity.MIMETYPE);
					if (list.size() > 0) {
						StringBuilder tagsBuilder = new StringBuilder("[");
						for (Entity entity : list) {
							tagsBuilder.append(entity.toString()).append(DELIMITER);
						}
						tagsBuilder.append("]");
						appendParam(builder, PARAM_LABELS, tagsBuilder.toString());
					} else {
						return; // contact with no labels cannot be shared
					}
					// set "email", "number", "address", "groups" labels
					appendPhoneParam(map.get(PhoneEntity.MIMETYPE), builder);
					appendParam(map.get(EmailEntity.MIMETYPE), builder, PARAM_EMAIL);
					appendParam(map.get(AddressEntity.MIMETYPE), builder, PARAM_ADDRESS);
					if (groups.size() > 0) {
						appendParam(builder, PARAM_GROUPS, listToString(groups));
					}
					appendParam(builder, PARAM_CITY, getCity(context));
					execute(builder.toString());
				} catch (Exception e) {
					Log.e(TAG, e.getMessage(), e);
				}
			}
		}).start();
	}

	/**
	 * Returns at most {@code size} popular search queries by the public.
	 * 
	 * @return
	 */
	public static List<String> getPublicQueries(Context context, int size) {
		List<String> results = new ArrayList<String>();
		String response = getResponse(URL_TOP_QUERIES
				+ "?" + PARAM_CITY + "=" + getCity(context));
		String[] queries = response.split(",");
		if (size > queries.length) {
			size = queries.length;
		}
		for (int i = 0; i < size; i++) {
			results.add(queries[i]);
		}
		
		return results;
	}
	
	private static void appendParam(List<Entity> list, StringBuilder builder,
			String param) throws Exception {
		if (list.size() > 0) {
			builder.append("&").append(param).append("=").append(
				URLEncoder.encode(list.get(0).toString(), UTF8));
		}
	}
	
	private static void appendPhoneParam(List<Entity> list, StringBuilder builder)
			throws Exception {
		if (list.size() > 0) {
			builder.append("&").append(PARAM_NUMBER).append("=").append(
				URLEncoder.encode(formatPhone(list.get(0).toString()), UTF8));
		}
	}
	
	/**
	 * Appends the pair of {@code param} and {@code value} to {@code builder}.
	 * 
	 * <p> The {@code value} param will be encoded in utf-8 before returning.
	 * 
	 * @param builder
	 * @param param
	 * @param value
	 * 
	 * @throws UnsupportedEncodingException
	 * 			this should never occur because utf-8 is supported
	 */
	private static void appendParam(StringBuilder builder, String param,
			String value) throws UnsupportedEncodingException {
		builder.append("&").append(param).append("=").append(
				URLEncoder.encode(value, UTF8));
	}

	/**
	 * Returns device id of this phone.
	 * 
	 * @param context
	 * @return
	 */
	private static String getDeviceId(Context context) {
		return ((TelephonyManager) context.getSystemService(
				Context.TELEPHONY_SERVICE)).getDeviceId();
	}
	
	/**
	 * Executes a request at {@code request}.
	 * 
	 * @param request an encoded url
	 */
	private static void execute(String request) throws Exception {
		HttpClient httpClient = new DefaultHttpClient();
	  HttpGet httpGet = new HttpGet(request);
	  httpClient.execute(httpGet);
	  httpClient.getConnectionManager().shutdown();
	}
	
	/**
	 * Executes a request at {@code request} and returns the response from the server.
	 *  
	 * @param request an encoded url
	 * @return
	 * 			the response from the server; an empty string is returned if an error occurs
	 */
	private static String getResponse(String request) {
		String response = "";
		try {
			HttpClient httpclient = new DefaultHttpClient();
		  HttpGet httpget = new HttpGet(request); 
	    ResponseHandler<String> responseHandler = new BasicResponseHandler();
	    response = httpclient.execute(httpget, responseHandler);
	    httpclient.getConnectionManager().shutdown();
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
		}
		
		return response;
	}
	
	/**
	 * Converts a list to a string representation. The entire string is enclosed by [],
	 * whereas each element is separated by {@value #DELIMITER}.
	 * 
	 * @param list
	 * @return
	 */
	private static String listToString(List<String> list) {
		StringBuilder builder = new StringBuilder("[");
		for (String s : list) {
			builder.append(s).append(DELIMITER);
		}
		builder.append("]");
		
		return builder.toString();
	}

	/**
	 * Formats the phone {@code number} so that it conforms to the phone number
	 * format used by the server.
	 * 
	 * @param number
	 * @return
	 */
	public static String formatPhone(String number) {
		StringBuilder builder = new StringBuilder();
		char c;
		for (int i = 0; i < number.length(); i++) {
			c = number.charAt(i);
			if (c != '-') {
				builder.append(c);
			}
		}
		return builder.toString();
	}
}
