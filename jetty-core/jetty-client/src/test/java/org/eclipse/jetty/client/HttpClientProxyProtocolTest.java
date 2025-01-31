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

package org.eclipse.jetty.client;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.eclipse.jetty.client.transport.HttpDestination;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpHeaderValue;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ProxyConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.eclipse.jetty.client.ProxyProtocolClientConnectionFactory.V1;
import static org.eclipse.jetty.client.ProxyProtocolClientConnectionFactory.V2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpClientProxyProtocolTest
{
    private ArrayByteBufferPool.Tracking serverBufferPool;
    private Server server;
    private ServerConnector connector;
    private ArrayByteBufferPool.Tracking clientBufferPool;
    private HttpClient client;

    private void startServer(Handler handler) throws Exception
    {
        QueuedThreadPool serverThreads = new QueuedThreadPool();
        serverThreads.setName("server");
        serverBufferPool = new ArrayByteBufferPool.Tracking();
        server = new Server(serverThreads, null, serverBufferPool);
        HttpConnectionFactory http = new HttpConnectionFactory();
        ProxyConnectionFactory proxy = new ProxyConnectionFactory(http.getProtocol());
        connector = new ServerConnector(server, 1, 1, proxy, http);
        server.addConnector(connector);
        server.setHandler(handler);
        server.start();
    }

    private void startClient() throws Exception
    {
        QueuedThreadPool clientThreads = new QueuedThreadPool();
        clientThreads.setName("client");
        clientBufferPool = new ArrayByteBufferPool.Tracking();
        client = new HttpClient();
        client.setExecutor(clientThreads);
        client.setByteBufferPool(clientBufferPool);
        client.start();
    }

    @AfterEach
    public void dispose() throws Exception
    {
        LifeCycle.stop(client);
        LifeCycle.stop(server);
        Set<ArrayByteBufferPool.Tracking.Buffer> serverLeaks = serverBufferPool.getLeaks();
        assertEquals(0, serverLeaks.size(), serverBufferPool.dumpLeaks());
        Set<ArrayByteBufferPool.Tracking.Buffer> clientLeaks = clientBufferPool.getLeaks();
        assertEquals(0, clientLeaks.size(), clientBufferPool.dumpLeaks());
    }

    @Test
    public void testClientProxyProtocolV1() throws Exception
    {
        startServer(new Handler.Abstract()
        {
            @Override
            public boolean handle(Request request, Response response, Callback callback)
            {
                response.getHeaders().put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_PLAIN.asString());
                Content.Sink.write(response, true, String.valueOf(Request.getRemotePort(request)), callback);
                return true;
            }
        });
        startClient();

        int serverPort = connector.getLocalPort();

        int clientPort = ThreadLocalRandom.current().nextInt(1024, 65536);
        V1.Tag tag = new V1.Tag("127.0.0.1", clientPort);

        ContentResponse response = client.newRequest("localhost", serverPort)
            .tag(tag)
            .send();
        assertEquals(HttpStatus.OK_200, response.getStatus());
        assertEquals(String.valueOf(clientPort), response.getContentAsString());
    }

    @Test
    public void testClientProxyProtocolV1Unknown() throws Exception
    {
        startServer(new EmptyServerHandler());
        startClient();

        ContentResponse response = client.newRequest("localhost", connector.getLocalPort())
            .tag(V1.Tag.UNKNOWN)
            .send();
        assertEquals(HttpStatus.OK_200, response.getStatus());
    }

    @Test
    public void testClientProxyProtocolV2() throws Exception
    {
        startServer(new Handler.Abstract()
        {
            @Override
            public boolean handle(Request request, Response response, Callback callback)
            {
                response.getHeaders().put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_PLAIN.asString());
                Content.Sink.write(response, true, String.valueOf(Request.getRemotePort(request)), callback);
                return true;
            }
        });
        startClient();

        int serverPort = connector.getLocalPort();

        int clientPort = ThreadLocalRandom.current().nextInt(1024, 65536);
        V2.Tag tag = new V2.Tag("127.0.0.1", clientPort);

        ContentResponse response = client.newRequest("localhost", serverPort)
            .tag(tag)
            .send();
        assertEquals(HttpStatus.OK_200, response.getStatus());
        assertEquals(String.valueOf(clientPort), response.getContentAsString());
    }

    @Test
    public void testClientProxyProtocolV2Local() throws Exception
    {
        startServer(new EmptyServerHandler());
        startClient();

        ContentResponse response = client.newRequest("localhost", connector.getLocalPort())
            .tag(V2.Tag.LOCAL)
            .send();
        assertEquals(HttpStatus.OK_200, response.getStatus());
    }

    @Test
    public void testClientProxyProtocolV2WithVectors() throws Exception
    {
        int typeTLS = 0x20;
        String tlsVersion = "TLSv1.3";
        byte[] tlsVersionBytes = tlsVersion.getBytes(StandardCharsets.US_ASCII);
        startServer(new Handler.Abstract()
        {
            @Override
            public boolean handle(Request request, Response response, Callback callback)
            {
                EndPoint endPoint = request.getConnectionMetaData().getConnection().getEndPoint();
                assertTrue(endPoint instanceof ProxyConnectionFactory.ProxyEndPoint);
                ProxyConnectionFactory.ProxyEndPoint proxyEndPoint = (ProxyConnectionFactory.ProxyEndPoint)endPoint;
                if (Request.getPathInContext(request).equals("/tls_version"))
                {
                    assertNotNull(proxyEndPoint.getTLV(typeTLS));
                    assertEquals(tlsVersion, proxyEndPoint.getAttribute(ProxyConnectionFactory.TLS_VERSION));
                }
                response.getHeaders().put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_PLAIN.asString());
                Content.Sink.write(response, true, String.valueOf(Request.getRemotePort(request)), callback);
                return true;
            }
        });
        startClient();

        int serverPort = connector.getLocalPort();

        int clientPort = ThreadLocalRandom.current().nextInt(1024, 65536);
        byte[] dataTLS = new byte[1 + 4 + (1 + 2 + tlsVersionBytes.length)];
        dataTLS[0] = 0x01; // CLIENT_SSL
        dataTLS[5] = 0x21; // SUBTYPE_SSL_VERSION
        dataTLS[6] = 0x00; // Length, hi byte
        dataTLS[7] = (byte)tlsVersionBytes.length; // Length, lo byte
        System.arraycopy(tlsVersionBytes, 0, dataTLS, 8, tlsVersionBytes.length);
        V2.Tag.TLV tlv = new V2.Tag.TLV(typeTLS, dataTLS);
        V2.Tag tag = new V2.Tag("127.0.0.1", clientPort, Collections.singletonList(tlv));

        ContentResponse response = client.newRequest("localhost", serverPort)
            .path("/tls_version")
            .tag(tag)
            .send();
        assertEquals(HttpStatus.OK_200, response.getStatus());
        assertEquals(String.valueOf(clientPort), response.getContentAsString());

        // Make another request with the same address information, but different TLV.
        V2.Tag.TLV tlv2 = new V2.Tag.TLV(0x01, "http/1.1".getBytes(StandardCharsets.UTF_8));
        V2.Tag tag2 = new V2.Tag("127.0.0.1", clientPort, Collections.singletonList(tlv2));
        response = client.newRequest("localhost", serverPort)
            .tag(tag2)
            .send();
        assertEquals(HttpStatus.OK_200, response.getStatus());
        assertEquals(String.valueOf(clientPort), response.getContentAsString());

        // Make sure the two TLVs created two destinations.
        assertEquals(2, client.getDestinations().size());
    }

    @Test
    public void testProxyProtocolWrappingHTTPProxy() throws Exception
    {
        startServer(new Handler.Abstract()
        {
            @Override
            public boolean handle(Request request, Response response, Callback callback)
            {
                response.getHeaders().put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_PLAIN.asString());
                Content.Sink.write(response, true, String.valueOf(Request.getRemotePort(request)), callback);
                return true;
            }
        });
        startClient();

        int proxyPort = connector.getLocalPort();
        int serverPort = proxyPort + 1; // Any port will do.
        client.getProxyConfiguration().addProxy(new HttpProxy("localhost", proxyPort));

        // We are simulating to be a HttpClient inside a proxy.
        // The server is configured with the PROXY protocol to know the socket address of clients.

        // The proxy receives a request from the client, and it extracts the client address.
        int clientPort = ThreadLocalRandom.current().nextInt(1024, 65536);
        V1.Tag tag = new V1.Tag("127.0.0.1", clientPort);

        // The proxy maps the client address, then sends the request.
        ContentResponse response = client.newRequest("localhost", serverPort)
            .tag(tag)
            .headers(headers -> headers.put(HttpHeader.CONNECTION, HttpHeaderValue.CLOSE))
            .send();

        assertEquals(HttpStatus.OK_200, response.getStatus());
        assertEquals(String.valueOf(clientPort), response.getContentAsString());
        List<Destination> destinations = client.getDestinations();
        assertEquals(1, destinations.size());
        HttpDestination destination = (HttpDestination)destinations.get(0);
        assertTrue(destination.getConnectionPool().isEmpty());

        // The previous connection has been closed.
        // Make another request from the same client address.
        response = client.newRequest("localhost", serverPort)
            .tag(tag)
            .send();
        assertEquals(HttpStatus.OK_200, response.getStatus());
        assertEquals(String.valueOf(clientPort), response.getContentAsString());
        destinations = client.getDestinations();
        assertEquals(1, destinations.size());
        assertSame(destination, destinations.get(0));

        // Make another request from a different client address.
        int clientPort2 = clientPort + 1;
        V1.Tag tag2 = new V1.Tag("127.0.0.1", clientPort2);
        response = client.newRequest("localhost", serverPort)
            .tag(tag2)
            .send();
        assertEquals(HttpStatus.OK_200, response.getStatus());
        assertEquals(String.valueOf(clientPort2), response.getContentAsString());
        destinations = client.getDestinations();
        assertEquals(2, destinations.size());
    }
}
