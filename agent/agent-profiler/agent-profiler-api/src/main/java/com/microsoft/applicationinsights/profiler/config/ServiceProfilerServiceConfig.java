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

package com.microsoft.applicationinsights.profiler.config;

import java.io.File;
import java.net.URL;

/** Configuration of the service profiler subsystem. */
public class ServiceProfilerServiceConfig {

  public static final int DEFAULT_CONFIG_POLL_PERIOD_IN_MS = 60000;
  public static final int DEFAULT_PERIODIC_RECORDING_DURATION_IN_S = 120;
  public static final int DEFAULT_PERIODIC_RECORDING_INTERVAL_IN_S = 60 * 60;

  // duration between polls for configuration changes
  private final int configPollPeriod;

  // default duration of periodic profiles
  private final int periodicRecordingDuration;

  // default interval of periodic profiles
  private final int periodicRecordingInterval;

  private final URL serviceProfilerFrontEndPoint;

  // Either an inbuilt profile as defined in ProfileTypes, or a path to a custom JFC file to use for
  // memory profiling
  private final String memoryTriggeredSettings;

  // Either an inbuilt profile as defined in ProfileTypes, or a path to a custom JFC file to use for
  // cpu profiling
  private final String cpuTriggeredSettings;

  // Either an inbuilt profile as defined in ProfileTypes, or a path to a custom JFC file to use for
  // manual profiling
  private final String manualTriggeredSettings;

  // Location to which jfr files will be temporarily held
  private final File tempDirectory;

  private final boolean enableDiagnostics;

  public ServiceProfilerServiceConfig(
      int configPollPeriod,
      int periodicRecordingDuration,
      int periodicRecordingInterval,
      URL serviceProfilerFrontEndPoint,
      String memoryTriggeredSettings,
      String cpuTriggeredSettings,
      String manualTriggeredSettings,
      File tempDirectory,
      boolean enableDiagnostics) {
    this.configPollPeriod = configPollPeriod;
    this.periodicRecordingDuration = periodicRecordingDuration;
    this.periodicRecordingInterval = periodicRecordingInterval;
    this.serviceProfilerFrontEndPoint = serviceProfilerFrontEndPoint;
    this.memoryTriggeredSettings = memoryTriggeredSettings;
    this.cpuTriggeredSettings = cpuTriggeredSettings;
    this.manualTriggeredSettings = manualTriggeredSettings;
    this.tempDirectory = tempDirectory;
    this.enableDiagnostics = enableDiagnostics;
  }

  public int getConfigPollPeriod() {
    return configPollPeriod != -1 ? configPollPeriod * 1000 : DEFAULT_CONFIG_POLL_PERIOD_IN_MS;
  }

  public long getPeriodicRecordingDuration() {
    return periodicRecordingDuration != -1
        ? periodicRecordingDuration
        : DEFAULT_PERIODIC_RECORDING_DURATION_IN_S;
  }

  public long getPeriodicRecordingInterval() {
    return periodicRecordingInterval != -1
        ? periodicRecordingInterval
        : DEFAULT_PERIODIC_RECORDING_INTERVAL_IN_S;
  }

  public URL getServiceProfilerFrontEndPoint() {
    return serviceProfilerFrontEndPoint;
  }

  public String memoryTriggeredSettings() {
    return memoryTriggeredSettings;
  }

  public String cpuTriggeredSettings() {
    return cpuTriggeredSettings;
  }

  public String getManualTriggeredSettings() {
    return manualTriggeredSettings;
  }

  public File tempDirectory() {
    return tempDirectory;
  }

  public boolean enableDiagnostics() {
    return enableDiagnostics;
  }
}
