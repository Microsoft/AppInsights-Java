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

package com.microsoft.applicationinsights.agent.internal.exporter.builders;

import static com.microsoft.applicationinsights.agent.internal.common.TelemetryTruncation.truncateTelemetry;

import com.microsoft.applicationinsights.agent.internal.exporter.models.StackFrame;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryExceptionDetails;
import com.microsoft.applicationinsights.agent.internal.exporter.utils.SanitizationHelper;
import java.util.ArrayList;
import java.util.List;

public final class ExceptionDetailBuilder {

  private final TelemetryExceptionDetails data = new TelemetryExceptionDetails();

  public void setId(Integer id) {
    data.setId(id);
  }

  public void setOuter(ExceptionDetailBuilder outer) {
    data.setOuterId(outer.data.getId());
  }

  public void setTypeName(String typeName) {
    data.setTypeName(
        truncateTelemetry(
            typeName, SanitizationHelper.MAX_NAME_LENGTH, "ExceptionDetail.typeName"));
  }

  public void setMessage(String message) {
    data.setMessage(
        truncateTelemetry(
            message, SanitizationHelper.MAX_MESSAGE_LENGTH, "ExceptionDetail.message"));
  }

  public void setHasFullStack(Boolean hasFullStack) {
    data.setHasFullStack(hasFullStack);
  }

  public void setStack(String stack) {
    data.setStack(
        truncateTelemetry(stack, SanitizationHelper.MAX_MESSAGE_LENGTH, "ExceptionDetail.stack"));
  }

  public void setParsedStack(List<StackFrameBuilder> builders) {
    List<StackFrame> stackFrames = new ArrayList<>();
    for (StackFrameBuilder builder : builders) {
      stackFrames.add(builder.build());
    }
    data.setParsedStack(stackFrames);
  }

  // visible (beyond package protected) for testing
  public TelemetryExceptionDetails build() {
    return data;
  }
}
