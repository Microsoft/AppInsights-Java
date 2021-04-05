package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.*;

import static org.junit.Assert.*;

@UseAgent("disabled_springscheduling")
public class SpringSchedulingDisabledTest extends AiSmokeTest {

    @Test
    @TargetUri("/scheduler")
    public void fixedRateSchedulerTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 1);
        Envelope rdEnvelope = rdList.get(0);
        RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

        assertEquals("GET /SpringScheduling/scheduler", rd.getName());
        assertEquals("200", rd.getResponseCode());
        assertTrue(rd.getProperties().isEmpty());
        assertTrue(rd.getSuccess());

        // sleep a bit and make sure no spring scheduling "requests" are reported
        Thread.sleep(5000);
        assertEquals(1, mockedIngestion.getCountForType("RequestData"));
    }
}
