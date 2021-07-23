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

public class TelemetryTruncation {

  private static final ExceptionStats exceptionStats =
      new ExceptionStats(TelemetryTruncation.class, "Telemetry was truncated");

  public static String truncateTelemetry(String value, int maxLength, String attributeName) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    exceptionStats.recordFailure("truncated " + attributeName + ": " + value);
    return value.substring(0, maxLength);
  }

  // need a separate method because don't want to concatenate "property[key]" on every call above
  // which would be memory allocating
  public static String truncatePropertyValue(String value, int maxLength, String key) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    exceptionStats.recordFailure("truncated property[" + key + "]: " + value);
    return value.substring(0, maxLength);
  }

  private TelemetryTruncation() {}
}
