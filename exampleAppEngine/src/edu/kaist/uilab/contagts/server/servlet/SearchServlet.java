// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.contagts.server.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.kaist.uilab.contagts.server.ContactEntity;
import edu.kaist.uilab.contagts.server.Friend;
import edu.kaist.uilab.contagts.server.SearchQuery;
import edu.kaist.uilab.contagts.server.SharableContact;
import edu.kaist.uilab.contagts.server.TagPair;
import edu.kaist.uilab.contagts.server.TagPairHelper;

/**
 * Servlet for handling request at "/search".
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class SearchServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private static final String FILE_TAGS = "/WEB-INF/tags.txt";
	private static final String PARAM_DEVICE_ID = "deviceid";
	private static final String PARAM_QUERY = "query";
	private static final String PARAM_CITY = "city";
	private static final String ENCODING = "utf-8";
	private static final String CONTENT_TYPE = "text/xml";
	
	private static final float NORMALIZATION = 2000f;
	private static final float ABASE = 0.001f;
	
	private HashMap<String, HashMap<String, Integer>> mMap; // map of tags and its related tags
	private PersistenceManager mManager;
	
	@Override
	public void init() throws ServletException {
		mMap = readTags(getServletConfig());
		mManager = ServletUtils.getPmf().getPersistenceManager();
	}
	
	/**
	 * Reads tags from static file into the internal map.
	 */
	private HashMap<String, HashMap<String, Integer>> readTags(ServletConfig config)
		throws ServletException {
		HashMap<String, HashMap<String, Integer>> result = 
			new HashMap<String, HashMap<String, Integer>>();
		try {
			URL url = config.getServletContext().getResource(FILE_TAGS);
			File file = new File(url.toURI());
			BufferedReader in = new BufferedReader(new FileReader(file));
			HashMap<String, Integer> relatedMap;
			String tag;
			StringTokenizer tokenizer;
			while ((tag = in.readLine()) != null) {
				tokenizer = new StringTokenizer(in.readLine(), ",");
				relatedMap = new HashMap<String, Integer>();
				while (tokenizer.hasMoreTokens()) {
					relatedMap.put(tokenizer.nextToken(), Integer.parseInt(tokenizer.nextToken()));
				}
				result.put(tag, relatedMap);
			}
			in.close();
			return result;
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
	
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
  		throws IOException {
  	String deviceId = req.getParameter(PARAM_DEVICE_ID);
  	String queryValue = req.getParameter(PARAM_QUERY);
  	String city = req.getParameter(PARAM_CITY);
  	String[] terms = queryToTerms(queryValue);
 		// rank(u, c) = R(Tc, Q) * sumof(w(u, v) * l(v, c))
 		ContactEntity u = ServletUtils.getContactByDeviceId(mManager, deviceId);
 		// get candidates and compute R(Tc, Q)
 		TreeSet<SharableContact> rankedCandidates = new TreeSet<SharableContact>(
 				new SharableContactComparator());
 		List<SharableContact> candidates = moreSternQualify(u, terms, city);
		if (candidates.size() > 0) {
			List<Friend> friends = u.getFriends();
			for (SharableContact c : candidates) {
				float score = 0;
				// sumof(w(u,v) * l(v, c))
  			for (Friend friend : friends) {
  				String v = friend.getDeviceId();
  				float l = l(v, c);
  				if (l > 0) {
  					score += u.getWeight(v) * l;
  					ContactEntity person = ServletUtils.getContactByDeviceId(mManager, v);
  					if (person != null) {
  						c.addPerson(person.getNumber());
  					}	
  				}
  			}
  			// r(Tc, Q) * sumof(w(u,v) * l(v, c))
  			if (score != 0) {
  				c.setScore(score * c.getScore()); // if friends of u knows this service, update the score
  			} else {
  				// if u's friends don't know about this service, lower the score
  				// (because sumof(w(u,v) * l(v, c)) is very small)
 					c.setScore(ABASE * c.getScore());
  			}
  			rankedCandidates.add(c);
			}
		}
		printResult(resp, buildXml(rankedCandidates.descendingIterator()));
		logSearchQuery(queryValue, city);
  }
  
  /**
   * Prints result.
   * 
   * @param resp
   * @param value
   * @throws IOException
   */
  private void printResult(HttpServletResponse resp, String value)
  		throws IOException {
  	resp.setCharacterEncoding(ENCODING);
  	resp.setContentType(CONTENT_TYPE);
  	PrintWriter writer = resp.getWriter();
		writer.print(value);
		writer.flush();
		writer.close();
  }
  
  /**
   * Logs {@code queryValue} to the {@link SearchQuery} entity table.
   * 
   * @param queryValue the query
   * @param city the city in which the query is requested
   */
  private void logSearchQuery(String queryValue, String city) {
		// log this search query
		PersistenceManager pm = ServletUtils.getPmf().getPersistenceManager();
		try {
			Query query = pm.newQuery(SearchQuery.class);
			query.setFilter("query == :queryParam && city == :cityParam");
			query.setUnique(true);
			SearchQuery searchQuery = (SearchQuery) query.execute(queryValue, city);
			if (searchQuery == null) {
				searchQuery = new SearchQuery(queryValue, city);
			}
			searchQuery.increaseTimes();
			pm.makePersistent(searchQuery);
			query.closeAll();
		} finally {
			pm.close();
		}
  }
  
  /**
   * A comparator for comparing two {@link SharableContact}s based on their scores.
   */
  private class SharableContactComparator implements Comparator<SharableContact> {
		@Override
		public int compare(SharableContact o1, SharableContact o2) {
			if (o1.getScore() < o2.getScore())
				return -1;
			if (o1.getScore() == o2.getScore() && o1.getKey().equals(o2.getKey()))
				return 0;
			return 1;
		}
  }
  
  /**
   * Builds the xml document for returning to the client.
   * 
   * @param results
   * @return
   */
  private String buildXml(Iterator<SharableContact> results) {
  	StringBuilder builder = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
  	builder.append("<results>");
  	SharableContact result;
  	while (results.hasNext()) {
  		result = results.next();
  		builder.append("<result>");
 			builder.append(buildElement(SharableContact.NAME, result.getName()));
  		builder.append(buildElement(SharableContact.NUMBER, result.getNumber()));
  		builder.append(buildElement(SharableContact.EMAIL, result.getEmail()));
  		builder.append(buildElement(SharableContact.ADDRESS, result.getAddress()));
  		builder.append(buildElement(SharableContact.LABEL, result.getStringOfLabels()));
  		if (result.getPeople() != null) {
    		for (String number : result.getPeople()) {
    			builder.append(buildElement(SharableContact.PERSON, number));
    		}
  		}
  		builder.append("</result>");
  	}
  	builder.append("</results>");
  	return builder.toString();
  }
  
  /**
   * Helper method for building one xml element.
   * 
   * @param name the element name
   * @param value value of the element
   * @return
   */
  private String buildElement(String name, String value) {
  	String v = value;
  	if (v == null) {
  		v = "";
  	}
  	return "<" + name + ">" + v + "</" + name + ">";
  }
  
  /**
   * Computes the function l(v, c) which is defined as below.
   * if c is a SharableContact but not ContactEntity:
   *   l(v, c) := 1 if v tagged c
   *           := 0 otherwise
   * if c is both a SharableContact and a ContactEntity
   *   l(v, c) := 1 + w(v, c) if v tagged c
   *           := w(v, c) otherwise
   *           
   * @param v device id of v
   * @param c
   * @return
   */
  private float l(String v, SharableContact c) {
  	float l = (c.taggedBy(v)) ? 1 : 0;
  	// determine if c is a contact entity
  	if (c.getNumber() != null) {
  		ContactEntity entity = ServletUtils.getContactByNumber(mManager, c.getNumber());
    	if (entity != null) {
    		l += entity.getWeight(v);
    	}
  	}	
  	return l;
  }
  
	/**
   * Qualify all candidates against the query terms and the user who searches.
   * 
   * <p> A candidate c is qualified if accessible(u, c) and c's tags contains at
   * least one of the element of {@code terms} where
   * accessible(u,c) := true if u is granted to see c by some its friends.
   * 
   * @param u the {@code ContactEntity} that searches
   * @param terms the query terms
   * @return
   */
  @SuppressWarnings("unchecked")
	private List<SharableContact> moreSternQualify(ContactEntity u,
			String[] term, String city) {
  	Set<SharableContact> candidates = new HashSet<SharableContact>();
  	Query query = mManager.newQuery(SharableContact.class);
  	query.setFilter("city == :cityParam && stringLabels.contains(:label)");
  	try {
  		for (int i = 0; i < term.length; i++) {
  			candidates.addAll((List<SharableContact>) query.execute(city, term[i]));
  		}
  	} finally {
  		query.closeAll();
  	}
  	
  	List<SharableContact> result = new ArrayList<SharableContact>();
   	for (SharableContact c : candidates) {
			// explicitly get the data so that the lists are read from database
   		c.getLabels();
   		c.getGroups();
  		if (c.isVisibleTo(u)) {
  			List<String> labels = c.getStringLabels();
  			SharableContact newC = new SharableContact(c, getRelatedness(labels, term));
  			newC.setLabels(c.getLabels());
  			newC.setStringLabels(c.getStringLabels());
   			result.add(newC);
  		}
  	}
  	
  	return result;
  }

  /**
   * Gets the relatedness between two list of strings.
   * 
   * <p> The relatedness returned is normalized.
   * 
   * @param labels
   * @param terms
   * @return
   */
  private float getRelatedness(List<String> labels, String[] term) {
  	float result = 0;
  	HashMap<String, Integer> m;
  	Integer related;
  	TagPair pair;
  	for (int idx = 0; idx < term.length; idx++) {
  		m = mMap.get(term[idx]); // get related tags for term[idx]
			for (String label : labels) {
				if (label.equals(term[idx])) { // related(term[idx], label) = 1 (i.e., = normalization)
					result += NORMALIZATION;
				} else if (m != null) {
  				related = m.get(label); // get related(term[idx], label)
  				if (related != null) {
  					result += related;
  				}
				}
				// get relatedness from our internal database
				pair = TagPairHelper.getTagPair(mManager, term[idx], label);
				if (pair != null) {
					result += pair.getValue() * 3;
				}
			}
  	}
  	return result / NORMALIZATION;
  }
  
  /**
   * Tokenizes the query submitted by client to separate terms.
   * 
   * @param query
   * @return
   */
  private String[] queryToTerms(String query) {
  	StringTokenizer tokenizer = new StringTokenizer(query, " ");
  	String[] result = new String[tokenizer.countTokens()];
  	for (int i = 0; i < result.length; i++) {
  		result[i] = tokenizer.nextToken();
  	}
  	
  	return result;
  }
}
