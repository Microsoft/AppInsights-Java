// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.config;

import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.profiler.ProfilerConfigurationHandler;
import com.microsoft.applicationinsights.agent.internal.profiler.client.ServiceProfilerClient;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Monitors the Service Profiler endpoint for changes to configuration. */
public class ConfigMonitoringService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigMonitoringService.class);

  // Execution context for the monitoring
  private final ScheduledExecutorService serviceProfilerExecutorService;

  // period of the polling interval
  private final int pollPeriodInSeconds;
  private ScheduledFuture<?> future;

  private final ServiceProfilerClient serviceProfilerClient;
  private ConfigService configService;

  public static ConfigMonitoringService createServiceProfilerConfigService(
      ScheduledExecutorService serviceProfilerExecutorService,
      ServiceProfilerClient serviceProfilerClient,
      List<ProfilerConfigurationHandler> configObservers,
      Configuration.ProfilerConfiguration config) {
    ConfigMonitoringService configMonitoringService =
        new ConfigMonitoringService(
            serviceProfilerExecutorService, config.configPollPeriodSeconds, serviceProfilerClient);
    configMonitoringService.initialize(configObservers);
    return configMonitoringService;
  }

  public ConfigMonitoringService(
      ScheduledExecutorService serviceProfilerExecutorService,
      int pollPeriodInSeconds,
      ServiceProfilerClient serviceProfilerClient) {

    this.serviceProfilerExecutorService = serviceProfilerExecutorService;
    this.pollPeriodInSeconds = pollPeriodInSeconds;
    this.serviceProfilerClient = serviceProfilerClient;
  }

  public void initialize(List<ProfilerConfigurationHandler> observers) {
    scheduleSettingsPull(
        newConfig -> observers.forEach(observer -> observer.updateConfiguration(newConfig)));
  }

  // synchronized to ensure future is not created twice
  private synchronized void scheduleSettingsPull(ProfilerConfigurationHandler handleSettings) {
    if (future != null) {
      throw new IllegalStateException("Service already initialized");
    }

    configService = new ConfigService(serviceProfilerClient);

    // schedule regular config pull
    future =
        serviceProfilerExecutorService.scheduleAtFixedRate(
            pull(handleSettings), 0, pollPeriodInSeconds, TimeUnit.SECONDS);
  }

  // pull settings from the endpoint
  private Runnable pull(ProfilerConfigurationHandler handleSettings) {
    return () -> {
      try {
        configService
            .pullSettings()
            .doOnError(
                e -> {
                  LOGGER.error("Error pulling service profiler settings", e);
                })
            .subscribe(handleSettings::updateConfiguration);
      } catch (RuntimeException e) {
        LOGGER.error("Error pulling service profiler settings", e);
      } catch (Error e) {
        LOGGER.error("Error pulling service profiler settings", e);
        throw e;
      }
    };
  }
}
