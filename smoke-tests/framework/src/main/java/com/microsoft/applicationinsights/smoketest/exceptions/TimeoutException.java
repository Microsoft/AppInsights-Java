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

package com.microsoft.applicationinsights.smoketest.exceptions;

import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

public class TimeoutException extends SmokeTestException {
  public TimeoutException(String componentName, long timeout, TimeUnit unit) {
    this(componentName, timeout, unit, null, "");
  }

  public TimeoutException(
      String componentName, long timeout, TimeUnit unit, @Nullable Throwable cause) {
    this(componentName, timeout, unit, cause, "");
  }

  public TimeoutException(
      String componentName,
      long timeout,
      TimeUnit unit,
      @Nullable Throwable cause,
      String message) {
    super(
        String.format(
            "Timeout reached (%d %s) waiting for %s. %s",
            timeout, unit.toString().toLowerCase(), componentName, message),
        cause);
  }
}
