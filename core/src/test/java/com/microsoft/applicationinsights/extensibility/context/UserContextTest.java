/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.extensibility.context;

import org.junit.Test;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class UserContextTest {
    @Test
    public void testSetId() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        UserContext context = new UserContext(map);
        context.setId("mock");

        assertEquals("mock", context.getId());
        assertEquals(1, map.size());
        assertEquals("mock", map.get(ContextTagKeys.getKeys().getUserId()));
    }

    @Test
    public void testSetAccountId() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        UserContext context = new UserContext(map);
        context.setAccountId("mock");

        assertEquals("mock", context.getAccountId());
        assertEquals(1, map.size());
        assertEquals("mock", map.get(ContextTagKeys.getKeys().getUserAccountId()));
    }

    @Test
    public void testSetAcquisitionDate() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        UserContext context = new UserContext(map);
        Date date = new Date();
        context.setAcquisitionDate(date);

        assertEquals(context.getAcquisitionDate(), date);
        assertEquals(1, map.size());
    }

    @Test
    public void testSetAcquisitionNullDate() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        UserContext context = new UserContext(map);
        context.setAcquisitionDate(null);

        assertNull(context.getAcquisitionDate());
        assertEquals(0, map.size());
        assertNull(map.get(ContextTagKeys.getKeys().getUserAccountAcquisitionDate()));
    }

    @Test
    public void testSetUserAgent() {
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();
        UserContext context = new UserContext(map);
        context.setUserAgent("mock");

        assertEquals("mock", context.getUserAgent());
        assertEquals(1, map.size());
        assertEquals("mock", map.get(ContextTagKeys.getKeys().getUserAgent()));
    }
}