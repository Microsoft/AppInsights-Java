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

import org.junit.Test;

import static org.junit.Assert.*;

public final class RemoteDependencyTelemetryTest {

    @Test
    public void testEmptyCtor() {
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry();

        assertNull(telemetry.getName());
        assertTrue(telemetry.getProperties().isEmpty());
    }

    @Test
    public void testCtorWithNameParameter() {
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry("MockName");

        assertEquals("MockName", telemetry.getName());
        assertTrue(telemetry.getProperties().isEmpty());
    }

    @Test
    public void testCtorWithAllParameter() {
        String dependencyName = "DepName";
        String commandName = "Query1";
        Duration duration = new Duration(12345);
        boolean success = false;

        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry(dependencyName, commandName, duration, success);

        assertEquals(dependencyName, telemetry.getName());
        assertEquals(commandName, telemetry.getCommandName());
        assertEquals(duration, telemetry.getDuration());
        assertEquals(success, telemetry.getSuccess());

        assertTrue(telemetry.getProperties().isEmpty());
    }

    @Test
    public void testCommandName() {
        String commandName = "command";
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry();
        telemetry.setCommandName(commandName);

        assertEquals(commandName, telemetry.getCommandName());
    }

    @Test
    public void testDuration() {
        Duration duration = new Duration(1234);
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry();
        telemetry.setDuration(duration);

        assertEquals(duration, telemetry.getDuration());
    }

    @Test
    public void testSuccess() {
        boolean success = true;
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry();
        telemetry.setSuccess(success);

        assertEquals(success, telemetry.getSuccess());
    }

    @Test
    public void testSetName() {
        RemoteDependencyTelemetry telemetry = new RemoteDependencyTelemetry("MockName");

        telemetry.setName("MockName1");
        assertEquals("MockName1", telemetry.getName());
    }
}
