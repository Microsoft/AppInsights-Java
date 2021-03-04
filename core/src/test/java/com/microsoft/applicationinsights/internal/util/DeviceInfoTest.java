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

package com.microsoft.applicationinsights.internal.util;

import org.junit.Test;

import java.io.IOException;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

/**
 * Created by amnonsh on 2/10/2015.
 */
public class DeviceInfoTest {

    @Test
    public void testSimpleLocale() {
        Locale.setDefault(new Locale("en", "us"));
        String tag = DeviceInfo.getLocale();

        assertEquals("en-US", tag);
    }

    @Test
    public void testSpecialLocale() {
        Locale.setDefault(new Locale("iw", "il"));
        String tag = DeviceInfo.getLocale();

        assertEquals("he-IL", tag);
    }

    @Test
    public void testBadLocale() {
        Locale.setDefault(new Locale("BadLocale"));
        String tag = DeviceInfo.getLocale();

        assertEquals(isJava6() ? "badlocale" : "und", tag);
    }

    private boolean isJava6()
    {
        try {
            Locale.class.getMethod("toLanguageTag");
        } catch (NoSuchMethodException e) {
            return true;
        }
        return false;
    }
}
