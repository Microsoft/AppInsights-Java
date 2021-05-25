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

package com.microsoft.applicationinsights.internal.statsbeat;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class NetworkStatsbeat extends BaseStatsbeat {

    private static final String REQUEST_SUCCESS_COUNT_METRIC_NAME = "Request Success Count";
    private static final String REQUEST_FAILURE_COUNT_METRIC_NAME = "Requests Failure Count ";
    private static final String REQUEST_DURATION_METRIC_NAME = "Request Duration";
    private static final String RETRY_COUNT_METRIC_NAME = "Retry Count";
    private static final String THROTTLE_COUNT_METRIC_NAME = "Throttle Count";
    private static final String EXCEPTION_COUNT_METRIC_NAME = "Exception Count";

    private static final String INSTRUMENTATION_CUSTOM_DIMENSION = "instrumentation";

    private volatile IntervalMetrics current;

    private final Object lock = new Object();

    NetworkStatsbeat(TelemetryClient telemetryClient, long interval) {
        super(telemetryClient, interval);
        current = new IntervalMetrics();
    }

    @Override
    protected void send() {
        IntervalMetrics local;
        synchronized (lock) {
            local = current;
            current = new IntervalMetrics();
        }

        // send instrumentation as an UTF-8 string
        String instrumentation = String.valueOf(Instrumentations.encode(local.instrumentationList));

        if (local.requestSuccessCount.get() != 0) {
            MetricTelemetry requestSuccessCountSt = createStatsbeatTelemetry(REQUEST_SUCCESS_COUNT_METRIC_NAME, local.requestSuccessCount.get());
            requestSuccessCountSt.getProperties().put(INSTRUMENTATION_CUSTOM_DIMENSION, instrumentation);
            telemetryClient.track(requestSuccessCountSt);
        }

        if (local.requestFailureCount.get() != 0) {
            MetricTelemetry requestFailureCountSt = createStatsbeatTelemetry(REQUEST_FAILURE_COUNT_METRIC_NAME, local.requestFailureCount.get());
            requestFailureCountSt.getProperties().put(INSTRUMENTATION_CUSTOM_DIMENSION, instrumentation);
            telemetryClient.track(requestFailureCountSt);
        }

        double durationAvg = local.getRequestDurationAvg();
        if (durationAvg != 0) {
            MetricTelemetry requestDurationSt = createStatsbeatTelemetry(REQUEST_DURATION_METRIC_NAME, durationAvg);
            requestDurationSt.getProperties().put(INSTRUMENTATION_CUSTOM_DIMENSION, instrumentation);
            telemetryClient.track(requestDurationSt);
        }

        if (local.retryCount.get() != 0) {
            MetricTelemetry retryCountSt = createStatsbeatTelemetry(RETRY_COUNT_METRIC_NAME, local.retryCount.get());
            retryCountSt.getProperties().put(INSTRUMENTATION_CUSTOM_DIMENSION, instrumentation);
            telemetryClient.track(retryCountSt);
        }

        if (local.throttlingCount.get() != 0) {
            MetricTelemetry throttleCountSt = createStatsbeatTelemetry(THROTTLE_COUNT_METRIC_NAME, local.throttlingCount.get());
            throttleCountSt.getProperties().put(INSTRUMENTATION_CUSTOM_DIMENSION, instrumentation);
            telemetryClient.track(throttleCountSt);
        }

        if (local.exceptionCount.get() != 0) {
            MetricTelemetry exceptionCountSt = createStatsbeatTelemetry(EXCEPTION_COUNT_METRIC_NAME, local.exceptionCount.get());
            exceptionCountSt.getProperties().put(INSTRUMENTATION_CUSTOM_DIMENSION, instrumentation);
            telemetryClient.track(exceptionCountSt);
        }
    }

    // this is used by Exporter
    public void addInstrumentation(String instrumentation) {
        synchronized (lock) {
            current.instrumentationList.add(instrumentation);
        }
    }

    public void incrementRequestSuccessCount(long duration) {
        synchronized (lock) {
            current.requestSuccessCount.incrementAndGet();
            current.totalRequestDuration.getAndAdd(duration);
        }
    }

    public void incrementRequestFailureCount() {
        synchronized (lock) {
            current.requestFailureCount.incrementAndGet();
        }
    }

    public void incrementRetryCount() {
        synchronized (lock) {
            current.retryCount.incrementAndGet();
        }
    }

    public void incrementThrottlingCount() {
        synchronized (lock) {
            current.throttlingCount.incrementAndGet();
        }
    }

    void incrementExceptionCount() {
        synchronized (lock) {
            current.exceptionCount.incrementAndGet();
        }
    }

    // only used by tests
    long getInstrumentation() {
        return Instrumentations.encode(current.instrumentationList);
    }

    // only used by tests
    long getRequestSuccessCount() {
        return current.requestSuccessCount.get();
    }

    // only used by tests
    long getRequestFailureCount() {
        return current.requestFailureCount.get();
    }

    // only used by tests
    double getRequestDurationAvg() { return current.getRequestDurationAvg(); }

    // only used by tests
    long getRetryCount() {
        return current.retryCount.get();
    }

    // only used by tests
    long getThrottlingCount() {
        return current.throttlingCount.get();
    }

    // only used by tests
    long getExceptionCount() {
        return current.exceptionCount.get();
    }

    // only used by tests
    Set<String> getInstrumentationList() {
        return current.instrumentationList;
    }

    private static class IntervalMetrics {
        private final Set<String> instrumentationList = Collections.newSetFromMap(new ConcurrentHashMap<>());
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

            return  sum;
        }
    }
}
