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

package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.util.Sanitizer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class TraceTelemetryTest {
    @Test
    public void testEmptyCtor() {
        TraceTelemetry telemetry = new TraceTelemetry();

        assertEquals("", telemetry.getMessage());
    }

    @Test
    public void testCtor() {
        TraceTelemetry telemetry = new TraceTelemetry("MockMessage");

        assertEquals("MockMessage", telemetry.getMessage());
    }

    @Test
    public void testSetMessage() {
        TraceTelemetry telemetry = new TraceTelemetry("MockMessage");

        telemetry.setMessage("MockMessage1");
        assertEquals("MockMessage1", telemetry.getMessage());
    }

    @Test
    public void testSetSeverityLevel() {
        testSeverityLevel(SeverityLevel.Error);
    }

    @Test
    public void testSetSeverityLevelWithNull() {
        testSeverityLevel(null);
    }

    @Test
    public void testFirstValueIsNull() {
        TraceTelemetry telemetry = new TraceTelemetry("Mock");
        assertNull(telemetry.getSeverityLevel());
    }

    private static void testSeverityLevel(SeverityLevel severityLevel) {
        TraceTelemetry telemetry = new TraceTelemetry("Mock");

        telemetry.setSeverityLevel(severityLevel);
        assertEquals(telemetry.getSeverityLevel(), severityLevel);
    }
}
