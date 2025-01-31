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

package org.eclipse.jetty.http2.parser;

import java.nio.ByteBuffer;

import org.eclipse.jetty.http2.frames.PriorityFrame;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.RetainableByteBuffer;
import org.eclipse.jetty.util.BufferUtil;

public class HeaderBlockFragments
{
    private final ByteBufferPool bufferPool;
    private PriorityFrame priorityFrame;
    private boolean endStream;
    private int streamId;
    private RetainableByteBuffer storage;

    public HeaderBlockFragments(ByteBufferPool bufferPool)
    {
        this.bufferPool = bufferPool;
    }

    public void storeFragment(ByteBuffer fragment, int length, boolean last)
    {
        if (storage == null)
        {
            int space = last ? length : length * 2;
            storage = bufferPool.acquire(space, fragment.isDirect());
            BufferUtil.flipToFill(storage.getByteBuffer());
        }

        // Grow the storage if necessary.
        if (storage.remaining() < length)
        {
            ByteBuffer byteBuffer = storage.getByteBuffer();
            int space = last ? length : length * 2;
            int capacity = byteBuffer.position() + space;
            RetainableByteBuffer newStorage = bufferPool.acquire(capacity, storage.isDirect());
            BufferUtil.flipToFill(newStorage.getByteBuffer());
            byteBuffer.flip();
            newStorage.getByteBuffer().put(byteBuffer);
            storage.release();
            storage = newStorage;
        }

        // Copy the fragment into the storage.
        int limit = fragment.limit();
        fragment.limit(fragment.position() + length);
        storage.getByteBuffer().put(fragment);
        fragment.limit(limit);
    }

    public PriorityFrame getPriorityFrame()
    {
        return priorityFrame;
    }

    public void setPriorityFrame(PriorityFrame priorityFrame)
    {
        this.priorityFrame = priorityFrame;
    }

    public boolean isEndStream()
    {
        return endStream;
    }

    public void setEndStream(boolean endStream)
    {
        this.endStream = endStream;
    }

    public RetainableByteBuffer complete()
    {
        RetainableByteBuffer result = storage;
        storage = null;
        result.getByteBuffer().flip();
        return result;
    }

    public int getStreamId()
    {
        return streamId;
    }

    public void setStreamId(int streamId)
    {
        this.streamId = streamId;
    }
}
