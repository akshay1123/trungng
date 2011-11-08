// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.contagts.server.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.kaist.uilab.contagts.server.CallLog;
import edu.kaist.uilab.contagts.server.ContactEntity;
import edu.kaist.uilab.contagts.server.Label;
import edu.kaist.uilab.contagts.server.LogMessage;
import edu.kaist.uilab.contagts.server.SearchQuery;
import edu.kaist.uilab.contagts.server.SharableContact;
import edu.kaist.uilab.contagts.server.TagPair;

/**
 * Servlet for printing log content to the browser.
 * 
 * <p> This servlet serves request at /see.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class LogPrinterServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	static final String PARAM_VALUE_CONTACT_ENTITY = "contact";
	static final String PARAM_VALUE_LOG_MESSAGE = "labellog";
	static final String PARAM_VALUE_LABEL = "label";
	static final String PARAM_VALUE_CALL_LOG = "calllog";
	static final String PARAM_VALUE_SHARED = "shared";
	static final String PARAM_VALUE_TAG = "tag";
	static final String PARAM_VALUE_SEARCH = "search";
	
	static final String PARAM_ENTITY = "entity";
	
	@SuppressWarnings("unchecked")
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
  		throws IOException {
		String entity = req.getParameter(PARAM_ENTITY);
		String query = "";
		if (PARAM_VALUE_CALL_LOG.equals(entity)) {
			query = "select from " + CallLog.class.getName();
		} else if (PARAM_VALUE_LOG_MESSAGE.equals(entity)) {
			query = "select from " + LogMessage.class.getName();
		} else if (PARAM_VALUE_CONTACT_ENTITY.equals(entity)) {
			query = "select from " + ContactEntity.class.getName();
		} else if (PARAM_VALUE_SHARED.equals(entity)) {
			query = "select from " + SharableContact.class.getName();
		} else if (PARAM_VALUE_TAG.equals(entity)) {
			query = "select from " + TagPair.class.getName();
		} else if (PARAM_VALUE_SEARCH.equals(entity)){
			query = "select from " + SearchQuery.class.getName();
		} else if (PARAM_VALUE_LABEL.equals(entity)) {
			query = "select from " + Label.class.getName();
		}
		
		resp.setContentType("text/plain");
		resp.setCharacterEncoding("utf-8");
		PrintWriter writer = resp.getWriter();
		PersistenceManager pm = ServletUtils.getPmf().getPersistenceManager();
		List msgs = (List) pm.newQuery(query).execute();
		for (Object msg : msgs) {
			writer.println(msg);
		}
		writer.flush();
  }	
}
