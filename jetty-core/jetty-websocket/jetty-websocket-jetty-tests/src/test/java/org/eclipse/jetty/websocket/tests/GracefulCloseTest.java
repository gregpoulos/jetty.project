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

package org.eclipse.jetty.websocket.tests;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.util.WSURI;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GracefulCloseTest
{
    private final EventSocket serverEndpoint = new EchoSocket();
    private Server server;
    private URI serverUri;
    private WebSocketClient client;

    @BeforeEach
    public void before() throws Exception
    {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        server.addConnector(connector);

        ContextHandler context = new ContextHandler("/");

        WebSocketUpgradeHandler wsHandler = WebSocketUpgradeHandler.from(server, context);
        context.setHandler(wsHandler);
        wsHandler.configure(container ->
            container.addMapping("/", (rq, rs, cb) -> serverEndpoint));

        server.setHandler(context);
        server.start();
        serverUri = WSURI.toWebsocket(server.getURI());

        // StopTimeout is necessary for the websocket server sessions to gracefully close.
        server.setStopTimeout(1000);

        client = new WebSocketClient();
        client.setStopTimeout(1000);
        client.start();
    }

    @AfterEach
    public void after() throws Exception
    {
        client.stop();
        server.stop();
    }

    @Test
    public void testClientStop() throws Exception
    {
        EventSocket clientEndpoint = new EventSocket();
        client.connect(clientEndpoint, serverUri).get(5, TimeUnit.SECONDS);

        client.stop();

        // Check that the client endpoint was closed with the correct status code and no error.
        assertTrue(clientEndpoint.closeLatch.await(5, TimeUnit.SECONDS));
        assertThat(clientEndpoint.closeCode, is(StatusCode.SHUTDOWN));
        assertNull(clientEndpoint.error);

        // Check that the server endpoint was closed with the correct status code and no error.
        assertTrue(serverEndpoint.closeLatch.await(5, TimeUnit.SECONDS));
        assertThat(serverEndpoint.closeCode, is(StatusCode.SHUTDOWN));
        assertNull(serverEndpoint.error);
    }

    @Test
    public void testServerStop() throws Exception
    {
        EventSocket clientEndpoint = new EventSocket();
        client.connect(clientEndpoint, serverUri).get(5, TimeUnit.SECONDS);

        server.stop();

        // Check that the client endpoint was closed with the correct status code and no error.
        assertTrue(clientEndpoint.closeLatch.await(5, TimeUnit.SECONDS));
        assertThat(clientEndpoint.closeCode, is(StatusCode.SHUTDOWN));
        assertNull(clientEndpoint.error);

        // Check that the server endpoint was closed with the correct status code and no error.
        assertTrue(serverEndpoint.closeLatch.await(5, TimeUnit.SECONDS));
        assertThat(serverEndpoint.closeCode, is(StatusCode.SHUTDOWN));
        assertNull(serverEndpoint.error);
    }
}
