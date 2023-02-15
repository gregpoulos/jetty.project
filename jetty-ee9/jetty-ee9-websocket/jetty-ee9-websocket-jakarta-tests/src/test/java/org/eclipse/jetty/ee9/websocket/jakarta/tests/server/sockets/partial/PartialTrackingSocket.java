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

package org.eclipse.jetty.ee9.websocket.jakarta.tests.server.sockets.partial;

import java.io.IOException;

import jakarta.websocket.OnMessage;
import jakarta.websocket.server.ServerEndpoint;
import org.eclipse.jetty.ee9.websocket.jakarta.tests.server.sockets.TrackingSocket;

@ServerEndpoint("/echo/partial/tracking")
public class PartialTrackingSocket extends TrackingSocket
{
    @OnMessage
    public void onPartial(String msg, boolean fin) throws IOException
    {
        addEvent("onPartial(\"%s\",%b)", msg, fin);
    }
}
