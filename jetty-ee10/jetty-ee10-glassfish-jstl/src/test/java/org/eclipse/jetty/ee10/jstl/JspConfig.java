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

package org.eclipse.jetty.ee10.jstl;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;

import org.eclipse.jetty.ee10.webapp.WebAppContext;

/**
 * Attempt at collecting up all of the JSP specific configuration bits and pieces into a single place
 * for WebAppContext users to utilize.
 */
public class JspConfig
{
    public static void init(WebAppContext context, URI baseUri, File scratchDir)
    {
        context.setAttribute("jakarta.servlet.context.tempdir", scratchDir);
        context.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
            ".*/jetty-jakarta-servlet-api-[^/]*\\.jar$|.*jakarta.servlet.jsp.jstl-[^/]*\\.jar|.*taglibs-standard.*\\.jar");
        context.setWar(baseUri.toASCIIString());
        context.setBaseResourceAsPath(Path.of(baseUri));
    }
}
