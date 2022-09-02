// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration.EngineMode;
import com.microsoft.applicationinsights.alerting.config.DefaultConfiguration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class AlertingSubsystemTest {

  private static AlertingSubsystem getAlertMonitor(
      Consumer<AlertBreach> consumer, TestTimeSource timeSource) {
    AlertingSubsystem monitor = AlertingSubsystem.create(consumer, timeSource);

    monitor.updateConfiguration(
        new AlertingConfiguration(
            new AlertConfiguration(AlertMetricType.CPU, true, 80, 30, 14400),
            new AlertConfiguration(AlertMetricType.MEMORY, true, 20, 120, 14400),
            new DefaultConfiguration(true, 5, 120),
            new CollectionPlanConfiguration(
                true, EngineMode.immediate, Instant.now(), 120, "a-settings-moniker")));
    return monitor;
  }

  @Test
  void alertTriggerIsCalled() {

    AtomicReference<AlertBreach> called = new AtomicReference<>();
    Consumer<AlertBreach> consumer = called::set;
    TestTimeSource timeSource = new TestTimeSource();

    AlertingSubsystem service = getAlertMonitor(consumer, timeSource);

    for (int i = 0; i < 10; i++) {
      service.track(AlertMetricType.CPU, 90.0);
    }
    timeSource.increment(50000);
    service.track(AlertMetricType.CPU, 90.0);

    assertThat(called.get().getType()).isEqualTo(AlertMetricType.CPU);
    assertThat(called.get().getAlertValue()).isEqualTo(90.0);
  }

  @Test
  void manualAlertWorks() {
    AtomicReference<AlertBreach> called = new AtomicReference<>();
    Consumer<AlertBreach> consumer = called::set;
    TestTimeSource timeSource = new TestTimeSource();

    AlertingSubsystem service = AlertingSubsystem.create(consumer, timeSource);

    service.updateConfiguration(
        new AlertingConfiguration(
            new AlertConfiguration(AlertMetricType.CPU, true, 80, 30, 14400),
            new AlertConfiguration(AlertMetricType.MEMORY, true, 20, 120, 14400),
            new DefaultConfiguration(true, 5, 120),
            new CollectionPlanConfiguration(
                true,
                EngineMode.immediate,
                Instant.now().plus(100, ChronoUnit.SECONDS),
                120,
                "a-settings-moniker")));

    assertThat(called.get().getType()).isEqualTo(AlertMetricType.MANUAL);
  }

  @Test
  void manualAlertDoesNotTriggerAfterExpired() {
    AtomicReference<AlertBreach> called = new AtomicReference<>();
    Consumer<AlertBreach> consumer = called::set;
    TestTimeSource timeSource = new TestTimeSource();

    AlertingSubsystem service = AlertingSubsystem.create(consumer, timeSource);

    service.updateConfiguration(
        new AlertingConfiguration(
            new AlertConfiguration(AlertMetricType.CPU, true, 80, 30, 14400),
            new AlertConfiguration(AlertMetricType.MEMORY, true, 20, 120, 14400),
            new DefaultConfiguration(true, 5, 120),
            new CollectionPlanConfiguration(
                true,
                EngineMode.immediate,
                Instant.now().minus(100, ChronoUnit.SECONDS),
                120,
                "a-settings-moniker")));

    assertThat(called.get()).isNull();
  }
}
