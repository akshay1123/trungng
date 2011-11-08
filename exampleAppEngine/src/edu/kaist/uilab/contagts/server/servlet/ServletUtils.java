// Copyright (C) 2010 U&I Lab, CS Dept., KAIST.

package edu.kaist.uilab.contagts.server.servlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.persistence.Persistence;

import edu.kaist.uilab.contagts.server.ContactEntity;
import edu.kaist.uilab.contagts.server.Group;
import edu.kaist.uilab.contagts.server.Label;

/**
 * Util class for common methods used by servlets.
 * 
 * @author Trung Nguyen (trung.ngvan@gmail.com)
 */
public final class ServletUtils {
	
	private static final String DELIMITER = "*";
	
  private static final PersistenceManagerFactory pmf =
  	JDOHelper.getPersistenceManagerFactory("transactions-optional");
  
  /**
   * Returns the unique {@link PersistenceManagerFactory} for this app.
   * 
   * @return
   */
  public static PersistenceManagerFactory getPmf() {
  	return pmf;
  }

	/**
	 * Persists {@code entity} to the datastore in a transaction.
	 *  
	 * @param pc a {@link Persistence} object
	 */
	public static void makePersistent(Object entity) {
		PersistenceManager pm = getPmf().getPersistenceManager();
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			pm.makePersistent(entity);
			tx.commit();
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
		}
		pm.close();
	}
  
	/**
	 * Persists {@code entity} to the datastore in a transaction.
	 *  
	 * @param pc a {@link Persistence} object
	 */
	public static void makePersistent(PersistenceManager pm, Object entity) {
		Transaction tx = null;
		try {
			tx = pm.currentTransaction();
			tx.begin();
			pm.makePersistent(entity);
			tx.commit();
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
		}
	}
	
  /**
   * Returns the {@link ContactEntity} whose phone number matches {@code number}.
   * 
   * @return
   * 		null if no such entity exists
   */
	public static ContactEntity getContactByNumber(PersistenceManager pm, String number) {
  	Query query = pm.newQuery(ContactEntity.class);
  	query.setFilter("number == numberParam");
  	query.declareParameters("String numberParam");
  	query.setUnique(true);
  	try {
  		return (ContactEntity) query.execute(number);
  	} finally {
  		query.closeAll();
  	}
  }

  /**
   * Returns the {@link ContactEntity} whose id matches {@code deviceId}.
   * 
   * @return
   * 		null if no such entity exists
   */
	public static ContactEntity getContactByDeviceId(PersistenceManager pm, String deviceId) {
  	Query query = pm.newQuery(ContactEntity.class);
  	query.setFilter("id == idParam");
  	query.declareParameters("String idParam");
  	query.setUnique(true);
  	try {
  		return (ContactEntity) query.execute(deviceId);
  	} finally {
  		query.closeAll();
  	}
  }
  
  /**
   * Constructs the list of {@code Label} from the tags.
   * 
   * <p> The content of {@code tags} as submitted by client conforms to this format:
   * "[tag1*tag2*]"
   * 
   * @param tagger device id of the entity that created {@code tags}
   * @param tags the string containing tags in specified format
   * @return
   */
  public static List<Label> toLabels(String tagger, String tags) {
  	StringTokenizer tokenizer = new StringTokenizer(tags.substring(1, tags.length() - 1),
  			DELIMITER);
  	ArrayList<Label> labels = new ArrayList<Label>();
  	while (tokenizer.hasMoreTokens()) {
  		labels.add(new Label(tagger, tokenizer.nextToken()));
  	}
  	
  	return labels;
  }

  /**
   * Constructs the list of {@code Group} from {@code groups}.
   * 
   * <p> The content of {@code groups} as submitted by client conforms to this format:
   * "[tag1*tag2*]"
   * 
   * @param deviceId device id of the entity that created {@code groups}
   * @param groups the string containing group names in specified format
   * @return
   */
  public static List<Group> toGroups(String deviceId, String groups) {
  	StringTokenizer tokenizer = new StringTokenizer(groups.substring(1, groups.length() - 1),
  			DELIMITER);
  	ArrayList<Group> result = new ArrayList<Group>();
  	while (tokenizer.hasMoreTokens()) {
  		result.add(new Group(deviceId, tokenizer.nextToken()));
  	}
  	
  	return result;
  }
  
	/**
	 * Returns a string for the given collection. Useful for debugging purpose.
	 * 
	 * @param <T>
	 * @param col
	 * @return
	 */
	public static <T> String toString(Collection<T> col) {
		StringBuilder builder = new StringBuilder("[");
		for (T t : col) {
			builder.append(t).append(DELIMITER);
		}
		builder.append("]");
		
		return builder.toString();
	}

	/**
	 * Returns a string for the given iterator. Useful for debugging purpose.
	 * 
	 * @param <T>
	 * @param iter
	 * @return
	 */
	public static <T> String toLines(Iterator<T> iter) {
		StringBuilder builder = new StringBuilder();
		while (iter.hasNext()) {
			builder.append("\n[").append(iter.next()).append("]");
		}
		
		return builder.toString();
	}
}
