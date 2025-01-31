//
// ========================================================================
// Copyright (c) 1995 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.ee9.session.jdbc;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.eclipse.jetty.ee9.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.session.DatabaseAdaptor;
import org.eclipse.jetty.session.DefaultSessionIdManager;
import org.eclipse.jetty.session.JDBCSessionDataStore;
import org.eclipse.jetty.session.JdbcTestHelper;
import org.eclipse.jetty.session.SessionContext;
import org.eclipse.jetty.util.NanoTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the SessionTableSchema behaviour when the database treats "" as a NULL,
 * like Oracle does.
 */
@Testcontainers(disabledWithoutDocker = true)
public class SessionTableSchemaTest
{
    DatabaseAdaptor _da;
    JDBCSessionDataStore.SessionTableSchema _tableSchema;

    private String sessionTableName;

    @BeforeEach
    public void setUp() throws Exception
    {
        this.sessionTableName = getClass().getSimpleName() + "_" + System.nanoTime();
        //pretend to be an Oracle-like database that treats "" as NULL
        _da = new DatabaseAdaptor()
        {

            @Override
            public boolean isEmptyStringNull()
            {
                return true; //test special handling for oracle
            }
        };
        _da.setDriverInfo(JdbcTestHelper.DRIVER_CLASS, JdbcTestHelper.DEFAULT_CONNECTION_URL);
        _tableSchema = JdbcTestHelper.newSessionTableSchema(sessionTableName);
        JdbcTestHelper.setDatabaseAdaptor(_tableSchema, _da);
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        JdbcTestHelper.shutdown(sessionTableName);
    }

    /**
     * This inserts a session into the db that does not set the session attributes MAP column. As such
     * this results in a row that is unreadable by the JDBCSessionDataStore, but is readable by using
     * only jdbc api, which is what this test does.
     * 
     * @param id id of session
     * @param contextPath the context path of the session
     * @param vhost the virtual host of the session 
     * @throws Exception
     */
    public void insertSessionWithoutAttributes(String id, String contextPath, String vhost)
        throws Exception
    {
        try (Connection con = JdbcTestHelper.getConnection())
        {
            PreparedStatement statement = con.prepareStatement("insert into " + sessionTableName +
                " (" + JdbcTestHelper.ID_COL + ", " + JdbcTestHelper.CONTEXT_COL + ", virtualHost, " + JdbcTestHelper.LAST_NODE_COL +
                ", " + JdbcTestHelper.ACCESS_COL + ", " + JdbcTestHelper.LAST_ACCESS_COL + ", " + JdbcTestHelper.CREATE_COL + ", " + JdbcTestHelper.COOKIE_COL +
                ", " + JdbcTestHelper.LAST_SAVE_COL + ", " + JdbcTestHelper.EXPIRY_COL + " " + ") " +
                " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            statement.setString(1, id);
            statement.setString(2, contextPath);
            statement.setString(3, vhost);
            statement.setString(4, "0");

            statement.setLong(5, System.currentTimeMillis());
            statement.setLong(6, System.currentTimeMillis());
            statement.setLong(7, System.currentTimeMillis());
            statement.setLong(8, System.currentTimeMillis());

            statement.setLong(9, System.currentTimeMillis());
            statement.setLong(10, System.currentTimeMillis());

            statement.execute();
            assertEquals(1, statement.getUpdateCount());
        }
    }
    
    @Test
    public void testLoad()
        throws Exception
    {
        //set up the db
        _da.initialize();
        _tableSchema.prepareTables();

        String id = Long.toString(NanoTime.now());

        //insert a fake session at the root context
        insertSessionWithoutAttributes(id, "/", "0.0.0.0");

        //test if it can be seen
        try (Connection con = JdbcTestHelper.getConnection(_da))
        {
            //make a root context
            ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
            handler.getSessionHandler().getSessionManager().setSessionIdManager(new DefaultSessionIdManager(new Server()));
            handler.setContextPath("/");
            SessionContext sc = new SessionContext(handler.getSessionHandler().getSessionManager());
            //test the load statement
            PreparedStatement s = _tableSchema.getLoadStatement(con, id, sc);
            ResultSet rs = s.executeQuery();
            assertTrue(rs.next());
        }
    }

    @Test
    public void testExists()
        throws Exception
    {
        //set up the db
        _da.initialize();
        _tableSchema.prepareTables();

        String id = Long.toString(NanoTime.now());

        //insert a fake session at the root context
        insertSessionWithoutAttributes(id, "/", "0.0.0.0");

        //test if it can be seen
        try (Connection con = JdbcTestHelper.getConnection(_da))
        {
            ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
            handler.getSessionHandler().getSessionManager().setSessionIdManager(new DefaultSessionIdManager(new Server()));
            handler.setContextPath("/");
            SessionContext sc = new SessionContext(handler.getSessionHandler().getSessionManager());
            PreparedStatement s = _tableSchema.getCheckSessionExistsStatement(con, sc);
            s.setString(1, id);
            ResultSet rs = s.executeQuery();
            assertTrue(rs.next());
        }
    }

    @Test
    public void testDelete()
        throws Exception
    {
        //set up the db
        _da.initialize();
        _tableSchema.prepareTables();

        String id = Long.toString(NanoTime.now());

        //insert a fake session at the root context
        insertSessionWithoutAttributes(id, "/", "0.0.0.0");

        //test if it can be deleted
        try (Connection con = JdbcTestHelper.getConnection(_da))
        {
            ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
            handler.getSessionHandler().getSessionManager().setSessionIdManager(new DefaultSessionIdManager(new Server()));
            handler.setContextPath("/");
            SessionContext sc = new SessionContext(handler.getSessionHandler().getSessionManager());
            PreparedStatement s = _tableSchema.getDeleteStatement(con, id, sc);
            assertEquals(1, s.executeUpdate());

            assertFalse(JdbcTestHelper.existsInSessionTable(id, false, sessionTableName));
        }
    }

    @Test
    public void testExpired()
        throws Exception
    {
        //set up the db
        _da.initialize();
        _tableSchema.prepareTables();

        String id = Long.toString(NanoTime.now());

        //insert a fake session at the root context
        insertSessionWithoutAttributes(id, "/", "0.0.0.0");

        try (Connection con = JdbcTestHelper.getConnection(_da))
        {
            ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
            handler.getSessionHandler().getSessionManager().setSessionIdManager(new DefaultSessionIdManager(new Server()));
            handler.setContextPath("/");
            SessionContext sc = new SessionContext(handler.getSessionHandler().getSessionManager());
            PreparedStatement s = _tableSchema.getExpiredSessionsStatement(con,
                sc.getCanonicalContextPath(),
                sc.getVhost(),
                (System.currentTimeMillis() + 100L));
            ResultSet rs = s.executeQuery();
            assertTrue(rs.next());
            assertEquals(id, rs.getString(1));
        }
    }

    @Test
    public void testMyExpiredSessions()
        throws Exception
    {
        //set up the db
        _da.initialize();
        _tableSchema.prepareTables();

        String id = Long.toString(NanoTime.now());

        //insert a fake session at the root context
        insertSessionWithoutAttributes(id, "/", "0.0.0.0");

        try (Connection con = JdbcTestHelper.getConnection(_da))
        {
            ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
            DefaultSessionIdManager idMgr = new DefaultSessionIdManager(new Server());
            idMgr.setWorkerName("0");
            handler.getSessionHandler().getSessionManager().setSessionIdManager(idMgr);
            handler.setContextPath("/");
            SessionContext sc = new SessionContext(handler.getSessionHandler().getSessionManager());
            PreparedStatement s = _tableSchema.getMyExpiredSessionsStatement(con,
                sc,
                (System.currentTimeMillis() + 100L));
            ResultSet rs = s.executeQuery();
            assertTrue(rs.next());
            assertEquals(id, rs.getString(1));
        }
    }

    @Test
    public void testUpdate()
        throws Exception
    {
        //set up the db
        _da.initialize();
        _tableSchema.prepareTables();

        String id = Long.toString(NanoTime.now());

        //insert a fake session at the root context
        insertSessionWithoutAttributes(id, "/", "0.0.0.0");

        try (Connection con = JdbcTestHelper.getConnection(_da))
        {
            ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
            handler.getSessionHandler().getSessionManager().setSessionIdManager(new DefaultSessionIdManager(new Server()));
            handler.setContextPath("/");
            SessionContext sc = new SessionContext(handler.getSessionHandler().getSessionManager());
            PreparedStatement s = _tableSchema.getUpdateStatement(con,
                id,
                sc);

            s.setString(1, "0"); //should be my node id
            s.setLong(2, System.currentTimeMillis());
            s.setLong(3, System.currentTimeMillis());
            s.setLong(4, System.currentTimeMillis());
            s.setLong(5, System.currentTimeMillis());
            s.setLong(6, 2000L);

            byte[] bytes = new byte[3];
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            s.setBinaryStream(7, bais, bytes.length); //attribute map as blob

            assertEquals(1, s.executeUpdate());
        }
    }
}
