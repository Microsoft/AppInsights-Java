// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_20;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_20_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class GrpcTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/simple")
  void doSimpleTest() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 2);
    List<Envelope> rpcClientDurationMetrics =
        testing.mockedIngestion.waitForMetricItems("rpc.client.duration", 1);
    List<Envelope> rpcServerMetrics =
        testing.mockedIngestion.waitForMetricItems("rpc.server.duration", 1);

    Envelope rdEnvelope1 = getRequestEnvelope(rdList, "GET /simple");
    Envelope rdEnvelope2 = getRequestEnvelope(rdList, "example.Greeter/SayHello");
    String operationId = rdEnvelope1.getTags().get("ai.operation.id");

    List<Envelope> rddList =
        testing.mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
    // auto-collected grpc events are suppressed by exporter because they are noisy
    assertThat(testing.mockedIngestion.getCountForType("MessageData", operationId)).isZero();

    Envelope rddEnvelope = getDependencyEnvelope(rddList, "example.Greeter/SayHello");

    assertThat(rdEnvelope1.getSampleRate()).isNull();
    assertThat(rdEnvelope2.getSampleRate()).isNull();
    assertThat(rddEnvelope.getSampleRate()).isNull();

    RequestData rd1 = (RequestData) ((Data<?>) rdEnvelope1.getData()).getBaseData();
    RemoteDependencyData rdd =
        (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

    assertThat(rdd.getTarget()).isEqualTo("localhost:10203");

    assertThat(rd1.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rd1.getSuccess()).isTrue();

    assertThat(rdd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rdd.getSuccess()).isTrue();

    // TODO (trask): verify rd2

    SmokeTestExtension.assertParentChild(rd1, rdEnvelope1, rddEnvelope, "GET /simple");
    SmokeTestExtension.assertParentChild(
        rdd.getId(), rddEnvelope, rdEnvelope2, "GET /simple", "example.Greeter/SayHello", false);

    verifyRpcClientDurationPreAggregatedMetrics(rpcClientDurationMetrics);
    verifyRpcServerDurationPreAggregatedMetrics(rpcServerMetrics);
  }

  @Test
  @TargetUri("/conversation")
  void doConversationTest() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 2);
    List<Envelope> rpcClientDurationMetrics =
        testing.mockedIngestion.waitForMetricItems("rpc.client.duration", 1);
    List<Envelope> rpcServerMetrics =
        testing.mockedIngestion.waitForMetricItems("rpc.server.duration", 1);

    Envelope rdEnvelope1 = getRequestEnvelope(rdList, "GET /conversation");
    Envelope rdEnvelope2 = getRequestEnvelope(rdList, "example.Greeter/Conversation");
    String operationId = rdEnvelope1.getTags().get("ai.operation.id");

    List<Envelope> rddList =
        testing.mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
    // auto-collected grpc events are suppressed by exporter because they are noisy
    assertThat(testing.mockedIngestion.getCountForType("MessageData", operationId)).isZero();

    Envelope rddEnvelope = getDependencyEnvelope(rddList, "example.Greeter/Conversation");

    assertThat(rdEnvelope1.getSampleRate()).isNull();
    assertThat(rdEnvelope2.getSampleRate()).isNull();
    assertThat(rddEnvelope.getSampleRate()).isNull();

    RequestData rd1 = (RequestData) ((Data<?>) rdEnvelope1.getData()).getBaseData();
    RemoteDependencyData rdd =
        (RemoteDependencyData) ((Data<?>) rddEnvelope.getData()).getBaseData();

    assertThat(rdd.getTarget()).isEqualTo("localhost:10203");

    assertThat(rd1.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rd1.getSuccess()).isTrue();

    assertThat(rdd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rdd.getSuccess()).isTrue();

    // TODO (trask): verify rd2

    SmokeTestExtension.assertParentChild(rd1, rdEnvelope1, rddEnvelope, "GET /conversation");
    SmokeTestExtension.assertParentChild(
        rdd.getId(),
        rddEnvelope,
        rdEnvelope2,
        "GET /conversation",
        "example.Greeter/Conversation",
        false);

    verifyRpcClientDurationPreAggregatedMetrics(rpcClientDurationMetrics);
    verifyRpcServerDurationPreAggregatedMetrics(rpcServerMetrics);
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

  private static void verifyRpcClientDurationPreAggregatedMetrics(List<Envelope> metrics) {
    assertThat(metrics.size()).isEqualTo(1);

    Envelope envelope1 = metrics.get(0);
    validateTags(envelope1);
    MetricData md1 = (MetricData) ((Data<?>) envelope1.getData()).getBaseData();
    validateMetricData("client", md1);
  }

  private static void verifyRpcServerDurationPreAggregatedMetrics(List<Envelope> metrics) {
    assertThat(metrics.size()).isEqualTo(1);

    Envelope envelope1 = metrics.get(0);
    validateTags(envelope1);
    MetricData md1 = (MetricData) ((Data<?>) envelope1.getData()).getBaseData();
    validateMetricData("server", md1);
  }

  private static void validateTags(Envelope envelope) {
    Map<String, String> tags = envelope.getTags();
    assertThat(tags.get("ai.internal.sdkVersion")).isNotNull();
    assertThat(tags).containsEntry("ai.cloud.roleInstance", "testroleinstance");
    assertThat(tags).containsEntry("ai.cloud.role", "testrolename");
  }

  private static void validateMetricData(String type, MetricData metricData) {
    List<DataPoint> dataPoints = metricData.getMetrics();
    assertThat(dataPoints).hasSize(1);
    DataPoint dataPoint = dataPoints.get(0);
    assertThat(dataPoint.getCount()).isEqualTo(1);
    assertThat(dataPoint.getValue()).isGreaterThan(0d).isLessThan(60 * 1000.0);
    assertThat(dataPoint.getMin()).isGreaterThan(0d).isLessThan(60 * 1000.0);
    assertThat(dataPoint.getMin()).isGreaterThan(0d).isLessThan(60 * 1000.0);
    Map<String, String> properties = metricData.getProperties();
    if ("client".equals(type)) {
      assertThat(properties).hasSize(8);
      assertThat(properties.get("_MS.MetricId")).isEqualTo("dependencies/duration");
      assertThat(properties.get("dependency/target")).isEqualTo("localhost:10203");
      assertThat(properties.get("Dependency.Type")).isEqualTo("grpc");
    } else {
      assertThat(properties).hasSize(6);
      assertThat(properties.get("_MS.MetricId")).isEqualTo("requests/duration");
      assertThat(properties.get("Request.Success")).isEqualTo("True");
    }
    assertThat(properties.get("operation/synthetic")).isEqualTo("False");
    assertThat(properties.get("cloud/roleInstance")).isEqualTo("testroleinstance");
    assertThat(properties.get("cloud/roleName")).isEqualTo("testrolename");
    assertThat(properties.get("_MS.IsAutocollected")).isEqualTo("True");
  }

  @Environment(JAVA_8)
  static class Java8Test extends GrpcTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends GrpcTest {}

  @Environment(JAVA_11)
  static class Java11Test extends GrpcTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends GrpcTest {}

  @Environment(JAVA_17)
  static class Java17Test extends GrpcTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends GrpcTest {}

  @Environment(JAVA_20)
  static class JavaLatestTest extends GrpcTest {}

  @Environment(JAVA_20_OPENJ9)
  static class JavaLatestOpenJ9Test extends GrpcTest {}

  @Environment(JAVA_21)
  static class JavaPrereleaseTest extends GrpcTest {}
}
