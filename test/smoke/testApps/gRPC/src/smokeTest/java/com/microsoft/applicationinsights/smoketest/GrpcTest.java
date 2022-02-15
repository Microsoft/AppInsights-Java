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

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import org.junit.Test;

@UseAgent
public class GrpcTest extends AiSmokeTest {

  @Test
  @TargetUri("/simple")
  public void doSimpleTest() throws Exception {
    List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 2);

    Envelope rdEnvelope1 = getRequestEnvelope(rdList, "GET /simple");
    Envelope rdEnvelope2 = getRequestEnvelope(rdList, "example.Greeter/SayHello");
    String operationId = rdEnvelope1.getTags().get("ai.operation.id");

    List<Envelope> rddList =
        mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
    // auto-collected grpc events are suppressed by exporter because they are noisy
    assertEquals(0, mockedIngestion.getCountForType("MessageData", operationId));

    Envelope rddEnvelope = getDependencyEnvelope(rddList, "example.Greeter/SayHello");

    RequestData rd1 = (RequestData) ((Data<?>) rdEnvelope1.getData()).getBaseData();
    RemoteDependencyData rdd =
        (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

    assertEquals("localhost:10203", rdd.getTarget());

    assertTrue(rd1.getProperties().isEmpty());
    assertTrue(rd1.getSuccess());

    assertTrue(rdd.getProperties().isEmpty());
    assertTrue(rdd.getSuccess());

    // TODO (trask): verify rd2

    assertParentChild(rd1, rdEnvelope1, rddEnvelope, "GET /simple");
    assertParentChild(
        rdd.getId(), rddEnvelope, rdEnvelope2, "GET /simple", "example.Greeter/SayHello", false);
  }

  @Test
  @TargetUri("/conversation")
  public void doConversationTest() throws Exception {
    List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 2);

    Envelope rdEnvelope1 = getRequestEnvelope(rdList, "GET /conversation");
    Envelope rdEnvelope2 = getRequestEnvelope(rdList, "example.Greeter/Conversation");
    String operationId = rdEnvelope1.getTags().get("ai.operation.id");

    List<Envelope> rddList =
        mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
    // auto-collected grpc events are suppressed by exporter because they are noisy
    assertEquals(0, mockedIngestion.getCountForType("MessageData", operationId));

    Envelope rddEnvelope = getDependencyEnvelope(rddList, "example.Greeter/Conversation");

    RequestData rd1 = (RequestData) ((Data<?>) rdEnvelope1.getData()).getBaseData();
    RemoteDependencyData rdd =
        (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

    assertEquals("localhost:10203", rdd.getTarget());

    assertTrue(rd1.getProperties().isEmpty());
    assertTrue(rd1.getSuccess());

    assertTrue(rdd.getProperties().isEmpty());
    assertTrue(rdd.getSuccess());

    // TODO (trask): verify rd2

    assertParentChild(rd1, rdEnvelope1, rddEnvelope, "GET /conversation");
    assertParentChild(
        rdd.getId(),
        rddEnvelope,
        rdEnvelope2,
        "GET /conversation",
        "example.Greeter/Conversation",
        false);
  }

  private static Envelope getRequestEnvelope(List<Envelope> envelopes, String name) {
    for (Envelope envelope : envelopes) {
      RequestData rd = (RequestData) ((Data<?>) envelope.getData()).getBaseData();
      if (rd.getName().equals(name)) {
        return envelope;
      }
    }
    throw new IllegalStateException("Could not find request with name: " + name);
  }

  private static Envelope getDependencyEnvelope(List<Envelope> envelopes, String name) {
    for (Envelope envelope : envelopes) {
      RemoteDependencyData rdd =
          (RemoteDependencyData) ((Data<?>) envelope.getData()).getBaseData();
      if (rdd.getName().equals(name)) {
        return envelope;
      }
    }
    throw new IllegalStateException("Could not find dependency with name: " + name);
  }
}
