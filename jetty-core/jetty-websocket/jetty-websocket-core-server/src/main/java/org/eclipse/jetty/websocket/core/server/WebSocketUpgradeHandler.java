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

package org.eclipse.jetty.websocket.core.server;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.websocket.core.Configuration;
import org.eclipse.jetty.websocket.core.WebSocketComponents;

public class WebSocketUpgradeHandler extends Handler.Wrapper
{
    private final WebSocketMappings mappings;
    private final Configuration.ConfigurationCustomizer customizer = new Configuration.ConfigurationCustomizer();

    public WebSocketUpgradeHandler()
    {
        this(new WebSocketComponents());
    }

    public WebSocketUpgradeHandler(WebSocketComponents components)
    {
        this.mappings = new WebSocketMappings(components);
        setHandler(new Handler.Abstract.NonBlocking()
        {
            @Override
            public boolean handle(Request request, Response response, Callback callback)
            {
                Response.writeError(request, response, callback, HttpStatus.NOT_FOUND_404);
                return true;
            }
        });
    }

    public void addMapping(String pathSpec, WebSocketNegotiator negotiator)
    {
        mappings.addMapping(WebSocketMappings.parsePathSpec(pathSpec), negotiator);
    }

    public void addMapping(PathSpec pathSpec, WebSocketNegotiator negotiator)
    {
        mappings.addMapping(pathSpec, negotiator);
    }

    public Configuration getConfiguration()
    {
        return customizer;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception
    {
        try
        {
            if (mappings.upgrade(request, response, callback, customizer))
                return true;
            return super.handle(request, response, callback);
        }
        catch (Throwable x)
        {
            Response.writeError(request, response, callback, x);
            return true;
        }
    }
}
