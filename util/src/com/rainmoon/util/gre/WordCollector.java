package com.rainmoon.util.gre;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

public class WordCollector {
	private static String MERRIAM_WEBSTER = "http://www.merriam-webster.com/dictionary/";
	
	public static void main(String args[]) throws Exception {
		// abase, abbess, abbey
		System.out.println(getResponse(MERRIAM_WEBSTER + "abase"));
//		PrintWriter writer = new PrintWriter("");
//		writer.close();
	}
	
	/**
	 * Gets the response from the given {@code requestUrl}.
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
