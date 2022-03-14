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

package com.microsoft.applicationinsights.smoketest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import java.util.List;
import org.junit.Test;

@UseAgent("telemetryprocessors")
public class SpringBootAutoTest extends AiSmokeTest {

  @Test
  @TargetUri("/test")
  public void doMostBasicTest() throws Exception {
    Telemetry telemetry = getTelemetry(0);

    assertEquals("testValue1", telemetry.rd.getProperties().get("attribute1"));
    assertEquals("testValue2", telemetry.rd.getProperties().get("attribute2"));
    assertEquals("sensitiveData1", telemetry.rd.getProperties().get("sensitiveAttribute1"));
    assertEquals("*/TelemetryProcessors/test*", telemetry.rd.getProperties().get("httpPath"));
    assertEquals(4, telemetry.rd.getProperties().size());
    assertTrue(telemetry.rd.getSuccess());
    // Log processor test
    List<MessageData> logs = mockedIngestion.getMessageDataInRequest();
    MessageData md1 = logs.get(0);
    assertEquals("testValue1::testValue2", md1.getMessage());
  }

  @Test
  @TargetUri("/sensitivedata")
  public void doSimpleTestPiiData() throws Exception {
    Telemetry telemetry = getTelemetry(0);

    assertEquals("testValue1::testValue2", telemetry.rd.getName());
    assertEquals("testValue1", telemetry.rd.getProperties().get("attribute1"));
    assertEquals("testValue2", telemetry.rd.getProperties().get("attribute2"));
    assertEquals("redacted", telemetry.rd.getProperties().get("sensitiveAttribute1"));
    assertEquals(
        "*/TelemetryProcessors/sensitivedata*", telemetry.rd.getProperties().get("httpPath"));
    assertEquals(4, telemetry.rd.getProperties().size());
    assertTrue(telemetry.rd.getSuccess());
  }
}
