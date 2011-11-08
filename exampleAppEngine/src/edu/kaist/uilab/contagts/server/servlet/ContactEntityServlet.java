// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.contagts.server.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.kaist.uilab.contagts.server.ContactEntity;

/**
 * Servlet for receiving request to "/entity" and persist them into JDO datastore.
 * 
 * TODO(trung): load the device id & number already logged before
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class ContactEntityServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final String PARAM_DEVICE_ID = "deviceid";
	private static final String PARAM_NUMBER = "number";

  public void doGet(HttpServletRequest req, HttpServletResponse resp)
  		throws IOException {
  	String deviceId = req.getParameter(PARAM_DEVICE_ID);
  	String number = req.getParameter(PARAM_NUMBER);
  	if (deviceId != null) {
  		ContactEntity entity = new ContactEntity(deviceId, number);
  		ServletUtils.makePersistent(entity);
  	}	
  }
}
