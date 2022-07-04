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

import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.matchers.ExceptionDataMatchers.ExceptionDetailsMatchers.withMessage;
import static com.microsoft.applicationinsights.smoketest.matchers.ExceptionDataMatchers.hasException;
import static com.microsoft.applicationinsights.smoketest.matchers.ExceptionDataMatchers.hasMeasurement;
import static com.microsoft.applicationinsights.smoketest.matchers.ExceptionDataMatchers.hasSeverityLevel;
import static com.microsoft.applicationinsights.smoketest.matchers.RequestDataMatchers.hasDuration;
import static com.microsoft.applicationinsights.smoketest.matchers.RequestDataMatchers.hasName;
import static com.microsoft.applicationinsights.smoketest.matchers.RequestDataMatchers.hasResponseCode;
import static com.microsoft.applicationinsights.smoketest.matchers.RequestDataMatchers.hasSuccess;
import static com.microsoft.applicationinsights.smoketest.matchers.RequestDataMatchers.hasUrl;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.microsoft.applicationinsights.smoketest.matchers.ExceptionDataMatchers;
import com.microsoft.applicationinsights.smoketest.matchers.TraceDataMatchers;
import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.Domain;
import com.microsoft.applicationinsights.smoketest.schemav2.Duration;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.EventData;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionData;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import com.microsoft.applicationinsights.smoketest.schemav2.PageViewData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.schemav2.SeverityLevel;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

@UseAgent
abstract class CoreAndFilterTests extends AiSmokeTest {

  @Test
  @TargetUri("/trackDependency")
  public void trackDependency() throws Exception {
    Telemetry telemetry = getTelemetry(1);

    Duration expectedDuration = new Duration(0, 0, 1, 1, 1);

    assertThat(telemetry.rdd1.getName()).isEqualTo("DependencyTest");
    assertThat(telemetry.rdd1.getData()).isEqualTo("commandName");
    assertThat(telemetry.rdd1.getType()).isNull();
    assertThat(telemetry.rdd1.getTarget()).isNull();
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getDuration()).isEqualTo(expectedDuration);

    AiSmokeTest.assertParentChild(
        telemetry.rd,
        telemetry.rdEnvelope,
        telemetry.rddEnvelope1,
        "GET /CoreAndFilter/trackDependency");
  }

  @Test
  @TargetUri("/trackEvent")
  public void testTrackEvent() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> edList =
        testing.mockedIngestion.waitForItemsInOperation("EventData", 2, operationId);

    Envelope edEnvelope1 = edList.get(0);
    Envelope edEnvelope2 = edList.get(1);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    List<EventData> events = testing.mockedIngestion.getTelemetryDataByTypeInRequest("EventData");
    events.sort(Comparator.comparing(EventData::getName));

    EventData ed1 = events.get(0);
    EventData ed2 = events.get(1);

    assertEquals("EventDataPropertyTest", ed1.getName());
    assertEquals("value", ed1.getProperties().get("key"));
    assertEquals((Double) 1.0, ed1.getMeasurements().get("key"));

    assertEquals("EventDataTest", ed2.getName());

    AiSmokeTest.assertParentChild(rd, rdEnvelope, edEnvelope1, "GET /CoreAndFilter/trackEvent");
    AiSmokeTest.assertParentChild(rd, rdEnvelope, edEnvelope2, "GET /CoreAndFilter/trackEvent");
  }

  @Test
  @TargetUri("/trackException")
  public void testTrackException() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> edList =
        testing.mockedIngestion.waitForItemsInOperation("ExceptionData", 3, operationId);
    assertEquals(0, testing.mockedIngestion.getCountForType("EventData"));

    Envelope edEnvelope1 = edList.get(0);
    Envelope edEnvelope2 = edList.get(1);
    Envelope edEnvelope3 = edList.get(2);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    final String expectedName = "This is track exception.";
    final String expectedProperties = "value";
    final Double expectedMetrice = 1d;

    List<ExceptionData> exceptions =
        testing.mockedIngestion.getTelemetryDataByTypeInRequest("ExceptionData");
    assertThat(exceptions, hasItem(hasException(withMessage(expectedName))));
    assertThat(
        exceptions,
        hasItem(
            allOf(
                hasException(withMessage(expectedName)),
                ExceptionDataMatchers.hasProperty("key", expectedProperties),
                hasMeasurement("key", expectedMetrice))));
    assertThat(
        exceptions,
        hasItem(
            allOf(hasException(withMessage(expectedName)), hasSeverityLevel(SeverityLevel.ERROR))));

    AiSmokeTest.assertParentChild(rd, rdEnvelope, edEnvelope1, "GET /CoreAndFilter/trackException");
    AiSmokeTest.assertParentChild(rd, rdEnvelope, edEnvelope2, "GET /CoreAndFilter/trackException");
    AiSmokeTest.assertParentChild(rd, rdEnvelope, edEnvelope3, "GET /CoreAndFilter/trackException");
  }

  @Test
  @TargetUri("/trackHttpRequest")
  public void testHttpRequest() throws Exception {
    testing.mockedIngestion.waitForItems("RequestData", 5);

    int totalItems = testing.mockedIngestion.getItemCount();
    int expectedItems = 5;
    assertEquals(
        String.format("There were %d extra telemetry items received.", totalItems - expectedItems),
        expectedItems,
        totalItems);

    // TODO get HttpRequest data envelope and verify value
    List<Domain> requests = testing.mockedIngestion.getTelemetryDataByType("RequestData");
    // true
    assertThat(
        requests,
        hasItem(
            allOf(
                hasName("HttpRequestDataTest"),
                hasResponseCode("200"),
                hasDuration(new Duration(4711)),
                hasSuccess(true))));
    assertThat(
        requests,
        hasItem(
            allOf(
                hasName("PingTest"),
                hasResponseCode("200"),
                hasDuration(new Duration(1)),
                hasSuccess(true),
                hasUrl("http://tempuri.org/ping"))));

    // false
    assertThat(
        requests,
        hasItem(
            allOf(
                hasName("FailedHttpRequest"),
                hasResponseCode("404"),
                hasDuration(new Duration(6666)),
                hasSuccess(false))));
    assertThat(
        requests,
        hasItem(
            allOf(
                hasName("FailedHttpRequest2"),
                hasResponseCode("505"),
                hasDuration(new Duration(8888)),
                hasSuccess(false),
                hasUrl("https://www.bingasdasdasdasda.com/"))));
  }

  @Test
  @TargetUri("/trackMetric")
  public void trackMetric() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    List<Envelope> mdList = testing.mockedIngestion.waitForItems("MetricData", 1);

    Envelope rdEnvelope = rdList.get(0);
    Envelope mdEnvelope = mdList.get(0);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    MetricData md = (MetricData) ((Data<?>) mdEnvelope.getData()).getBaseData();

    List<DataPoint> metrics = md.getMetrics();
    assertEquals(1, metrics.size());
    DataPoint dp = metrics.get(0);

    final double expectedValue = 111222333.0;
    double epsilon = Math.ulp(expectedValue);
    assertEquals(expectedValue, dp.getValue(), epsilon);
    assertEquals("TimeToRespond", dp.getName());

    assertNull("getCount was non-null", dp.getCount());
    assertNull("getMin was non-null", dp.getMin());
    assertNull("getMax was non-null", dp.getMax());
    assertNull("getStdDev was non-null", dp.getStdDev());

    AiSmokeTest.assertParentChild(rd, rdEnvelope, mdEnvelope, "GET /CoreAndFilter/trackMetric");
  }

  @Test
  @TargetUri("/trackTrace")
  public void testTrackTrace() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> mdList = testing.mockedIngestion.waitForMessageItemsInRequest(3, operationId);

    Envelope mdEnvelope1 = mdList.get(0);
    Envelope mdEnvelope2 = mdList.get(1);
    Envelope mdEnvelope3 = mdList.get(2);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    List<MessageData> messages = testing.mockedIngestion.getMessageDataInRequest();
    assertThat(messages, hasItem(TraceDataMatchers.hasMessage("This is first trace message.")));

    assertThat(
        messages,
        hasItem(
            allOf(
                TraceDataMatchers.hasMessage("This is second trace message."),
                TraceDataMatchers.hasSeverityLevel(SeverityLevel.ERROR))));

    assertThat(
        messages,
        hasItem(
            allOf(
                TraceDataMatchers.hasMessage("This is third trace message."),
                TraceDataMatchers.hasSeverityLevel(SeverityLevel.INFORMATION),
                TraceDataMatchers.hasProperty("key", "value"))));

    AiSmokeTest.assertParentChild(rd, rdEnvelope, mdEnvelope1, "GET /CoreAndFilter/trackTrace");
    AiSmokeTest.assertParentChild(rd, rdEnvelope, mdEnvelope2, "GET /CoreAndFilter/trackTrace");
    AiSmokeTest.assertParentChild(rd, rdEnvelope, mdEnvelope3, "GET /CoreAndFilter/trackTrace");
  }

  @Test
  @TargetUri("/trackPageView")
  public void testTrackPageView() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    List<Envelope> pvdList = testing.mockedIngestion.waitForItems("PageViewData", 3);
    assertEquals(0, testing.mockedIngestion.getCountForType("EventData"));

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    Envelope pvdEnvelope1 = null;
    Envelope pvdEnvelope2 = null;
    Envelope pvdEnvelope3 = null;

    for (Envelope pvdEnvelope : pvdList) {
      PageViewData pv = (PageViewData) ((Data<?>) pvdEnvelope.getData()).getBaseData();
      if (pv.getName().equals("test-page")) {
        pvdEnvelope1 = pvdEnvelope;
      } else if (pv.getName().equals("test-page-2")) {
        pvdEnvelope2 = pvdEnvelope;
      } else if (pv.getName().equals("test-page-3")) {
        pvdEnvelope3 = pvdEnvelope;
      } else {
        throw new AssertionError("Unexpected page view: " + pv.getName());
      }
    }

    PageViewData pv1 = (PageViewData) ((Data<?>) pvdEnvelope1.getData()).getBaseData();
    PageViewData pv2 = (PageViewData) ((Data<?>) pvdEnvelope2.getData()).getBaseData();
    PageViewData pv3 = (PageViewData) ((Data<?>) pvdEnvelope3.getData()).getBaseData();

    assertNotNull(pv1);
    assertEquals(new Duration(0), pv1.getDuration());
    // checking that instrumentation key, cloud role name, cloud role instance, and sdk version are
    // from the agent
    assertEquals("00000000-0000-0000-0000-0FEEDDADBEEF", pvdEnvelope1.getIKey());
    assertEquals("testrolename", pvdEnvelope1.getTags().get("ai.cloud.role"));
    assertEquals("testroleinstance", pvdEnvelope1.getTags().get("ai.cloud.roleInstance"));
    assertTrue(pvdEnvelope1.getTags().get("ai.internal.sdkVersion").startsWith("java:3."));

    assertNotNull(pv2);
    assertEquals(new Duration(123456), pv2.getDuration());
    assertEquals("2010-10-10T00:00:00Z", pvdEnvelope2.getTime());
    assertEquals("value", pv2.getProperties().get("key"));
    assertEquals("a-value", pv2.getProperties().get("a-prop"));
    assertEquals("another-value", pv2.getProperties().get("another-prop"));
    // operation name is verified below in assertParentChild()
    assertEquals("user-id-goes-here", pvdEnvelope2.getTags().get("ai.user.id"));
    assertEquals("account-id-goes-here", pvdEnvelope2.getTags().get("ai.user.accountId"));
    assertEquals("user-agent-goes-here", pvdEnvelope2.getTags().get("ai.user.userAgent"));
    assertEquals("os-goes-here", pvdEnvelope2.getTags().get("ai.device.os"));
    assertEquals("session-id-goes-here", pvdEnvelope2.getTags().get("ai.session.id"));
    assertEquals("1.2.3.4", pvdEnvelope2.getTags().get("ai.location.ip"));
    // checking that instrumentation key, cloud role name and cloud role instance are overridden
    assertEquals("12341234-1234-1234-1234-123412341234", pvdEnvelope2.getIKey());
    assertEquals("role-goes-here", pvdEnvelope2.getTags().get("ai.cloud.role"));
    assertEquals("role-instance-goes-here", pvdEnvelope2.getTags().get("ai.cloud.roleInstance"));
    // checking that sdk version is from the agent
    assertTrue(pvdEnvelope2.getTags().get("ai.internal.sdkVersion").startsWith("java:3."));

    assertNotNull(pv3);
    assertEquals(new Duration(123456), pv3.getDuration());
    assertEquals("2010-10-10T00:00:00Z", pvdEnvelope3.getTime());
    assertEquals("value", pv3.getProperties().get("key"));
    assertEquals("a-value", pv3.getProperties().get("a-prop"));
    assertEquals("another-value", pv3.getProperties().get("another-prop"));
    // operation name is verified below in assertParentChild()
    assertEquals("user-id-goes-here", pvdEnvelope3.getTags().get("ai.user.id"));
    assertEquals("account-id-goes-here", pvdEnvelope3.getTags().get("ai.user.accountId"));
    assertEquals("user-agent-goes-here", pvdEnvelope3.getTags().get("ai.user.userAgent"));
    assertEquals("os-goes-here", pvdEnvelope3.getTags().get("ai.device.os"));
    assertEquals("session-id-goes-here", pvdEnvelope3.getTags().get("ai.session.id"));
    assertEquals("1.2.3.4", pvdEnvelope3.getTags().get("ai.location.ip"));
    // checking that instrumentation key, cloud role name and cloud role instance are from the agent
    assertEquals("00000000-0000-0000-0000-0FEEDDADBEEF", pvdEnvelope3.getIKey());
    assertEquals("testrolename", pvdEnvelope3.getTags().get("ai.cloud.role"));
    assertEquals("testroleinstance", pvdEnvelope3.getTags().get("ai.cloud.roleInstance"));
    // checking that sdk version is from the agent
    assertTrue(pvdEnvelope3.getTags().get("ai.internal.sdkVersion").startsWith("java:3."));

    AiSmokeTest.assertParentChild(rd, rdEnvelope, pvdEnvelope1, "GET /CoreAndFilter/trackPageView");

    assertEquals("operation-id-goes-here", pvdEnvelope2.getTags().get("ai.operation.id"));
    assertEquals(
        "operation-parent-id-goes-here", pvdEnvelope2.getTags().get("ai.operation.parentId"));
    assertEquals("operation-name-goes-here", pvdEnvelope2.getTags().get("ai.operation.name"));

    assertEquals("operation-id-goes-here", pvdEnvelope3.getTags().get("ai.operation.id"));
    assertEquals(
        "operation-parent-id-goes-here", pvdEnvelope3.getTags().get("ai.operation.parentId"));
    assertEquals("operation-name-goes-here", pvdEnvelope3.getTags().get("ai.operation.name"));
  }

  @Test
  @TargetUri("/doPageView.jsp")
  public void testTrackPageViewJsp() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> pvdList =
        testing.mockedIngestion.waitForItemsInOperation("PageViewData", 1, operationId);
    assertEquals(0, testing.mockedIngestion.getCountForType("EventData"));

    Envelope pvdEnvelope = pvdList.get(0);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    PageViewData pv = (PageViewData) ((Data<?>) pvdEnvelope.getData()).getBaseData();
    assertEquals("doPageView", pv.getName());
    assertEquals(new Duration(0), pv.getDuration());

    AiSmokeTest.assertParentChild(rd, rdEnvelope, pvdEnvelope, "GET /CoreAndFilter/doPageView.jsp");
  }

  @Test
  @TargetUri("/autoFailedRequestWithResultCode")
  public void testAutoFailedRequestWithResultCode() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    assertEquals(false, rd.getSuccess());
    assertEquals("404", rd.getResponseCode());

    assertEquals("GET /CoreAndFilter/*", rdEnvelope.getTags().get("ai.operation.name"));
  }

  @Test
  @TargetUri("/requestSlow?sleeptime=20")
  public void testRequestSlowWithResponseTime() throws Exception {
    validateSlowTest(20, "GET /CoreAndFilter/requestSlow");
  }

  @Test
  @TargetUri("/slowLoop?responseTime=20")
  public void testSlowRequestUsingCpuBoundLoop() throws Exception {
    validateSlowTest(20, "GET /CoreAndFilter/slowLoop");
  }

  @Test
  @TargetUri("/autoExceptionWithFailedRequest")
  public void testAutoExceptionWithFailedRequest() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> edList =
        testing.mockedIngestion.waitForItems(
            new Predicate<Envelope>() {
              @Override
              public boolean test(Envelope input) {
                if (!"ExceptionData".equals(input.getData().getBaseType())) {
                  return false;
                }
                if (!operationId.equals(input.getTags().get("ai.operation.id"))) {
                  return false;
                }
                // lastly, filter out ExceptionData captured from tomcat logger
                ExceptionData data = (ExceptionData) ((Data<?>) input.getData()).getBaseData();
                return !data.getProperties().containsKey("LoggerName");
              }
            },
            1,
            10,
            TimeUnit.SECONDS);

    Envelope edEnvelope = edList.get(0);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    ExceptionData ed = (ExceptionData) ((Data<?>) edEnvelope.getData()).getBaseData();

    assertFalse(rd.getSuccess());

    ExceptionDetails details = getExceptionDetails(ed);
    assertEquals("This is a auto thrown exception !", details.getMessage());
  }

  @Test
  @TargetUri("/index.jsp")
  public void testRequestJsp() throws Exception {
    testing.mockedIngestion.waitForItems("RequestData", 1);
  }

  private static ExceptionDetails getExceptionDetails(ExceptionData exceptionData) {
    List<ExceptionDetails> details = exceptionData.getExceptions();
    return details.get(0);
  }

  private void validateSlowTest(int expectedDurationSeconds, String operationName)
      throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    long actual = rd.getDuration().getTotalMilliseconds();
    long expected = (new Duration(0, 0, 0, expectedDurationSeconds, 0).getTotalMilliseconds());
    long tolerance = 2 * 1000; // 2 seconds

    long min = expected - tolerance;
    long max = expected + tolerance;

    System.out.printf("Slow response time: expected=%d, actual=%d%n", expected, actual);
    assertThat(actual, both(greaterThanOrEqualTo(min)).and(lessThan(max)));

    assertEquals(operationName, rdEnvelope.getTags().get("ai.operation.name"));
  }

  // old Application Insights version that do not support Java 11+

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends CoreAndFilterTests {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends CoreAndFilterTests {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends CoreAndFilterTests {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends CoreAndFilterTests {}
}
