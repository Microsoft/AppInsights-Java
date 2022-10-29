// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.alerting;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.analysis.TimeSource;
import com.microsoft.applicationinsights.alerting.analysis.data.TelemetryDataPoint;
import com.microsoft.applicationinsights.alerting.analysis.pipelines.AlertPipeline;
import com.microsoft.applicationinsights.alerting.analysis.pipelines.AlertPipelines;
import com.microsoft.applicationinsights.alerting.config.AlertConfiguration;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import com.microsoft.applicationinsights.alerting.config.AlertingConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration;
import com.microsoft.applicationinsights.alerting.config.CollectionPlanConfiguration.EngineMode;
import com.microsoft.applicationinsights.alerting.config.DefaultConfiguration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entrypoint for the alerting subsystem. - Configures alerts according to a provided
 * configuration - Receives telemetry data, feeds it into the appropriate alert pipeline and if
 * necessary issue an alert.
 */
public class AlertingSubsystem {
  private static final Logger LOGGER = LoggerFactory.getLogger(AlertingSubsystem.class);

  // Downstream observer of alerts produced by the alerting system
  private final Consumer<AlertBreach> alertHandler;

  // List of manual triggers that have already been processed
  private final Set<String> manualTriggersExecuted = new HashSet<>();

  private final AlertPipelines alertPipelines;
  private final TimeSource timeSource;

  // Current configuration of the alerting subsystem
  private AlertingConfiguration alertConfig;

  protected AlertingSubsystem(Consumer<AlertBreach> alertHandler) {
    this(alertHandler, TimeSource.DEFAULT);
  }

  protected AlertingSubsystem(Consumer<AlertBreach> alertHandler, TimeSource timeSource) {
    this.alertHandler = alertHandler;
    alertPipelines = new AlertPipelines(alertHandler);
    this.timeSource = timeSource;
  }

  public static AlertingSubsystem create(
      Consumer<AlertBreach> alertHandler, TimeSource timeSource) {
    AlertingSubsystem alertingSubsystem = new AlertingSubsystem(alertHandler, timeSource);
    // init with disabled config
    alertingSubsystem.initialize(
        AlertingConfiguration.create(
            AlertConfiguration.builder()
                .setType(AlertMetricType.CPU)
                .setEnabled(false)
                .setThreshold(0)
                .setProfileDuration(0)
                .setCooldown(0)
                .build(),
            AlertConfiguration.builder()
                .setType(AlertMetricType.MEMORY)
                .setEnabled(false)
                .setThreshold(0)
                .setProfileDuration(0)
                .setCooldown(0)
                .build(),
            DefaultConfiguration.builder()
                .setSamplingEnabled(false)
                .setSamplingRate(0)
                .setSamplingProfileDuration(0)
                .build(),
            CollectionPlanConfiguration.builder()
                .setSingle(false)
                .setMode(EngineMode.immediate)
                .setExpiration(Instant.now())
                .setImmediateProfilingDuration(0)
                .setSettingsMoniker("")
                .build()));
    return alertingSubsystem;
  }

  /** Create alerting pipelines with default configuration. */
  public void initialize(AlertingConfiguration alertConfig) {
    updateConfiguration(alertConfig);
  }

  /** Add telemetry to alert processing pipeline. */
  public void track(@Nullable AlertMetricType type, @Nullable Number value) {
    if (type != null && value != null) {
      trackTelemetryDataPoint(
          TelemetryDataPoint.create(type, timeSource.getNow(), type.name(), value.doubleValue()));
    }
  }

  /** Deliver data to pipelines. */
  public void trackTelemetryDataPoint(@Nullable TelemetryDataPoint telemetryDataPoint) {
    if (telemetryDataPoint == null) {
      return;
    }
    LOGGER.trace(
        "Tracking " + telemetryDataPoint.getType().name() + " " + telemetryDataPoint.getValue());
    alertPipelines.process(telemetryDataPoint);
  }

  /** Apply given configuration to the alerting pipelines. */
  public void updateConfiguration(AlertingConfiguration alertingConfig) {
    if (this.alertConfig == null || !this.alertConfig.equals(alertingConfig)) {
      AlertConfiguration oldCpuConfig =
          this.alertConfig == null ? null : this.alertConfig.getCpuAlert();
      updatePipelineConfig(alertingConfig.getCpuAlert(), oldCpuConfig);

      AlertConfiguration oldMemoryConfig =
          this.alertConfig == null ? null : this.alertConfig.getMemoryAlert();
      updatePipelineConfig(alertingConfig.getMemoryAlert(), oldMemoryConfig);

      evaluateManualTrigger(alertingConfig);
      this.alertConfig = alertingConfig;
    }
  }

  /** If the config has changed update the pipeline. */
  private void updatePipelineConfig(
      AlertConfiguration newAlertConfig, @Nullable AlertConfiguration oldAlertConfig) {
    if (oldAlertConfig == null || !oldAlertConfig.equals(newAlertConfig)) {
      alertPipelines.updateAlertConfig(newAlertConfig, timeSource);
    }
  }

  /** Determine if a manual alert has been requested. */
  private void evaluateManualTrigger(AlertingConfiguration alertConfig) {
    CollectionPlanConfiguration config = alertConfig.getCollectionPlanConfiguration();

    boolean shouldTrigger =
        config.isSingle()
            && config.getMode() == EngineMode.immediate
            && Instant.now().isBefore(config.getExpiration())
            && !manualTriggersExecuted.contains(config.getSettingsMoniker());

    if (shouldTrigger) {
      manualTriggersExecuted.add(config.getSettingsMoniker());

      AlertBreach alertBreach =
          AlertBreach.builder()
              .setType(AlertMetricType.MANUAL)
              .setAlertValue(0.0)
              .setAlertConfiguration(
                  AlertConfiguration.builder()
                      .setType(AlertMetricType.MANUAL)
                      .setEnabled(true)
                      .setProfileDuration(config.getImmediateProfilingDuration())
                      .build())
              .setProfileId(UUID.randomUUID().toString())
              .setCpuMetric(0)
              .setMemoryUsage(0)
              .build();
      alertHandler.accept(alertBreach);
    }
  }

  public void setPipeline(AlertMetricType type, AlertPipeline alertPipeline) {
    alertPipelines.setAlertPipeline(type, alertPipeline);
  }
}
