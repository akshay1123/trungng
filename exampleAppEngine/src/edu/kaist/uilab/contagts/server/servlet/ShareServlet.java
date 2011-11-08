// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.contagts.server.servlet;

import java.io.IOException;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.kaist.uilab.contagts.server.Label;
import edu.kaist.uilab.contagts.server.SharableContact;

/**
 * Servlet for handling request at "/share" for sharing contacts.
 * 
 * <p> By this request, user can specify the information about the contact this he
 * want to share including {@code name, number, email, address, tags} as well as the
 * groups of people that he wants to share with.
 * 
 * <p> The groups that are shared with is specified by the {@code groups} parameter
 * which must conform to the following rule. (1) If share to public, {@code groups} parameter
 * should be null (i.e., not specified). (2) If share to particular groups, groups are
 * separated by "-" and the entire list is enclosed by []. Example: [group1-g2-]
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class ShareServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	private final String PARAM_DEVICEID = "deviceid";
	private final String PARAM_NUMBER = "number";
	private final String PARAM_NAME = "name";
	private final String PARAM_EMAIL = "email";
	private final String PARAM_ADDRESS = "address";
	private final String PARAM_LABELS = "labels";
	private final String PARAM_GROUPS = "groups";
	private final String PARAM_CITY = "city";

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException {
		String deviceId = req.getParameter(PARAM_DEVICEID);
  	String number = req.getParameter(PARAM_NUMBER);
  	String name = req.getParameter(PARAM_NAME);
  	String email = req.getParameter(PARAM_EMAIL);
  	String address = req.getParameter(PARAM_ADDRESS);
  	String labels = req.getParameter(PARAM_LABELS);
  	String groups = req.getParameter(PARAM_GROUPS);
  	String city = req.getParameter(PARAM_CITY);
  	
		PersistenceManager pm = ServletUtils.getPmf().getPersistenceManager();
		try {
			SharableContact contact = null;
			if (number != null) {
				contact = getContactByNumber(pm, number);
			}
			if (contact == null && email != null) {
				contact = getContactByEmail(pm, email);
			}
			if (contact == null) {
				contact = new SharableContact(email, name, number, address, city);
			} else {
				// if there is new supplementary data, update
				if (name != null) {
					contact.setName(name);
				}
				if (email != null) {
					contact.setEmail(email);
				}
				if (number != null) {
					contact.setNumber(number);
				}
				if (address != null) {
					contact.setAddress(address);
				}
			}
			// add labels and groups for contact and persist
			if (labels != null) {
				List<Label> newLabels = ServletUtils.toLabels(deviceId, labels);
				contact.getLabels().addAll(newLabels);
				contact.updateStringLabels();
			}
			if (groups != null) {
				contact.getGroups().addAll(ServletUtils.toGroups(deviceId, groups));
			} else {
				contact.makePublic();	// user makes this contact public
			}
			ServletUtils.makePersistent(pm, contact);
		} finally {
			pm.close();
		}
  }
	
	/**
	 * Gets the {@link SharableContact} whose number matches {@code number}.
	 * 
	 * @param pm a {@link PersistenceManager}
	 * @param number
	 * @return
	 * 		the contact if it exists, otherwise returns null
	 */
	private SharableContact getContactByNumber(PersistenceManager pm, String number) {
		Query query = pm.newQuery(SharableContact.class);
		query.setFilter("number == numberParam");
		query.declareParameters("String numberParam");
		query.setUnique(true);
		try {
			return ((SharableContact) query.execute(number));
		} finally {
			query.closeAll();
		}
	}
	
	/**
	 * Gets the {@link SharableContact} whose email matches {@code email}.
	 * 
	 * @param pm a {@link PersistenceManager}
	 * @param email
	 * @return
	 * 		the contact if it exists, otherwise returns null
	 */
	private SharableContact getContactByEmail(PersistenceManager pm, String email) {
		Query query = pm.newQuery(SharableContact.class);
		query.setFilter("email == emailParam");
		query.declareParameters("String emailParam");
		query.setUnique(true);
		try {
			return (SharableContact) query.execute(email);
		} finally {
			query.closeAll();
		}
	}
}
