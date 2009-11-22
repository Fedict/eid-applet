/*
 * eID Applet Project.
 * Copyright (C) 2008-2009 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

package test.unit.be.fedict.eid.applet.beta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ejb.Ejb3Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import be.fedict.eid.applet.beta.FeedbackEntity;
import be.fedict.eid.applet.beta.SessionContextEntity;
import be.fedict.eid.applet.beta.TestReportEntity;
import be.fedict.eid.applet.beta.TestResultEntity;

public class PersistenceTest {

	private static final Log LOG = LogFactory.getLog(PersistenceTest.class);

	private EntityManager entityManager;

	@Before
	public void setUp() throws Exception {
		Class.forName("org.hsqldb.jdbcDriver");
		Ejb3Configuration configuration = new Ejb3Configuration();
		configuration.setProperty("hibernate.dialect",
				"org.hibernate.dialect.HSQLDialect");
		configuration.setProperty("hibernate.connection.driver_class",
				"org.hsqldb.jdbcDriver");
		configuration.setProperty("hibernate.connection.url",
				"jdbc:hsqldb:mem:beta");
		configuration.setProperty("hibernate.hbm2ddl.auto", "create");
		configuration.addAnnotatedClass(SessionContextEntity.class);
		configuration.addAnnotatedClass(FeedbackEntity.class);
		configuration.addAnnotatedClass(TestResultEntity.class);
		configuration.addAnnotatedClass(TestReportEntity.class);
		EntityManagerFactory entityManagerFactory = configuration
				.buildEntityManagerFactory();

		this.entityManager = entityManagerFactory.createEntityManager();
		this.entityManager.getTransaction().begin();
	}

	@After
	public void tearDown() throws Exception {
		EntityTransaction entityTransaction = this.entityManager
				.getTransaction();
		LOG.debug("entity manager open: " + this.entityManager.isOpen());
		LOG.debug("entity transaction active: " + entityTransaction.isActive());
		if (entityTransaction.isActive()) {
			if (entityTransaction.getRollbackOnly()) {
				entityTransaction.rollback();
			} else {
				entityTransaction.commit();
			}
		}
		this.entityManager.close();
	}

	@Test
	public void sessionContextEntityAutogeneratedContextId() throws Exception {
		SessionContextEntity sessionContextEntity = new SessionContextEntity(
				"http-session-id", "user-agent");

		this.entityManager.persist(sessionContextEntity);
		LOG.debug("context id: " + sessionContextEntity.getContextId());
		assertEquals(1, sessionContextEntity.getContextId());

		SessionContextEntity sessionContextEntity2 = new SessionContextEntity(
				"http-session-id-2", "user-agent");
		this.entityManager.persist(sessionContextEntity2);
		LOG.debug("context id: " + sessionContextEntity2.getContextId());
		assertEquals(2, sessionContextEntity2.getContextId());
	}

	@Test
	public void sessionContextEntityHttpSessionIdNullFails() throws Exception {
		SessionContextEntity sessionContextEntity = new SessionContextEntity(
				null, "user-agent");

		try {
			this.entityManager.persist(sessionContextEntity);
			fail();
		} catch (Exception e) {
			// expected
		}
	}

	@Test
	public void sessionContextEntityUserAgentNullFails() throws Exception {
		SessionContextEntity sessionContextEntity = new SessionContextEntity(
				"http-session-id", null);

		try {
			this.entityManager.persist(sessionContextEntity);
			fail();
		} catch (Exception e) {
			// expected
		}
	}

	@Test
	public void feedbackEntity() throws Exception {
		SessionContextEntity sessionContextEntity = new SessionContextEntity(
				"http-session-id", "user-agent");
		this.entityManager.persist(sessionContextEntity);
		int contextId = sessionContextEntity.getContextId();

		this.entityManager.getTransaction().commit();
		this.entityManager.getTransaction().begin();

		sessionContextEntity = this.entityManager.find(
				SessionContextEntity.class, contextId);

		FeedbackEntity feedbackEntity = new FeedbackEntity();
		feedbackEntity.setEmail("email");
		feedbackEntity.setSubject("subject");
		feedbackEntity.setMessage("message");
		feedbackEntity.setCreated(Calendar.getInstance());
		feedbackEntity.setSessionContext(sessionContextEntity);

		this.entityManager.persist(feedbackEntity);
		int feedbackId = feedbackEntity.getId();

		this.entityManager.getTransaction().commit();
		this.entityManager.getTransaction().begin();

		feedbackEntity = this.entityManager.find(FeedbackEntity.class,
				feedbackId);
		assertNotNull(feedbackEntity.getSessionContext());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testResults() throws Exception {
		SessionContextEntity sessionContextEntity = new SessionContextEntity(
				"http-session-id", "user-agent");
		this.entityManager.persist(sessionContextEntity);

		TestResultEntity testResultEntity = new TestResultEntity("test",
				"result", sessionContextEntity);
		this.entityManager.persist(testResultEntity);

		TestResultEntity testResultEntity2 = new TestResultEntity("test2",
				"result2", sessionContextEntity);
		this.entityManager.persist(testResultEntity2);

		Query query = this.entityManager
				.createQuery("FROM TestResultEntity AS tr WHERE tr.sessionContext = :sessionContext");
		query.setParameter("sessionContext", sessionContextEntity);
		List<TestResultEntity> testResults = query.getResultList();

		// verify
		assertEquals(2, testResults.size());
	}

	@Test
	public void testTestReportQuery() throws Exception {
		// setup
		TestReportEntity testReportEntity = new TestReportEntity("javaVersion",
				"javaVendor", "osName", "osArch", "osVersion", "userAgent",
				"navigatorAppName", "navigatorAppVersion", "navigatorUserAgent");
		this.entityManager.persist(testReportEntity);
		LOG.debug("id: " + testReportEntity.getId());

		// operate
		Query query = this.entityManager
				.createNamedQuery(TestReportEntity.QUERY_TEST_REPORT);
		List<TestReportEntity> resultList = query.getResultList();

		// verify
		assertEquals(1, resultList.size());
	}
}
