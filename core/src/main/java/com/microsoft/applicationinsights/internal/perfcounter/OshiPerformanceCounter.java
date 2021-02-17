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

package com.microsoft.applicationinsights.internal.perfcounter;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

public class OshiPerformanceCounter implements PerformanceCounter {

    private static final Logger logger = LoggerFactory.getLogger(OshiPerformanceCounter.class);
    private static final String ID = Constants.PERFORMANCE_COUNTER_PREFIX + "OshiPerformanceCounter";
    private final static double MILLIS_IN_SECOND = 1000;
    private long prevCollectionInMillis = -1;
    private double prevProcessIO;
    private double currentProcessIO;
    private OSProcess processInfo;
    private double prevTotalProcessorTime;
    private double currentTotalProcessorTime;

    public OshiPerformanceCounter() {
        SystemInfo systemInfo = new SystemInfo();
        OperatingSystem osInfo = systemInfo.getOperatingSystem();
        processInfo = osInfo.getProcess(osInfo.getProcessId());
        processInfo.updateAttributes();

        currentProcessIO = (double) (processInfo.getBytesRead() + processInfo.getBytesWritten());
        prevProcessIO = currentProcessIO;

        currentTotalProcessorTime = processInfo.getUserTime() + processInfo.getKernelTime();
        prevTotalProcessorTime = currentTotalProcessorTime;
    }

    @Override public String getId() {
        return ID;
    }

    @Override public void report(TelemetryClient telemetryClient) {
        processInfo.updateAttributes();

        long currentCollectionInMillis = System.currentTimeMillis();
        currentProcessIO = (double) (processInfo.getBytesRead() + processInfo.getBytesWritten());
        currentTotalProcessorTime = processInfo.getUserTime() + processInfo.getKernelTime();
        if (prevCollectionInMillis != -1) {
            double timeElapsedInSeconds = (currentCollectionInMillis - prevCollectionInMillis) / MILLIS_IN_SECOND;
            double processIo = (currentProcessIO - prevProcessIO) / timeElapsedInSeconds;
            send(telemetryClient, processIo, Constants.PROCESS_IO_PC_METRIC_NAME);
            logger.trace("Sent performance counter for '{}': '{}'", Constants.PROCESS_IO_PC_METRIC_NAME, processIo);

            double processorTime = (currentTotalProcessorTime - prevTotalProcessorTime) / timeElapsedInSeconds;
            send(telemetryClient, processorTime, Constants.TOTAL_CPU_PC_METRIC_NAME);
            logger.trace("Sent performance counter for '{}': '{}'", Constants.TOTAL_CPU_PC_METRIC_NAME, processorTime);
        }

        prevProcessIO = currentProcessIO;
        prevTotalProcessorTime = currentTotalProcessorTime;
        prevCollectionInMillis = currentCollectionInMillis;
    }

    private void send(TelemetryClient telemetryClient, double value, String metricName) {
        MetricTelemetry telemetry = new MetricTelemetry(metricName, value);
        telemetryClient.track(telemetry);
    }
}
