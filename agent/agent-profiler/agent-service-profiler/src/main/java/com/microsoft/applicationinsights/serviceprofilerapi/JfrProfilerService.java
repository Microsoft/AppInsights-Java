// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.serviceprofilerapi;

import com.microsoft.applicationinsights.profiler.ProfileHandler;
import com.microsoft.applicationinsights.profiler.Profiler;
import com.microsoft.applicationinsights.profiler.ProfilerConfigurationHandler;
import com.microsoft.applicationinsights.profiler.ProfilerService;
import com.microsoft.applicationinsights.profiler.config.ServiceProfilerServiceConfig;
import com.microsoft.applicationinsights.serviceprofilerapi.client.ServiceProfilerClientV2;
import com.microsoft.applicationinsights.serviceprofilerapi.config.ServiceProfilerConfigMonitorService;
import com.microsoft.applicationinsights.serviceprofilerapi.profiler.JfrUploadService;
import com.microsoft.applicationinsights.serviceprofilerapi.upload.ServiceProfilerUploader;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JFR Service Profiler main entry point, wires up the items below.
 *
 * <ul>
 *   <li>Configuration polling
 *   <li>Notifying upstream
 *   <li>consumers (such as the alerting subsystem) of configuration updates
 *   <li>JFR Profiling service
 *   <li>JFR Uploader service
 * </ul>
 */
public class JfrProfilerService implements ProfilerService {

  private static final Logger LOGGER = LoggerFactory.getLogger(JfrProfilerService.class);

  private static final String APP_ID_PREFIX = "cid-v1:";

  private final ServiceProfilerServiceConfig config;
  private final ServiceProfilerClientV2 serviceProfilerClient;
  private final ServiceProfilerUploader serviceProfilerUploader;

  private final Supplier<String> appIdSupplier;

  @SuppressWarnings("unused")
  private final Profiler profiler;

  private final ScheduledExecutorService serviceProfilerExecutorService;
  private final ProfilerConfigurationHandler profilerConfigurationHandler;

  private final AtomicBoolean initialised = new AtomicBoolean();

  private ProfileHandler profileHandler;

  public JfrProfilerService(
      Supplier<String> appIdSupplier,
      ServiceProfilerServiceConfig config,
      Profiler profiler,
      ProfilerConfigurationHandler profilerConfigurationHandler,
      ServiceProfilerClientV2 serviceProfilerClient,
      ServiceProfilerUploader serviceProfilerUploader,
      ScheduledExecutorService serviceProfilerExecutorService) {
    this.appIdSupplier = getAppId(appIdSupplier);
    this.config = config;
    this.profiler = profiler;
    this.serviceProfilerClient = serviceProfilerClient;
    this.serviceProfilerUploader = serviceProfilerUploader;
    this.serviceProfilerExecutorService = serviceProfilerExecutorService;
    this.profilerConfigurationHandler = profilerConfigurationHandler;
  }

  public Future<ProfilerService> initialize() {
    CompletableFuture<ProfilerService> result = new CompletableFuture<>();
    if (initialised.getAndSet(true)) {
      result.complete(this);
      return result;
    }

    LOGGER.warn("INITIALISING JFR PROFILING SUBSYSTEM THIS FEATURE IS IN BETA");

    profileHandler = new JfrUploadService(serviceProfilerUploader, appIdSupplier);

    serviceProfilerExecutorService.submit(
        () -> {
          try {
            // Daemon remains alive permanently due to scheduling an update
            profiler.initialize(profileHandler, serviceProfilerExecutorService);

            // Monitor service remains alive permanently due to scheduling an periodic config pull
            ServiceProfilerConfigMonitorService.createServiceProfilerConfigService(
                serviceProfilerExecutorService,
                serviceProfilerClient,
                Arrays.asList(profilerConfigurationHandler, profiler),
                config);

            result.complete(this);
          } catch (Throwable t) {
            LOGGER.error(
                "Failed to initialise profiler service",
                new RuntimeException(
                    "Unable to obtain JFR connection, this may indicate that your JVM does not"
                        + " have JFR enabled. JFR profiling system will shutdown"));
            result.completeExceptionally(t);
          }
        });
    return result;
  }

  private static Supplier<String> getAppId(Supplier<String> supplier) {
    return () -> {
      String appId = supplier.get();

      if (appId == null || appId.isEmpty()) {
        return null;
      }

      if (appId.startsWith(APP_ID_PREFIX)) {
        appId = appId.substring(APP_ID_PREFIX.length());
      }
      return appId;
    };
  }

  @Override
  public Profiler getProfiler() {
    return profiler;
  }
}
