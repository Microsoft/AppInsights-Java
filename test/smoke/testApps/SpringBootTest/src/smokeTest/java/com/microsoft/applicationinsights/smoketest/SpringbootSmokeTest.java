package com.microsoft.applicationinsights.smoketest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Predicate;
import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.EventData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionData;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.AiSmokeTest;
import com.microsoft.applicationinsights.smoketest.TargetUri;
import com.microsoft.applicationinsights.smoketest.UseAgent;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.*;

@UseAgent
public class SpringbootSmokeTest extends AiSmokeTest {

    @Test
    @TargetUri("/basic/trackEvent")
    public void trackEvent() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");

        mockedIngestion.waitForItemsInOperation("EventData", 2, operationId);

        // TODO get event data envelope and verify value
        final List<EventData> data = mockedIngestion.getTelemetryDataByTypeInRequest("EventData");
        assertThat(
                data,
                hasItem(
                        new TypeSafeMatcher<EventData>() {
                            final String name = "EventDataTest";
                            final Matcher<String> nameMatcher = Matchers.equalTo(name);

                            @Override
                            protected boolean matchesSafely(EventData item) {
                                return nameMatcher.matches(item.getName());
                            }

                            @Override
                            public void describeTo(Description description) {
                                description.appendDescriptionOf(nameMatcher);
                            }
                        }));

        assertThat(
                data,
                hasItem(
                        new TypeSafeMatcher<EventData>() {
                            final String expectedKey = "key";
                            final String expectedName = "EventDataPropertyTest";
                            final String expectedPropertyValue = "value";
                            final Double expectedMetricValue = 1d;
                            final Matcher<Map<? extends String, ? extends Double>> metricMatcher =
                                    Matchers.hasEntry(expectedKey, expectedMetricValue);
                            final Matcher<Map<? extends String, ? extends String>> propertyMatcher =
                                    Matchers.hasEntry(expectedKey, expectedPropertyValue);
                            final Matcher<String> nameMatcher = Matchers.equalTo(expectedName);

                            @Override
                            public void describeTo(Description description) {
                                description.appendDescriptionOf(nameMatcher);
                                description.appendDescriptionOf(propertyMatcher);
                                description.appendDescriptionOf(metricMatcher);
                            }

                            @Override
                            protected boolean matchesSafely(EventData item) {
                                return nameMatcher.matches(item.getName())
                                        && propertyMatcher.matches(item.getProperties())
                                        && metricMatcher.matches(item.getMeasurements());
                            }
                        }));
    }

    @Test
    @TargetUri("/throwsException")
    public void testResultCodeWhenRestControllerThrows() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        List<Envelope> rddList = mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
        List<Envelope> edList = mockedIngestion.waitForItems(new Predicate<Envelope>() {
            @Override
            public boolean apply(Envelope input) {
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
        }, 2, 10, TimeUnit.SECONDS);
        assertEquals(0, mockedIngestion.getCountForType("EventData"));

        Envelope rddEnvelope = rddList.get(0);
        Envelope edEnvelope1 = edList.get(0);

        RequestData rd = getTelemetryDataForType(0, "RequestData");
        RemoteDependencyData rdd = (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();
        System.out.println("Response code after exception: " + rd.getResponseCode());
        int code = -123;
        try {
            code = Integer.parseInt(rd.getResponseCode());
        } catch (NumberFormatException e) {
            fail("Response code is not a number");
        }
        assertThat(code, greaterThanOrEqualTo(500));

        assertParentChild(rdd.getId(), rdEnvelope, edEnvelope1);
    }

    @Test
    @TargetUri("/asyncDependencyCall")
    public void testAsyncDependencyCall() throws Exception {
        commonValidation();
    }

    private static void commonValidation() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);

        Envelope rdEnvelope = rdList.get(0);
        String operationId = rdEnvelope.getTags().get("ai.operation.id");
        List<Envelope> rddList = mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 3, operationId);
        assertEquals(0, mockedIngestion.getCountForType("EventData"));

        Envelope rddEnvelope1 = rddList.get(0);
        Envelope rddEnvelope2 = rddList.get(1);
        Envelope rddEnvelope3 = rddList.get(2);

        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
        RemoteDependencyData rdd1 =
                (RemoteDependencyData) ((Data<?>) rddEnvelope1.getData()).getBaseData();
        RemoteDependencyData rdd2 =
                (RemoteDependencyData) ((Data<?>) rddEnvelope2.getData()).getBaseData();
        RemoteDependencyData rdd3 =
                (RemoteDependencyData) ((Data<?>) rddEnvelope3.getData()).getBaseData();

        assertEquals("GET /SpringBootTest/asyncDependencyCall", rd.getName());
        assertEquals("200", rd.getResponseCode());
        assertTrue(rd.getProperties().isEmpty());
        assertTrue(rd.getSuccess());

        assertEquals("TestController.asyncDependencyCall", rdd1.getName());
        assertTrue(rdd1.getProperties().isEmpty());
        assertTrue(rdd1.getSuccess());

        assertEquals("HTTP GET", rdd2.getName());
        assertEquals("https://www.bing.com", rdd2.getData());
        assertEquals("www.bing.com", rdd2.getTarget());
        assertTrue(rdd2.getProperties().isEmpty());
        assertTrue(rdd2.getSuccess());

        assertEquals("TestController.asyncDependencyCall", rdd3.getName());
        assertTrue(rdd3.getProperties().isEmpty());
        assertTrue(rdd3.getSuccess());

        assertParentChild(rdd1.getId(), rdEnvelope, rddEnvelope2);
        assertParentChild(rdd1.getId(), rdEnvelope, rddEnvelope3);
    }

    private static void assertParentChild(
            String parentId, Envelope parentEnvelope, Envelope childEnvelope) {
        String operationId = parentEnvelope.getTags().get("ai.operation.id");

        assertNotNull(operationId);

        assertEquals(operationId, childEnvelope.getTags().get("ai.operation.id"));
        assertEquals(parentId, childEnvelope.getTags().get("ai.operation.parentId"));
    }
}
