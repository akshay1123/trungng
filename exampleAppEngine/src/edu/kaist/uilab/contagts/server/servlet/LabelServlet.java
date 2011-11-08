// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.contagts.server.servlet;

import java.io.IOException;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.kaist.uilab.contagts.server.ContactEntity;
import edu.kaist.uilab.contagts.server.Label;
import edu.kaist.uilab.contagts.server.LogMessage;
import edu.kaist.uilab.contagts.server.SharableContact;
import edu.kaist.uilab.contagts.server.TagPairHelper;

/**
 * Servlet for receiving request to "/label" and persist them into JDO datastore.
 * 
 * <p> This servlet writes data into 2 tables. One is "logmessage" table which is used
 * mainly for logging purpose whereas the other is "Label" which is used as part of the
 * network for generating search result.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class LabelServlet extends HttpServlet {
  
	private static final long serialVersionUID = 1L;
	private static final String PARAM_DEVICE_ID = "deviceid";
	private static final String PARAM_TIME = "time";
	private static final String PARAM_TYPE = "type";
	private static final String PARAM_CONTENT = "content";
	private static final String PARAM_ID = "id";
	
	private static final String TYPE_GROUP = "group";
	private static final String TYPE_TAG = "tag";
	
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
  	String deviceId = req.getParameter(PARAM_DEVICE_ID);
  	String type = req.getParameter(PARAM_TYPE);
  	String number = req.getParameter(PARAM_ID);
  	String content = req.getParameter(PARAM_CONTENT);
  	LogMessage msg = new LogMessage(deviceId, req.getParameter(PARAM_TIME),
  			type, content, number);
  	ServletUtils.makePersistent(msg);
  	PersistenceManager pm = ServletUtils.getPmf().getPersistenceManager();
  	try {
    	if (number != null) {
      	ContactEntity taggedEntity = ServletUtils.getContactByNumber(pm, number);
      	// if number can be resolved to some ContactEntity, update its labels
      	if (taggedEntity != null) {
        	if (TYPE_GROUP.equals(type)) {
        		taggedEntity.getLabels().add(new Label(deviceId, content));
        	} else if (TYPE_TAG.equals(type)) {
        		taggedEntity.getLabels().addAll(ServletUtils.toLabels(deviceId, content));
        		TagPairHelper.updateTagPairs(content);
        	}
      	}
      	// if number can be resolved to some SharableContact, update its labels
      	if (TYPE_TAG.equals(type)) {
      		updateSharableContact(number, deviceId, content);
      	}
      	// TODO(trung): if number cannot be resolved, optimistically save it
      	// because this user might join later!
      	ServletUtils.makePersistent(pm, taggedEntity);
    	}
  	} finally {
  		pm.close();
  	}
  }

  /**
   * Updates the sharable contact with labels in a single transaction.
   * 
   * @param number
   * @param deviceId
   * @param content
   */
  private void updateSharableContact(String number, String deviceId,
  		String content) {
		PersistenceManager sharablePm = ServletUtils.getPmf().getPersistenceManager();
		try {
  		SharableContact sharableContact = getSharableContact(sharablePm, number);
     	if (sharableContact != null) {
     		sharableContact.getLabels().addAll(ServletUtils.toLabels(deviceId, content));
     		sharableContact.updateStringLabels();
     	}
     	ServletUtils.makePersistent(sharablePm, sharableContact);
		} finally {
			sharablePm.close();
		}
  }
  
  /**
   * Returns the {@link SharableContact} whose phone number is {@code number}.
   * 
   * @return
   * 		null if no such entity exists
   */
	private SharableContact getSharableContact(PersistenceManager pm, String number) {
  	Query query = pm.newQuery(SharableContact.class);
  	query.setFilter("number == numberParam");
  	query.declareParameters("String numberParam");
  	query.setUnique(true);
  	try {
  		return (SharableContact) query.execute(number);
  	} finally {
  		query.closeAll();
  	}
  }
}
