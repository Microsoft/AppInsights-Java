// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.serviceprofilerapi.profiler;

import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import com.microsoft.applicationinsights.profiler.config.ServiceProfilerServiceConfig;
import com.microsoft.jfr.RecordingConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Allows loading alternative jfc configuration files. */
public class AlternativeJfrConfigurations {
  private static final Logger LOGGER = LoggerFactory.getLogger(AlternativeJfrConfigurations.class);

  public static final String REDUCED_MEMORY_PROFILE = "reduced-memory-profile.jfc";
  public static final String REDUCED_CPU_PROFILE = "reduced-cpu-profile.jfc";

  public static final String DIAGNOSTIC_MEMORY_PROFILE = "diagnostic-memory-profile.jfc";
  public static final String DIAGNOSTIC_CPU_PROFILE = "diagnostic-cpu-profile.jfc";

  private AlternativeJfrConfigurations() {}

  /** Loads a pre-set recoding file that ships with Application Insights. */
  private static RecordingConfiguration getRecordingConfiguration(
      ProfileTypes profile, String reducedProfile, String diagnosticProfile) {
    switch (profile) {
      case PROFILE_WITHOUT_ENV_DATA:
        return new RecordingConfiguration.JfcFileConfiguration(
            Objects.requireNonNull(
                AlternativeJfrConfigurations.class.getResourceAsStream(reducedProfile)));
      case DIAGNOSTIC_PROFILE:
        return new RecordingConfiguration.JfcFileConfiguration(
            Objects.requireNonNull(
                AlternativeJfrConfigurations.class.getResourceAsStream(diagnosticProfile)));
      default:
        return RecordingConfiguration.PROFILE_CONFIGURATION;
    }
  }

  /** Search for JFC file including the local file system. */
  @SuppressFBWarnings(
      value = "SECPTI", // Potential Path Traversal
      justification =
          "The constructed file path cannot be controlled by an end user of the instrumented application")
  private static RecordingConfiguration getRecordingConfiguration(
      @Nullable String triggeredSettings, AlertMetricType type) {
    if (triggeredSettings != null) {
      try {
        // Look for file on the local file system
        FileInputStream fis = new FileInputStream(triggeredSettings);
        return new RecordingConfiguration.JfcFileConfiguration(fis);
      } catch (FileNotFoundException e) {
        // NOP, to be expected if a configuration and not a file is provided
      }

      // Look for file in the class path
      InputStream fis = AlternativeJfrConfigurations.class.getResourceAsStream(triggeredSettings);
      if (fis != null) {
        return new RecordingConfiguration.JfcFileConfiguration(fis);
      }

      try {
        // Try parsing the triggeredSettings as a pre-configured type
        // Convert from kebab case to enum type
        String enumType = triggeredSettings.toUpperCase().replaceAll("-", "_");
        ProfileTypes profile = ProfileTypes.valueOf(enumType);

        return AlternativeJfrConfigurations.get(profile, type);
      } catch (IllegalArgumentException e) {
        LOGGER.error("Failed to find JFC configuration " + triggeredSettings);
      }
    }

    return RecordingConfiguration.PROFILE_CONFIGURATION;
  }

  public static RecordingConfiguration getCpu(ProfileTypes profile) {
    return getRecordingConfiguration(profile, REDUCED_CPU_PROFILE, DIAGNOSTIC_CPU_PROFILE);
  }

  public static RecordingConfiguration getMemory(ProfileTypes profile) {
    return getRecordingConfiguration(profile, REDUCED_MEMORY_PROFILE, DIAGNOSTIC_MEMORY_PROFILE);
  }

  public static RecordingConfiguration getRequestConfiguration(ProfileTypes profile) {
    // Reusing the cpu profile as the most likely profile type required for a span trigger
    return getRecordingConfiguration(profile, REDUCED_CPU_PROFILE, DIAGNOSTIC_CPU_PROFILE);
  }

  public static RecordingConfiguration get(ProfileTypes profile, AlertMetricType type) {
    switch (type) {
      case MEMORY:
        return getMemory(profile);
      case REQUEST:
        return getRequestConfiguration(profile);
      default:
        return getCpu(profile);
    }
  }

  public static RecordingConfiguration getMemoryProfileConfig(
      ServiceProfilerServiceConfig configuration) {
    return getRecordingConfiguration(
        configuration.memoryTriggeredSettings(), AlertMetricType.MEMORY);
  }

  public static RecordingConfiguration getCpuProfileConfig(
      ServiceProfilerServiceConfig configuration) {
    return getRecordingConfiguration(configuration.cpuTriggeredSettings(), AlertMetricType.CPU);
  }

  public static RecordingConfiguration getSpanProfileConfig(
      ServiceProfilerServiceConfig configuration) {
    return getRecordingConfiguration(configuration.cpuTriggeredSettings(), AlertMetricType.REQUEST);
  }

  public static RecordingConfiguration getManualProfileConfig(
      ServiceProfilerServiceConfig configuration) {
    return getRecordingConfiguration(
        configuration.getManualTriggeredSettings(), AlertMetricType.MANUAL);
  }
}
