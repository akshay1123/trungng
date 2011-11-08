// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.contagts.server.servlet;

import java.io.IOException;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.kaist.uilab.contagts.server.SearchQuery;

/**
 * Servlet for handling request at "/topqueries" which returns the list of
 * at most {@value MAX_RESULTS} top queries.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class TopQueryServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	
	private static final String PARAM_CITY = "city";
	private static final String CONTENT_TYPE = "text/plain";
	private static final String ENCODING = "utf-8";
	
	public static final int MAX_RESULTS = 18;
	private static final String QUERY_DELIMITER = ",";

  @SuppressWarnings("unchecked")
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
  	String city = req.getParameter(PARAM_CITY);
  	PersistenceManager pm = ServletUtils.getPmf().getPersistenceManager();
  	try {
  		Query query = pm.newQuery(SearchQuery.class);
  		query.setFilter("city == :cityParam");
  		query.setRange(0, MAX_RESULTS);
  		query.setOrdering("times desc");
  		printResult(resp, toString((List<SearchQuery>) query.execute(city)));  		
  		query.closeAll();
  	} finally {
  		pm.close();
  	}
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
  	resp.setContentType(CONTENT_TYPE);
  	resp.setCharacterEncoding(ENCODING);
   	resp.getWriter().println(value);
   	resp.getWriter().close();
  }
  
  /**
   * Returns the string representation of {@code list}.
   * 
   * <p> Each element of the list is separated by {@value DELIMITER}.
   * 
   * @param list
   * @return
   */
  private String toString(List<SearchQuery> list) {
  	StringBuilder builder = new StringBuilder();
  	for (SearchQuery query : list) {
  		builder.append(query.getQuery()).append(QUERY_DELIMITER);
  	}
  	return builder.toString();
  }
}
