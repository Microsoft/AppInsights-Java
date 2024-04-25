// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.perfcounter;

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;

public interface PerformanceCounter {

  //void report(TelemetryClient telemetryClient);

  void createMeter();

}
