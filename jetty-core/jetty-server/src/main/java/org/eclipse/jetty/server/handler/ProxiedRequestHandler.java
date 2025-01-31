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

package org.eclipse.jetty.server.handler;

import java.net.SocketAddress;

import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.ConnectionMetaData;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.HostPort;

public class ProxiedRequestHandler extends Handler.Wrapper
{
    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception
    {
        ConnectionMetaData proxiedFor = new ConnectionMetaData.Wrapper(request.getConnectionMetaData())
        {
            @Override
            public boolean isSecure()
            {
                // TODO replace with value determined from headers
                return super.isSecure();
            }

            @Override
            public SocketAddress getRemoteSocketAddress()
            {
                // TODO replace with value determined from headers
                return super.getRemoteSocketAddress();
            }

            @Override
            public SocketAddress getLocalSocketAddress()
            {
                // TODO replace with value determined from headers
                return super.getLocalSocketAddress();
            }

            @Override
            public HostPort getServerAuthority()
            {
                // TODO replace with value determined from headers
                return super.getServerAuthority();
            }
        };

        return super.handle(new Request.Wrapper(request)
        {
            @Override
            public HttpURI getHttpURI()
            {
                // TODO replace with any change in authority
                return super.getHttpURI();
            }

            @Override
            public ConnectionMetaData getConnectionMetaData()
            {
                return proxiedFor;
            }
        }, response, callback);
    }
}
