// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

abstract class RuntimeAttachWithDelayedConnectionStringTest {

  @RegisterExtension
  static final SmokeTestExtension testing =
      SmokeTestExtension.builder().doNotSetConnectionString().build();

  @Test
  @TargetUri("/test")
  void doMostBasicTest() throws Exception {
    Telemetry telemetry = testing.getTelemetry(0);

    assertThat(telemetry.rd.getName()).isEqualTo("GET /test");
    assertThat(telemetry.rd.getUrl()).matches("http://localhost:[0-9]+/test");
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("200");
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(telemetry.rd.getMeasurements()).isEmpty();

    assertThat(telemetry.rdEnvelope.getTags())
        .hasEntrySatisfying("ai.internal.sdkVersion", v -> assertThat(v).startsWith("ra_java:3."));
  }

  @Environment(JAVA_8)
  static class Java8Test extends RuntimeAttachWithDelayedConnectionStringTest {}

  @Environment(JAVA_8_OPENJ9)
  static class Java8OpenJ9Test extends RuntimeAttachWithDelayedConnectionStringTest {}

  @Environment(JAVA_11)
  static class Java11Test extends RuntimeAttachWithDelayedConnectionStringTest {}

  @Environment(JAVA_11_OPENJ9)
  static class Java11OpenJ9Test extends RuntimeAttachWithDelayedConnectionStringTest {}

  @Environment(JAVA_17)
  static class Java17Test extends RuntimeAttachWithDelayedConnectionStringTest {}

  @Environment(JAVA_17_OPENJ9)
  static class Java17OpenJ9Test extends RuntimeAttachWithDelayedConnectionStringTest {}

  @Environment(JAVA_21)
  static class JavaPrereleaseTest extends RuntimeAttachWithDelayedConnectionStringTest {}
}
