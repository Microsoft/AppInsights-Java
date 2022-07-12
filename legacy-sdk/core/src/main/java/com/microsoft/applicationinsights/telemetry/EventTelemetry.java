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

package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.schemav2.EventData;
import java.util.concurrent.ConcurrentMap;

/** Telemetry type used to track custom events in Azure Application Insights. */
public final class EventTelemetry extends BaseSampleSourceTelemetry<EventData> {

  private Double samplingPercentage;
  private final EventData data;

  /**
   * Initializes a new instance.
   *
   * @param name The event's name. Max length 150.
   */
  public EventTelemetry(String name) {
    this();
    this.setName(name);
  }

  /** Default initialization for a new instance. */
  public EventTelemetry() {
    data = new EventData();
    initialize(data.getProperties());
  }

  /**
   * Gets a map of application-defined event metrics. These metrics appear along with the event in
   * Search and Analytics, but appear under 'Custom Metrics' in Metrics Explorer.
   *
   * @return The map of metrics
   */
  public ConcurrentMap<String, Double> getMetrics() {
    return data.getMeasurements();
  }

  /**
   * Gets the name of the event.
   *
   * @return The name
   */
  public String getName() {
    return data.getName();
  }

  /**
   * Sets the name of the event.
   *
   * @param name Name of the event. Max length 150.
   */
  public void setName(String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("The event name cannot be null or empty");
    }
    data.setName(name);
  }

  /**
   * Fetches the data structure the instance works with
   *
   * @return The inner data structure.
   */
  @Override
  protected EventData getData() {
    return data;
  }

  @Override
  public Double getSamplingPercentage() {
    return samplingPercentage;
  }

  @Override
  public void setSamplingPercentage(Double samplingPercentage) {
    this.samplingPercentage = samplingPercentage;
  }
}
