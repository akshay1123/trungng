// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.tagcontacts;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import edu.kaist.uilab.tagcontacts.database.DBHelper;
import edu.kaist.uilab.tagcontacts.view.tab.AllContactsView;
import edu.kaist.uilab.tagcontacts.view.tab.FavoriteView;
import edu.kaist.uilab.tagcontacts.view.tab.GroupsView;
import edu.kaist.uilab.tagcontacts.view.tab.PublicContactsView;

/**
 * The starting point of  the application.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class MainApplication extends TabActivity {

	private static final String FAVORITES = "Favorites";
	private static final String ALL_CONTACTS = "All";
	private static final String GROUPS = "Groups";
	private static final String PUBLIC = "Public";
	private static final long SIX_HOURS = 6 * 60 * 60 * 1000;

	private LayoutInflater inflater;
	private LocationListener mListener;
	private LocationManager mManager;
	
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
    TabHost tabHost = getTabHost();
    TabHost.TabSpec spec;
    Intent intent;
 
    intent = new Intent().setClass(this, FavoriteView.class);
    spec = tabHost.newTabSpec(FAVORITES).setIndicator(CustomIndicator.inflateTab(inflater,  
    		R.drawable.ic_tab_starred)).setContent(intent);
    tabHost.addTab(spec);

    intent = new Intent().setClass(this, AllContactsView.class);
    spec = tabHost.newTabSpec(ALL_CONTACTS).setIndicator(CustomIndicator.inflateTab(inflater,
    		R.drawable.ic_tab_contacts)).setContent(intent);
    tabHost.addTab(spec);

    intent = new Intent().setClass(this, GroupsView.class);
    spec = tabHost.newTabSpec(GROUPS).setIndicator(CustomIndicator.inflateTab(inflater,
        R.drawable.ic_tab_groups)).setContent(intent);
    tabHost.addTab(spec);

    intent = new Intent().setClass(this, PublicContactsView.class);
    spec = tabHost.newTabSpec(PUBLIC).setIndicator(CustomIndicator.inflateTab(inflater,
        R.drawable.ic_tab_public)).setContent(intent);
    tabHost.addTab(spec);
    tabHost.setCurrentTab(1);
    
    // manage location
		mManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		mListener = new OnetimeLocationListener(this);
		mManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				SIX_HOURS, 0, mListener);
		mManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
				SIX_HOURS, 0, mListener);
  }
  
  @Override
  public void onDestroy() {
  	super.onDestroy();
  	new DBHelper(this).close();
  }

  /**
   * Location listener that obtain location sparingly.
   */
  private class OnetimeLocationListener implements LocationListener {
  	
  	private Context mContext;

  	/**
  	 * Constructor
  	 * 
  	 * @param context
  	 */
  	public OnetimeLocationListener(Context context) {
  		mContext = context;
  	}
  	
		public void onLocationChanged(final Location loc) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					String request = "http://maps.google.com/maps/api/geocode/xml?latlng="
						+ Double.toString(loc.getLatitude()) + "," + Double.toString(loc.getLongitude())
						+ "&sensor=true";
					String response = null;
					try {
						// get geocode response from google geocode api
						HttpClient httpclient = new DefaultHttpClient();
						HttpGet httpget = new HttpGet(request);
						ResponseHandler<String> responseHandler = new BasicResponseHandler();
						response = httpclient.execute(httpget, responseHandler);
						httpclient.getConnectionManager().shutdown();
						
						// parse content for the postal code of this region
						LocationResolver resolver = new LocationResolver(response);
						if (resolver.isValid()) {
							Editor editor = mContext.getSharedPreferences(
									Constants.LOCATION_PREFERENCE, 0).edit();
							editor.putString(Constants.LAST_KNOWN_LOCATION,
									resolver.toString()).commit();
							Log.i("ConTAGts", resolver.toString());
						}
					} catch (Exception e) {
						Log.e("ConTAGts", e.getMessage(), e);
					}
				}
			}).start();
			// stops listening for location as soon as we obtain the first location
			mManager.removeUpdates(mListener);
		}
		
		public void onStatusChanged(String provider, int status, Bundle extras) {}
		public void onProviderEnabled(String provider) {}
		public void onProviderDisabled(String provider) {}
  }
  
  /**
   * Class that resolves the xml document returned by the Google Geocode API
   * service to some city of some country.
   */
  private static class LocationResolver {
  	private String mCountry;
  	private String mLocality;
  	
  	/**
  	 * Constructor
  	 * 
  	 * <p> This constructor parses the given {@code xml} text to get the locality
  	 * (city) and the country of the user.
  	 * 
  	 * @param xml the xml returned by google geocode api
  	 * @throws Exception
  	 */
  	public LocationResolver(String xml) throws Exception {
			DocumentBuilder builder =
				DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = builder.parse(new ByteArrayInputStream(
					xml.getBytes("utf-8")));
			Node address = document.getElementsByTagName("result").item(0);
			NodeList childNodes = address.getChildNodes();
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node node = childNodes.item(i);
				if ("address_component".equals(node.getNodeName())) {
					if (mCountry == null) {
						mCountry = getCountry(node);
					}
					if (mLocality == null) {
						mLocality = getLocality(node);
					}
				}
			}
  	}
  	
  	/**
  	 * Returns true if the address is successfully resolved.
  	 * 
  	 * @return
  	 */
  	public boolean isValid() {
  		return mCountry != null && mLocality != null;
  	}
  	
  	/**
  	 * Gets the country name if {@code node} is of the "country" address component.
  	 * 
  	 * @param node an xml element of type {@code address_component}
  	 * @return
  	 * 		null if node is not the address component for country type
  	 */
  	private String getCountry(Node node) {
  		NodeList childs = node.getChildNodes();
  		Node childNode;
  		for (int i = 0; i < childs.getLength(); i++) {
  			childNode = childs.item(i);
  			if (childNode.getNodeName().equals("type")
  					&& childNode.getFirstChild().getNodeValue().equals("country")) {
  				return getText(node, "short_name");
  			}
  		}
  		
  		return null;
  	}

  	/**
  	 * Returns the text value for a {@link Node} whose parent node is {@code node} and
  	 * name matches {@code nodeName}.
  	 * 
  	 * @param node
  	 * @param nodeName
  	 * @return
  	 * 		null if {@code node} does not contain any child with {@code nodeName}
  	 */
  	private String getText(Node node, String nodeName) {
  		NodeList childs = node.getChildNodes();
  		Node child;
  		for (int i = 0; i < childs.getLength(); i++) {
  			child = childs.item(i);
  			if (child.getNodeName().equals(nodeName)) {
  				return child.getFirstChild().getNodeValue();
  			}
  		}
  		
  		return null;
  	}
  	
  	/**
  	 * Gets the locality (city) name if {@code node} is of the "locality" address component.
  	 * 
  	 * @param node an xml element of type {@code address_component}
  	 * @return
  	 * 		null if node is not the address component for locality type
  	 */
  	private String getLocality(Node node) {
  		NodeList childs = node.getChildNodes();
  		Node childNode;
  		for (int i = 0; i < childs.getLength(); i++) {
  			childNode = childs.item(i);
  			if (childNode.getNodeName().equals("type")
  					&& childNode.getFirstChild().getNodeValue().equals("locality")) {
  				return getText(node, "long_name");
  			}
  		}
  		
  		return null;
  	}
  	
  	@Override
  	public String toString() {
  		return mCountry + ":" + mLocality;
  	}
  }
  
  /**
   * A custom tab indicator for a finer tab look.
   */
	private static class CustomIndicator {
		
		static LinearLayout inflateTab(LayoutInflater inflater, int drawable) {
			LinearLayout tabIndicator = (LinearLayout) inflater.inflate(
					R.layout.tab_indicator, null);
			ImageView img = (ImageView) tabIndicator.findViewById(R.id.img_tab);
			img.setImageResource(drawable);
			
			return tabIndicator;
		}
  }
}
