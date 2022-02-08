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

package com.microsoft.applicationinsights.agent.internal.common;

import com.azure.monitor.opentelemetry.exporter.implementation.models.DataPointType;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricDataPoint;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricsData;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MonitorBase;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import com.azure.monitor.opentelemetry.exporter.implementation.utils.FormattedTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TestUtils {

  public static TelemetryItem createMetricTelemetry(
      String name, int value, String instrumentationKey) {
    TelemetryItem telemetry = new TelemetryItem();
    telemetry.setVersion(1);
    telemetry.setName("Metric");
    telemetry.setInstrumentationKey(instrumentationKey);
    Map<String, String> tags = new HashMap<>();
    tags.put("ai.internal.sdkVersion", "test_version");
    tags.put("ai.internal.nodeName", "test_role_name");
    tags.put("ai.cloud.roleInstance", "test_cloud_name");
    telemetry.setTags(tags);

    MetricsData data = new MetricsData();
    List<MetricDataPoint> dataPoints = new ArrayList<>();
    MetricDataPoint dataPoint = new MetricDataPoint();
    dataPoint.setDataPointType(DataPointType.MEASUREMENT);
    dataPoint.setName(name);
    dataPoint.setValue(value);
    dataPoint.setCount(1);
    dataPoints.add(dataPoint);

    Map<String, String> properties = new HashMap<>();
    properties.put("state", "blocked");

    data.setMetrics(dataPoints);
    data.setProperties(properties);

    MonitorBase monitorBase = new MonitorBase();
    monitorBase.setBaseType("MetricData");
    monitorBase.setBaseData(data);
    telemetry.setData(monitorBase);
    telemetry.setTime(FormattedTime.offSetDateTimeFromNow());

    return telemetry;
  }

  private TestUtils() {}
}
