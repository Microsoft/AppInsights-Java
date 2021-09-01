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

import com.microsoft.applicationinsights.agent.internal.common.Strings;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryUtil;
import io.opentelemetry.instrumentation.api.caching.Cache;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.checkerframework.checker.lock.qual.GuardedBy;

public class NetworkStatsbeat extends BaseStatsbeat {

  private static final String REQUEST_SUCCESS_COUNT_METRIC_NAME = "Request Success Count";
  private static final String REQUEST_FAILURE_COUNT_METRIC_NAME = "Requests Failure Count ";
  private static final String REQUEST_DURATION_METRIC_NAME = "Request Duration";
  private static final String RETRY_COUNT_METRIC_NAME = "Retry Count";
  private static final String THROTTLE_COUNT_METRIC_NAME = "Throttle Count";
  private static final String EXCEPTION_COUNT_METRIC_NAME = "Exception Count";
  private static final String BREEZE_ENDPOINT = "breeze";

  private final Object lock = new Object();
  private final Cache<String, String> ikeyEndpointMap;

  @GuardedBy("lock")
  private final Map<String, IntervalMetrics> instrumentationKeyCounterMap = new HashMap<>();

  // only used by tests
  public NetworkStatsbeat() {
    super(new CustomDimensions());
    this.ikeyEndpointMap = Cache.newBuilder().build();
  }

  public NetworkStatsbeat(
      CustomDimensions customDimensions, Cache<String, String> ikeyEndpointMap) {
    super(customDimensions);
    this.ikeyEndpointMap = ikeyEndpointMap;
  }

  @Override
  protected void send(TelemetryClient telemetryClient) {
    Map<String, IntervalMetrics> local;
    synchronized (lock) {
      local = new HashMap<>(instrumentationKeyCounterMap);
      instrumentationKeyCounterMap.clear();
    }

    for (Map.Entry<String, IntervalMetrics> entry : local.entrySet()) {
      String ikey = entry.getKey();
      String endpointUrl = ikeyEndpointMap.get(ikey);
      if (Strings.isNullOrEmpty(endpointUrl)) {
        endpointUrl = telemetryClient.getEndpointProvider().getIngestionEndpointUrl().toString();
      }

      sendIntervalMetric(telemetryClient, ikey, entry.getValue(), getHost(endpointUrl));
    }
  }

  public void incrementRequestSuccessCount(long duration, String ikey) {
    doWithIntervalMetrics(
        ikey,
        intervalMetrics -> {
          intervalMetrics.requestSuccessCount.incrementAndGet();
          intervalMetrics.totalRequestDuration.getAndAdd(duration);
        });
  }

  public void incrementRequestFailureCount(String ikey) {
    doWithIntervalMetrics(
        ikey, intervalMetrics -> intervalMetrics.requestFailureCount.incrementAndGet());
  }

  public void incrementRetryCount(String ikey) {
    doWithIntervalMetrics(ikey, intervalMetrics -> intervalMetrics.retryCount.incrementAndGet());
  }

  public void incrementThrottlingCount(String ikey) {
    doWithIntervalMetrics(
        ikey, intervalMetrics -> intervalMetrics.throttlingCount.incrementAndGet());
  }

  void incrementExceptionCount(String ikey) {
    doWithIntervalMetrics(
        ikey, intervalMetrics -> intervalMetrics.exceptionCount.incrementAndGet());
  }

  // only used by tests
  long getRequestSuccessCount(String ikey) {
    synchronized (lock) {
      IntervalMetrics intervalMetrics = instrumentationKeyCounterMap.get(ikey);
      return intervalMetrics == null ? 0L : intervalMetrics.requestSuccessCount.get();
    }
  }

  // only used by tests
  long getRequestFailureCount(String ikey) {
    synchronized (lock) {
      IntervalMetrics intervalMetrics = instrumentationKeyCounterMap.get(ikey);
      return intervalMetrics == null ? 0L : intervalMetrics.requestFailureCount.get();
    }
  }

  // only used by tests
  double getRequestDurationAvg(String ikey) {
    synchronized (lock) {
      IntervalMetrics intervalMetrics = instrumentationKeyCounterMap.get(ikey);
      return intervalMetrics == null ? 0L : intervalMetrics.getRequestDurationAvg();
    }
  }

  // only used by tests
  long getRetryCount(String ikey) {
    synchronized (lock) {
      IntervalMetrics intervalMetrics = instrumentationKeyCounterMap.get(ikey);
      return intervalMetrics == null ? 0L : intervalMetrics.retryCount.get();
    }
  }

  // only used by tests
  long getThrottlingCount(String ikey) {
    synchronized (lock) {
      IntervalMetrics intervalMetrics = instrumentationKeyCounterMap.get(ikey);
      return intervalMetrics == null ? 0L : intervalMetrics.throttlingCount.get();
    }
  }

  // only used by tests
  long getExceptionCount(String ikey) {
    synchronized (lock) {
      IntervalMetrics intervalMetrics = instrumentationKeyCounterMap.get(ikey);
      return intervalMetrics == null ? 0L : intervalMetrics.exceptionCount.get();
    }
  }

  private void doWithIntervalMetrics(String ikey, Consumer<IntervalMetrics> update) {
    synchronized (lock) {
      update.accept(instrumentationKeyCounterMap.computeIfAbsent(ikey, k -> new IntervalMetrics()));
    }
  }

  private void sendIntervalMetric(
      TelemetryClient telemetryClient, String ikey, IntervalMetrics local, String host) {
    if (local.requestSuccessCount.get() != 0) {
      TelemetryItem requestSuccessCountSt =
          createStatsbeatTelemetry(
              telemetryClient, REQUEST_SUCCESS_COUNT_METRIC_NAME, local.requestSuccessCount.get());
      addCommonProperties(requestSuccessCountSt, ikey, host);
      telemetryClient.trackStatsbeatAsync(requestSuccessCountSt);
    }

    if (local.requestFailureCount.get() != 0) {
      TelemetryItem requestFailureCountSt =
          createStatsbeatTelemetry(
              telemetryClient, REQUEST_FAILURE_COUNT_METRIC_NAME, local.requestFailureCount.get());
      addCommonProperties(requestFailureCountSt, ikey, host);
      telemetryClient.trackStatsbeatAsync(requestFailureCountSt);
    }

    double durationAvg = local.getRequestDurationAvg();
    if (durationAvg != 0) {
      TelemetryItem requestDurationSt =
          createStatsbeatTelemetry(telemetryClient, REQUEST_DURATION_METRIC_NAME, durationAvg);
      addCommonProperties(requestDurationSt, ikey, host);
      telemetryClient.trackStatsbeatAsync(requestDurationSt);
    }

    if (local.retryCount.get() != 0) {
      TelemetryItem retryCountSt =
          createStatsbeatTelemetry(
              telemetryClient, RETRY_COUNT_METRIC_NAME, local.retryCount.get());
      addCommonProperties(retryCountSt, ikey, host);
      telemetryClient.trackStatsbeatAsync(retryCountSt);
    }

    if (local.throttlingCount.get() != 0) {
      TelemetryItem throttleCountSt =
          createStatsbeatTelemetry(
              telemetryClient, THROTTLE_COUNT_METRIC_NAME, local.throttlingCount.get());
      addCommonProperties(throttleCountSt, ikey, host);
      telemetryClient.trackStatsbeatAsync(throttleCountSt);
    }

    if (local.exceptionCount.get() != 0) {
      TelemetryItem exceptionCountSt =
          createStatsbeatTelemetry(
              telemetryClient, EXCEPTION_COUNT_METRIC_NAME, local.exceptionCount.get());
      addCommonProperties(exceptionCountSt, ikey, host);
      telemetryClient.trackStatsbeatAsync(exceptionCountSt);
    }
  }

  private static void addCommonProperties(TelemetryItem telemetryItem, String ikey, String host) {
    Map<String, String> properties =
        TelemetryUtil.getProperties(telemetryItem.getData().getBaseData());
    properties.put("endpoint", BREEZE_ENDPOINT);
    properties.put("cikey", ikey);
    properties.put("host", host);
  }

  private static class IntervalMetrics {
    private final AtomicLong requestSuccessCount = new AtomicLong();
    private final AtomicLong requestFailureCount = new AtomicLong();
    // request duration count only counts request success.
    private final AtomicLong totalRequestDuration = new AtomicLong(); // duration in milliseconds
    private final AtomicLong retryCount = new AtomicLong();
    private final AtomicLong throttlingCount = new AtomicLong();
    private final AtomicLong exceptionCount = new AtomicLong();

    private double getRequestDurationAvg() {
      double sum = totalRequestDuration.get();
      if (requestSuccessCount.get() != 0) {
        return sum / requestSuccessCount.get();
      }

      return sum;
    }
  }

  /**
   * e.g. endpointUrl 'https://westus-0.in.applicationinsights.azure.com/v2.1/track' host will
   * return 'westus-0.in.applicationinsights.azure.com'
   */
  static String getHost(String endpointUrl) {
    assert (endpointUrl != null && !endpointUrl.isEmpty());
    int start = endpointUrl.indexOf("://");
    if (start != -1) {
      int end = endpointUrl.indexOf("/", start + 3);
      if (end != -1) {
        return endpointUrl.substring(start + 3, end);
      }

      return endpointUrl.substring(start + 3);
    }

    int end = endpointUrl.indexOf("/");
    if (end != -1) {
      return endpointUrl.substring(0, end);
    }

    return endpointUrl;
  }
}
