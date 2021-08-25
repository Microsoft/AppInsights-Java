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
import static org.junit.Assert.assertNotNull;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionData;
import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.schemav2.SeverityLevel;
import java.util.Comparator;
import java.util.List;
import org.junit.Test;

@UseAgent("logging")
public class TraceLogBackTest extends AiSmokeTest {

  @Test
  @TargetUri("/traceLogBack")
  public void testTraceLogBack() throws Exception {
    List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
    List<Envelope> mdList = mockedIngestion.waitForMessageItemsInRequest(2);

    Envelope rdEnvelope = rdList.get(0);
    Envelope mdEnvelope1 = mdList.get(0);
    Envelope mdEnvelope2 = mdList.get(1);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    List<MessageData> logs = mockedIngestion.getMessageDataInRequest();
    logs.sort(Comparator.comparing(MessageData::getSeverityLevel));

    MessageData md1 = logs.get(0);
    MessageData md2 = logs.get(1);

    assertEquals("This is logback warn.", md1.getMessage());
    assertEquals(SeverityLevel.Warning, md1.getSeverityLevel());
    assertEquals("Logger", md1.getProperties().get("SourceType"));
    assertEquals("WARN", md1.getProperties().get("LoggingLevel"));
    assertEquals("smoketestapp", md1.getProperties().get("LoggerName"));
    assertNotNull(md1.getProperties().get("ThreadName"));
    // TODO add MDC instrumentation for jboss logging
    if (!currentImageName.contains("wildfly")) {
      assertEquals("MDC value", md1.getProperties().get("MDC key"));
      assertEquals(5, md1.getProperties().size());
    } else {
      assertEquals(4, md1.getProperties().size());
    }

    assertEquals("This is logback error.", md2.getMessage());
    assertEquals(SeverityLevel.Error, md2.getSeverityLevel());
    assertEquals("Logger", md2.getProperties().get("SourceType"));
    assertEquals("ERROR", md2.getProperties().get("LoggingLevel"));
    assertEquals("smoketestapp", md2.getProperties().get("LoggerName"));
    assertNotNull(md2.getProperties().get("ThreadName"));
    assertEquals(4, md2.getProperties().size());

    assertParentChild(rd, rdEnvelope, mdEnvelope1, "GET /TraceLogBack/traceLogBack");
    assertParentChild(rd, rdEnvelope, mdEnvelope2, "GET /TraceLogBack/traceLogBack");
  }

  @Test
  @TargetUri("traceLogBackWithException")
  public void testTraceLogBackWithExeption() throws Exception {
    List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> edList =
        mockedIngestion.waitForItemsInOperation("ExceptionData", 1, operationId);
    assertEquals(0, mockedIngestion.getCountForType("EventData"));

    Envelope edEnvelope = edList.get(0);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    ExceptionData ed = (ExceptionData) ((Data<?>) edEnvelope.getData()).getBaseData();

    assertEquals("Fake Exception", ed.getExceptions().get(0).getMessage());
    assertEquals(SeverityLevel.Error, ed.getSeverityLevel());
    assertEquals("This is an exception!", ed.getProperties().get("Logger Message"));
    assertEquals("Logger", ed.getProperties().get("SourceType"));
    assertEquals("ERROR", ed.getProperties().get("LoggingLevel"));
    assertEquals("smoketestapp", ed.getProperties().get("LoggerName"));
    assertNotNull(ed.getProperties().get("ThreadName"));
    // TODO add MDC instrumentation for jboss logging
    if (!currentImageName.contains("wildfly")) {
      assertEquals("MDC value", ed.getProperties().get("MDC key"));
      assertEquals(6, ed.getProperties().size());
    } else {
      assertEquals(5, ed.getProperties().size());
    }

    assertParentChild(rd, rdEnvelope, edEnvelope, "GET /TraceLogBack/traceLogBackWithException");
  }
}
