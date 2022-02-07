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

package com.azure.monitor.opentelemetry.exporter.implementation.builders;

import com.azure.monitor.opentelemetry.exporter.implementation.models.DataPointType;
import com.azure.monitor.opentelemetry.exporter.implementation.models.MetricDataPoint;

public final class MetricPointBuilder {

  private static final int MAX_METRIC_NAME_SPACE_LENGTH = 256;
  private static final int MAX_NAME_LENGTH = 1024;

  private final MetricDataPoint data = new MetricDataPoint();

  public void setNamespace(String namespace) {
    data.setNamespace(
        TelemetryTruncation.truncateTelemetry(
            namespace, MAX_METRIC_NAME_SPACE_LENGTH, "MetricPoint.namespace"));
  }

  public void setName(String name) {
    data.setName(TelemetryTruncation.truncateTelemetry(name, MAX_NAME_LENGTH, "MetricPoint.name"));
  }

  public void setDataPointType(DataPointType dataPointType) {
    data.setDataPointType(dataPointType);
  }

  public void setValue(double value) {
    data.setValue(value);
  }

  public void setCount(Integer count) {
    data.setCount(count);
  }

  public void setMin(Double min) {
    data.setMin(min);
  }

  public void setMax(Double max) {
    data.setMax(max);
  }

  public void setStdDev(Double stdDev) {
    data.setStdDev(stdDev);
  }

  MetricDataPoint build() {
    return data;
  }
}
