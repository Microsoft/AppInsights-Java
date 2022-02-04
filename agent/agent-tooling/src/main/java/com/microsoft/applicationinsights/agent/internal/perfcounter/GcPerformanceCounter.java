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

package com.microsoft.applicationinsights.agent.internal.perfcounter;

import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

/** The class reports GC related data. */
public final class GcPerformanceCounter implements PerformanceCounter {

  private static final String GC_TOTAL_COUNT = "GC Total Count";
  private static final String GC_TOTAL_TIME = "GC Total Time";

  private long currentTotalCount = 0;
  private long currentTotalTime = 0;

  @Override
  public String getId() {
    return "GCPerformanceCounter";
  }

  @Override
  public void report(TelemetryClient telemetryClient) {
    synchronized (this) {
      List<GarbageCollectorMXBean> gcs = ManagementFactory.getGarbageCollectorMXBeans();
      if (gcs.isEmpty()) {
        return;
      }

      long totalCollectionCount = 0;
      long totalCollectionTime = 0;
      for (GarbageCollectorMXBean gc : gcs) {
        long gcCollectionCount = gc.getCollectionCount();
        if (gcCollectionCount > 0) {
          totalCollectionCount += gcCollectionCount;
        }

        long gcCollectionTime = gc.getCollectionTime();
        if (gcCollectionTime > 0) {
          totalCollectionTime += gcCollectionTime;
        }
      }

      long countToReport = totalCollectionCount - currentTotalCount;
      long timeToReport = totalCollectionTime - currentTotalTime;

      currentTotalCount = totalCollectionCount;
      currentTotalTime = totalCollectionTime;

      telemetryClient.trackAsync(telemetryClient.newMetricTelemetry(GC_TOTAL_COUNT, countToReport));
      telemetryClient.trackAsync(telemetryClient.newMetricTelemetry(GC_TOTAL_TIME, timeToReport));
    }
  }
}
