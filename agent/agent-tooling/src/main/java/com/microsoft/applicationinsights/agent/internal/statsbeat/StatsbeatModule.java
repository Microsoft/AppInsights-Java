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

package com.microsoft.applicationinsights.agent.internal.statsbeat;

import com.microsoft.applicationinsights.agent.internal.common.ThreadPoolUtils;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import io.opentelemetry.instrumentation.api.cache.Cache;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatsbeatModule {

  private static final Logger logger = LoggerFactory.getLogger(BaseStatsbeat.class);

  private static final ScheduledExecutorService scheduledExecutor =
      Executors.newSingleThreadScheduledExecutor(
          ThreadPoolUtils.createDaemonThreadFactory(BaseStatsbeat.class));

  private final CustomDimensions customDimensions;
  private final NetworkStatsbeat networkStatsbeat;
  private final AttachStatsbeat attachStatsbeat;
  private final FeatureStatsbeat featureStatsbeat;
  private final FeatureStatsbeat instrumentationStatsbeat;
  private final NonessentialStatsbeat nonessentialStatsbeat;
  private final AzureMetadataService azureMetadataService;

  private final AtomicBoolean started = new AtomicBoolean();

  public StatsbeatModule(Cache<String, String> ikeyEndpointMap) {
    customDimensions = new CustomDimensions();
    networkStatsbeat = new NetworkStatsbeat(customDimensions, ikeyEndpointMap);
    attachStatsbeat = new AttachStatsbeat(customDimensions);
    featureStatsbeat = new FeatureStatsbeat(customDimensions, FeatureType.FEATURE);
    instrumentationStatsbeat = new FeatureStatsbeat(customDimensions, FeatureType.INSTRUMENTATION);
    nonessentialStatsbeat = new NonessentialStatsbeat(customDimensions);
    azureMetadataService = new AzureMetadataService(attachStatsbeat, customDimensions);
  }

  public void start(TelemetryClient telemetryClient, Configuration config) {
    if (started.getAndSet(true)) {
      throw new IllegalStateException("initialize already called");
    }

    if (config.internal.statsbeat.disabledAll) {
      // disabledAll is an internal emergency kill-switch to turn off Statsbeat completely when
      // something goes wrong.
      // this happens rarely.
      return;
    }

    long shortIntervalSeconds = config.internal.statsbeat.shortIntervalSeconds;
    long longIntervalSeconds = config.internal.statsbeat.longIntervalSeconds;

    scheduledExecutor.scheduleWithFixedDelay(
        new StatsbeatSender(networkStatsbeat, telemetryClient),
        shortIntervalSeconds,
        shortIntervalSeconds,
        TimeUnit.SECONDS);
    scheduledExecutor.scheduleWithFixedDelay(
        new StatsbeatSender(attachStatsbeat, telemetryClient),
        60,
        longIntervalSeconds,
        TimeUnit.SECONDS);
    scheduledExecutor.scheduleWithFixedDelay(
        new StatsbeatSender(featureStatsbeat, telemetryClient),
        60,
        longIntervalSeconds,
        TimeUnit.SECONDS);
    scheduledExecutor.scheduleWithFixedDelay(
        new StatsbeatSender(instrumentationStatsbeat, telemetryClient),
        60,
        longIntervalSeconds,
        TimeUnit.SECONDS);

    ResourceProvider rp = customDimensions.getResourceProvider();
    // only turn on AzureMetadataService when the resource provider is VM or UNKNOWN.
    if (rp == ResourceProvider.RP_VM || rp == ResourceProvider.UNKNOWN) {
      // will only reach here the first time, after instance has been instantiated
      azureMetadataService.scheduleWithFixedDelay(longIntervalSeconds);
    }

    featureStatsbeat.trackConfigurationOptions(config);

    if (!config.preview.statsbeat.disabled) {
      scheduledExecutor.scheduleWithFixedDelay(
          new StatsbeatSender(nonessentialStatsbeat, telemetryClient),
          longIntervalSeconds,
          longIntervalSeconds,
          TimeUnit.SECONDS);
    } else {
      logger.debug("Non-essential Statsbeat is disabled.");
    }
  }

  public void shutdown() {
    logger.debug("Shutting down Statsbeat scheduler.");
    scheduledExecutor.shutdown();
    azureMetadataService.shutdown();
  }

  public NetworkStatsbeat getNetworkStatsbeat() {
    return networkStatsbeat;
  }

  public FeatureStatsbeat getInstrumentationStatsbeat() {
    return instrumentationStatsbeat;
  }

  public NonessentialStatsbeat getNonessentialStatsbeat() {
    return nonessentialStatsbeat;
  }

  /** Runnable which is responsible for calling the send method to transmit Statsbeat telemetry. */
  private static class StatsbeatSender implements Runnable {

    private final BaseStatsbeat statsbeat;
    private final TelemetryClient telemetryClient;

    private StatsbeatSender(BaseStatsbeat statsbeat, TelemetryClient telemetryClient) {
      this.statsbeat = statsbeat;
      this.telemetryClient = telemetryClient;
    }

    @Override
    public void run() {
      try {
        // For Linux Consumption Plan, connection string is lazily set.
        // There is no need to send statsbeat when cikey is empty.
        String customerIkey = telemetryClient.getInstrumentationKey();
        if (customerIkey == null || customerIkey.isEmpty()) {
          return;
        }
        statsbeat.send(telemetryClient);
      } catch (RuntimeException e) {
        logger.error("Error occurred while sending statsbeat", e);
      }
    }
  }
}
