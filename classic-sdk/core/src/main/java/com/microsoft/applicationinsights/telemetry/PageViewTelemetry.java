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

import com.microsoft.applicationinsights.internal.schemav2.PageViewData;
import java.net.URI;
import java.util.concurrent.ConcurrentMap;

/**
 * Telemetry type used to track page views.
 *
 * <p>You can send information about pages viewed by your application to Application Insights by
 * passing an instance of this class to the 'trackPageView' method of the {@link
 * com.microsoft.applicationinsights.TelemetryClient}
 */
public final class PageViewTelemetry extends BaseTelemetry {

  private final PageViewData data;

  /** Initializes a new instance of the class with the specified {@code pageName}. */
  public PageViewTelemetry(String pageName) {
    this();
    setName(pageName);
  }

  public PageViewTelemetry() {
    data = new PageViewData();
    initialize(data.getProperties());
  }

  /** Sets the name of the page view. */
  public void setName(String name) {
    data.setName(name);
  }

  /** Gets the name of the page view. */
  public String getName() {
    return data.getName();
  }

  /** Gets the page view Uri. */
  public URI getUri() {
    return data.getUri();
  }

  /** Sets the page view Uri. */
  public void setUrl(URI uri) {
    data.setUri(uri);
  }

  /** Gets the page view duration. */
  public long getDuration() {
    return data.getDuration();
  }

  /** Sets the page view duration. */
  public void setDuration(long duration) {
    data.setDuration(duration);
  }

  /** Gets a dictionary of custom defined metrics. */
  public ConcurrentMap<String, Double> getMetrics() {
    return data.getMeasurements();
  }

  @Override
  protected PageViewData getData() {
    return data;
  }
}
