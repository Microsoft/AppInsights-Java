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

import java.util.HashSet;
import java.util.Set;

class FeatureStatsbeat extends BaseStatsbeat {

    private static final String FEATURE_METRIC_NAME = "Feature";

    private final Set<Feature> featureList = new HashSet<>(64);

    FeatureStatsbeat(TelemetryClient telemetryClient, long interval) {
        super(telemetryClient, interval);

        // track java distribution
        String javaVendor = System.getProperty("java.vendor");
        featureList.add(Feature.fromJavaVendor(javaVendor));
    }

    /**
     * @return a long that represents a list of features enabled. Each bitfield maps to a feature.
     */
    long getFeature() {
        return Feature.encode(featureList);
    }

    @Override
    protected void send() {
        MetricTelemetry statsbeatTelemetry = createStatsbeatTelemetry(FEATURE_METRIC_NAME, 0);
        statsbeatTelemetry.getProperties().put("feature", String.valueOf(getFeature()));
        telemetryClient.track(statsbeatTelemetry);
    }
}
