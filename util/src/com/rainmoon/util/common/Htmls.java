package com.rainmoon.util.common;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Utils for getting the content of a web url.
 * 
 * <p> This class requires the Apache's httpclient lib.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class Htmls {

	/**
	 * Sends request to the url at {@code requestUrl}.
	 * 
	 * @param requestUrl
	 */
	public static void sendRequest(String requestUrl) {
		try {
		  System.err.println("executing request " + requestUrl);
			HttpClient httpclient = new DefaultHttpClient();
	    httpclient.execute(new HttpGet(requestUrl));
	    httpclient.getConnectionManager().shutdown();
		} catch (Exception e) {
			System.err.println("Received error status: " + e.getMessage());
		}
	}
	
	/**
	 * Gets the string response from the given {@code requestUrl}.
	 * 
	 * @return
	 * 			 the response as a String, null if an error occurs
	 */
	public static String getResponse(String requestUrl) {
		String response = null;
		try {
			HttpClient httpclient = new DefaultHttpClient();
		  HttpGet httpget = new HttpGet(requestUrl); 
		  System.err.println("executing request " + httpget.getURI());
		
	    // Create a response handler
	    ResponseHandler<String> responseHandler = new BasicResponseHandler();
	    response = httpclient.execute(httpget, responseHandler);
	    httpclient.getConnectionManager().shutdown();
		} catch (Exception e) {
			System.err.println("Received error status: " + e.getMessage());
		}
		
		return response;
	}
}
