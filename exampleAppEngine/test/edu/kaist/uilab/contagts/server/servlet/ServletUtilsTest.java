package edu.kaist.uilab.contagts.server.servlet;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;

import org.easymock.IMocksControl;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

import edu.kaist.uilab.contagts.server.ContactEntity;

public class ServletUtilsTest {
	
	private final LocalServiceTestHelper helper = new LocalServiceTestHelper(
			new LocalDatastoreServiceTestConfig());
	private PersistenceManager mockPm;
	private Query mockQuery;
	private IMocksControl control;
	
	@Before
	public void setUp() {
		helper.setUp();
		control = createControl();
		mockPm = control.createMock(PersistenceManager.class);
		mockQuery = control.createMock(Query.class);
	}
	
	@After
	public void tearDown() {
		helper.tearDown();
	}

	@Test
	public void testMakePersistent() {
		// TODO(trung): probably use the local database
		// and then test against those database (using the helper.createDatastoreService)
		ContactEntity expected = new ContactEntity("11111", "11111"); 
		expect(mockPm.newQuery(ContactEntity.class)).andReturn(mockQuery);
		mockQuery.setFilter("number == numberParam");
		mockQuery.declareParameters("String numberParam");
		mockQuery.setUnique(true);
		mockQuery.closeAll();
		expect(mockQuery.execute("11111")).andReturn(expected);
		control.replay();
		ContactEntity result = ServletUtils.getContactByNumber(mockPm, "11111");
		control.verify();
		assertEquals(result.getDeviceId(), expected.getDeviceId());
	}
}
