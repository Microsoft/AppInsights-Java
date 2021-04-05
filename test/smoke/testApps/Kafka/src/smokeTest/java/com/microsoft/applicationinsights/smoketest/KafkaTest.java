package com.microsoft.applicationinsights.smoketest;

import java.util.List;

import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import org.junit.Test;

import static org.junit.Assert.*;

@UseAgent
@WithDependencyContainers({
        @DependencyContainer(
                value = "confluentinc/cp-zookeeper",
                portMapping = "2181",
                environmentVariables = {
                        "ZOOKEEPER_CLIENT_PORT=2181"
                },
                hostnameEnvironmentVariable = "ZOOKEEPER"),
        @DependencyContainer(
                value = "confluentinc/cp-kafka",
                portMapping = "9092",
                environmentVariables = {
                        "KAFKA_ZOOKEEPER_CONNECT=${ZOOKEEPER}:2181",
                        "KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://${CONTAINERNAME}:9092",
                        "KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1"
                },
                hostnameEnvironmentVariable = "KAFKA")
})
public class KafkaTest extends AiSmokeTest {

    @Test
    @TargetUri("/sendMessage")
    public void doMostBasicTest() throws Exception {
        List<Envelope> rdList = mockedIngestion.waitForItems("RequestData", 2);

        Envelope rdEnvelope1 = getRequestEnvelope(rdList, "GET /sendMessage");
        String operationId = rdEnvelope1.getTags().get("ai.operation.id");
        List<Envelope> rddList = mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 3, operationId);
        assertEquals(0, mockedIngestion.getCountForType("EventData"));

        Envelope rdEnvelope2 = getRequestEnvelope(rdList, "mytopic process");
        Envelope rddEnvelope1 = getDependencyEnvelope(rddList, "HelloController.sendMessage");
        Envelope rddEnvelope2 = getDependencyEnvelope(rddList, "mytopic send");
        Envelope rddEnvelope3 = getDependencyEnvelope(rddList, "HTTP GET");

        RequestData rd1 = (RequestData) ((Data<?>) rdEnvelope1.getData()).getBaseData();
        RequestData rd2 = (RequestData) ((Data<?>) rdEnvelope2.getData()).getBaseData();

        RemoteDependencyData rdd1 = (RemoteDependencyData) ((Data<?>) rddEnvelope1.getData()).getBaseData();
        RemoteDependencyData rdd2 = (RemoteDependencyData) ((Data<?>) rddEnvelope2.getData()).getBaseData();
        RemoteDependencyData rdd3 = (RemoteDependencyData) ((Data<?>) rddEnvelope3.getData()).getBaseData();

        assertEquals("GET /sendMessage", rd1.getName());
        assertTrue(rd1.getProperties().isEmpty());
        assertTrue(rd1.getSuccess());

        assertEquals("HelloController.sendMessage", rdd1.getName());
        assertTrue(rdd1.getProperties().isEmpty());
        assertTrue(rdd1.getSuccess());

        assertEquals("mytopic send", rdd2.getName());
        assertEquals("Queue Message | kafka", rdd2.getType());
        assertEquals("mytopic", rdd2.getTarget());
        assertTrue(rdd2.getProperties().isEmpty());
        assertTrue(rdd2.getSuccess());

        assertEquals("mytopic process", rd2.getName());
        assertEquals("mytopic", rd2.getSource());
        assertTrue(rd2.getProperties().isEmpty());
        assertTrue(rd2.getSuccess());

        assertEquals("HTTP GET", rdd3.getName());
        assertEquals("https://www.bing.com", rdd3.getData());
        assertTrue(rdd3.getProperties().isEmpty());
        assertTrue(rdd3.getSuccess());

        assertParentChild(rd1.getId(), rdEnvelope1, rddEnvelope1);
        assertParentChild(rdd1.getId(), rddEnvelope1, rddEnvelope2);
        assertParentChild(rdd2.getId(), rddEnvelope2, rdEnvelope2);
        assertParentChild(rd2.getId(), rdEnvelope2, rddEnvelope3);
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
            RemoteDependencyData rdd = (RemoteDependencyData) ((Data<?>) envelope.getData()).getBaseData();
            if (rdd.getName().equals(name)) {
                return envelope;
            }
        }
        throw new IllegalStateException("Could not find dependency with name: " + name);
    }

    private static void assertParentChild(String parentId, Envelope parentEnvelope, Envelope childEnvelope) {
        String operationId = parentEnvelope.getTags().get("ai.operation.id");

        assertNotNull(operationId);

        assertEquals(operationId, childEnvelope.getTags().get("ai.operation.id"));
        assertEquals(parentId, childEnvelope.getTags().get("ai.operation.parentId"));
    }
}
