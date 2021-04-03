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

package com.microsoft.applicationinsights.agent.internal;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import org.junit.*;

import static org.junit.Assert.*;

public class LazyConfigurationAccessorTest {

    /*
     * Lazily Set Connection String For Linux Consumption Plan:
     *
     *    Term      LazySetOptIn   ConnectionString      EnableAgent        LazySet
     *    Preview   FALSE          VALID                 TRUE               Enabled
     *                             VALID                 FALSE              Disabled
     *                             VALID                 NULL               Disabled
     *                             NULL                  TRUE/FALSE/NULL    Disabled
     *    GA        TRUE           VALID                 TRUE               Enabled
     *                             VALID                 FALSE              Disabled
     *                             VALID                 NULL               Enabled
     *                             NULL                  TRUE/FALSE/NULL    Disabled
     */
    private static final String CONNECTION_STRING = "InstrumentationKey=00000000-0000-0000-0000-0FEEDDADBEEF";

    private static final String INSTRUMENTATION_KEY = "00000000-0000-0000-0000-0FEEDDADBEEF";
    private static final String WEBSITE_SITE_NAME = "fake_site_name";

    @Test
    //"LazySetOptIn is FALSE, ConnectionString is valid and EnableAgent is TRUE"
    public void enableLazySetWithLazySetOptInOffEnableAgentOn() {
        assertTrue(LazyConfigurationAccessor.shouldSetConnectionString(false, "true"));
    }

    @Test
    //"LazySetOptIn is FALSE, ConnectionString is valid and EnableAgent is FALSE"
    public void disableLazySetWithLazySetOptInOffEnableAgentOff() {
        assertFalse(LazyConfigurationAccessor.shouldSetConnectionString(false, "false"));
    }

    @Test
    //"LazySetOptIn is FALSE, ConnectionString is valid and EnableAgent is NULL"
    public void disableLazySetWithLazySetOptInOffEnableAgentNull() {
        assertFalse(LazyConfigurationAccessor.shouldSetConnectionString(false, null));
    }

    @Test
    //"LazySetOptIn is FALSE, ConnectionString is NULL, InstrumentationKey is NULL, and EnableAgent is TRUE"
    public void disableLazySetWithLazySetOptInOffConnectionStringNullInstrumentationKeyNull() {
        String oldConnectionString = TelemetryConfiguration.getActive().getConnectionString();
        assertTrue(LazyConfigurationAccessor.shouldSetConnectionString(false, "true"));
        LazyConfigurationAccessor.setConnectionString(null, null);
        assertEquals(TelemetryConfiguration.getActive().getConnectionString(), oldConnectionString);
    }

    @Test
    //"LazySetOptIn is FALSE, ConnectionString is valid, InstrumentationKey is NULL, and EnableAgent is TRUE"
    public void disableLazySetWithLazySetOptInOffConnectionStringNotNullInstrumentationKeyNull() {
        assertTrue(LazyConfigurationAccessor.shouldSetConnectionString(false, "true"));
        LazyConfigurationAccessor.setConnectionString(CONNECTION_STRING, null);
        assertEquals(TelemetryConfiguration.getActive().getConnectionString(), CONNECTION_STRING);

        LazyConfigurationAccessor.setWebsiteSiteName(WEBSITE_SITE_NAME);
        assertEquals(TelemetryConfiguration.getActive().getRoleName(), WEBSITE_SITE_NAME);
    }

    @Test
    //"LazySetOptIn is FALSE, ConnectionString is NULL, InstrumentationKey is valid, and EnableAgent is TRUE")
    public void enableLazySetWithLazySetOptInOffConnectionStringNullInstrumentationKeyNotNull() {
        assertTrue(LazyConfigurationAccessor.shouldSetConnectionString(false, "true"));
        LazyConfigurationAccessor.setConnectionString(null, INSTRUMENTATION_KEY);
        assertEquals(TelemetryConfiguration.getActive().getConnectionString(), "InstrumentationKey=" + INSTRUMENTATION_KEY);
    }

    @Test
    //"LazySetOptIn is TRUE, ConnectionString is valid and EnableAgent is TRUE"
    public void enableLazySetWithLazySetOptInOnEnableAgentOn() {
        assertTrue(LazyConfigurationAccessor.shouldSetConnectionString(true, "true"));
    }

    @Test
    //"LazySetOptIn is TRUE, ConnectionString is valid and EnableAgent is FALSE"
    public void disableLazySetWithLazySetOptInOnEnableAgentOff() {
        assertFalse(LazyConfigurationAccessor.shouldSetConnectionString(true, "false"));
    }

    @Test
    //"LazySetOptIn is TRUE, ConnectionString is valid and EnableAgent is NULL"
    public void enableLazySetWithLazySetOptInOnEnableAgentNull() {
        assertTrue(LazyConfigurationAccessor.shouldSetConnectionString(true, null));
    }

    @Test
    //"LazySetOptIn is TRUE, ConnectionString is NULL, InstrumentationKey is NULL, and EnableAgent is TRUE"
    public void disableLazySetWithLazySetOptInOnConnectionStringNullAndInstrumentationKeyNull() {
        String oldConnectionString = TelemetryConfiguration.getActive().getConnectionString();
        assertTrue(LazyConfigurationAccessor.shouldSetConnectionString(true, "true"));
        LazyConfigurationAccessor.setConnectionString(null, null);
        assertEquals(TelemetryConfiguration.getActive().getConnectionString(), oldConnectionString);
    }

    @Test
    //"LazySetOptIn is TRUE, ConnectionString is valid, InstrumentationKey is NULL, and EnableAgent is TRUE"
    public void enableLazySetWithLazySetOptInOnConnectionStringNotNullInstrumentationKeyNull() {
        assertTrue(LazyConfigurationAccessor.shouldSetConnectionString(false, "true"));
        LazyConfigurationAccessor.setConnectionString(CONNECTION_STRING, null);
        assertEquals(TelemetryConfiguration.getActive().getConnectionString(), CONNECTION_STRING);
    }

    @Test
    //"LazySetOptIn is TRUE, ConnectionString is NULL, InstrumentationKey is valid, and EnableAgent is TRUE"
    public void enableLazySetWithLazySetOptInOnConnectionStringNullInstrumentationKeyNotNull() {
        assertTrue(LazyConfigurationAccessor.shouldSetConnectionString(false, "true"));
        LazyConfigurationAccessor.setConnectionString(null, INSTRUMENTATION_KEY);
        assertEquals(TelemetryConfiguration.getActive().getConnectionString(), "InstrumentationKey=" + INSTRUMENTATION_KEY);
    }
}
