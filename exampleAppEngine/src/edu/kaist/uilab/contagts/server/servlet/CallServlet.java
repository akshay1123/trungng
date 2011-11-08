// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.contagts.server.servlet;

import java.io.IOException;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.kaist.uilab.contagts.server.CallLog;
import edu.kaist.uilab.contagts.server.ContactEntity;
import edu.kaist.uilab.contagts.server.Friend;

/**
 * Servlet for receiving request to "/calllog" and persist them into JDO datastore.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public class CallServlet extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
	
	private static final String PARAM_DEVICE_ID = "deviceid";
	private static final String PARAM_NUMBER = "number";
	private static final String PARAM_DURATION = "duration";
	private static final String PARAM_DATE = "date";

  public void doGet(HttpServletRequest req, HttpServletResponse resp)
  		throws IOException {
  	String deviceId = req.getParameter(PARAM_DEVICE_ID);
  	String number = req.getParameter(PARAM_NUMBER);
  	long date = Long.parseLong(req.getParameter(PARAM_DATE));
  	int seconds = Integer.parseInt(req.getParameter(PARAM_DURATION));
  	
		PersistenceManager contactPm = ServletUtils.getPmf().getPersistenceManager();
		PersistenceManager callPm = ServletUtils.getPmf().getPersistenceManager();
		try {
			ContactEntity contact = ServletUtils.getContactByDeviceId(contactPm, deviceId);
			ContactEntity friend = ServletUtils.getContactByNumber(contactPm, number);
			float mins = ((float) seconds) / 60;
			if (contact.getNumber() != null && friend != null) {
				// both are known so just update the device's friend
				int pos = getFriend(contact, friend.getDeviceId());
				if (pos >= 0) {
					Friend updatingFriend = contact.getFriends().get(pos);
					updatingFriend.setCalls(updatingFriend.getCalls() + 1);
					updatingFriend.addMinutes(mins);
				} else {
					Friend newFriend = new Friend(friend.getDeviceId(), mins, 1);
					contact.getFriends().add(newFriend);
				}
			} else {
				// try to resolve both
				CallLog counterpartCall = null;
				if (friend != null) {
					counterpartCall = getCallLogForDevice(callPm, friend.getDeviceId(), seconds, date);
				} else if (contact.getNumber() != null) {
					counterpartCall = getCallLogForNumber(callPm, contact.getNumber(), seconds, date);
				}
				if (counterpartCall != null) {
					// entity & calllog are available -> update phone number and friends for both
					contact.setNumber(counterpartCall.getNumber());
					contact.getFriends().add(new Friend(counterpartCall.getDeviceId(), mins, 1));
					if (friend == null) {
						friend = new ContactEntity(counterpartCall.getDeviceId(), number);
					}
					friend.getFriends().add(new Friend(deviceId, mins, 1));
					contactPm.makePersistent(friend);
					callPm.deletePersistent(counterpartCall);
				} else {
					// fail to resolve both numbers so log for later attempt
					callPm.makePersistent(new CallLog(deviceId, number, seconds, date));
				}
			}
		} finally {
			callPm.close();
			contactPm.close();
		}
	}
  
	@SuppressWarnings("unchecked")
	private CallLog getCallLogForNumber(PersistenceManager pm, String number, int duration,
			long date) {
  	Query query = pm.newQuery(CallLog.class);
  	// note that property must go before param name
  	query.setFilter("number == numberParam && duration == durationParam" +
  			" && date >= minDateParam && date <= maxDateParam");
  	query.declareParameters("String numberParam, int durationParam, long minDateParam," +
  			" long maxDateParam");
  	try {
  		// the same call is one that was placed within 2 seconds of the time of this call
  		Object[] params = new Object[4];
  		params[0] = number;
  		params[1] = duration;
  		params[2] = date - 2000;
  		params[3] = date + 2000;
  		List<CallLog> results = (List<CallLog>) query.executeWithArray(params);
  		if (results != null && results.size() > 0) {
  			return results.get(0);
  		}
  	} finally {
  		query.closeAll();
  	}
  	
  	return null;
  }
  
	@SuppressWarnings("unchecked")
	private CallLog getCallLogForDevice(PersistenceManager pm, String deviceId, int duration,
			long date) {
  	Query query = pm.newQuery(CallLog.class);
  	// note that property must go before param name
  	query.setFilter("deviceId == deviceParam && duration == durationParam" +
  			" && date >= minDateParam && date <= maxDateParam");
  	query.declareParameters("String deviceParam, int durationParam, long minDateParam," +
  			" long maxDateParam");
  	try {
  		// the same call is one that was placed within 2 seconds of the time of this call
  		Object[] params = new Object[4];
  		params[0] = deviceId;
  		params[1] = duration;
  		params[2] = date - 2000;
  		params[3] = date + 2000;
  		List<CallLog> results = (List<CallLog>) query.executeWithArray(params);
  		if (results != null && results.size() > 0) {
  			return results.get(0);
  		}
  	} finally {
  		query.closeAll();
  	}
  	
  	return null;
  }
  
  /**
   * Returns the position of the friend of {@code entity} whose device id matches {@code deviceId},
   * if any.
   *  
   * @param entity
   * @param deviceId
   * @return
   * 			the position of the friend, -1 if {@code entity} does not have this friend
   */
  private int getFriend(ContactEntity entity, String deviceId) {
  	List<Friend> friends = entity.getFriends();
  	for (int i = 0; i < friends.size(); i++) {
  		if (friends.get(i).getDeviceId().equals(deviceId)) {
  			return i;
  		}
  	}
  	
  	return -1;
  }
}
