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

package com.microsoft.applicationinsights.serviceprofilerapi.profiler;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.profiler.ProfileHandler;
import com.microsoft.applicationinsights.profiler.uploader.UploadCompleteHandler;
import com.microsoft.applicationinsights.profiler.uploader.UploadResult;
import com.microsoft.applicationinsights.serviceprofilerapi.upload.ServiceProfilerUploader;
import java.io.File;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Receives notifications of new profiles and uploads them to Service Profiler. */
public class JfrUploadService implements ProfileHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(JfrUploadService.class);

  private final ServiceProfilerUploader jfrUploader;
  private final Supplier<String> appIdSupplier;

  public JfrUploadService(ServiceProfilerUploader jfrUploader, Supplier<String> appIdSupplier) {
    this.jfrUploader = jfrUploader;
    this.appIdSupplier = appIdSupplier;
  }

  @Override
  public void receive(
      AlertBreach alertBreach,
      long timestamp,
      File file,
      UploadCompleteHandler uploadCompleteHandler) {
    String appId = appIdSupplier.get();
    if (appId == null || appId.isEmpty()) {
      LOGGER.error("Not uploading file due to lack of app id");
      return;
    }

    jfrUploader
        .uploadJfrFile(
            UUID.fromString(alertBreach.getProfileId()),
            alertBreach.getTriggerName(),
            timestamp,
            file,
            alertBreach.getCpuMetric(),
            alertBreach.getMemoryUsage())
        .subscribe(
            onUploadComplete(uploadCompleteHandler), e -> LOGGER.error("Failed to upload file", e));
  }

  // Notify listener that full profile and upload cycle has completed and log success
  private static Consumer<? super UploadResult> onUploadComplete(
      UploadCompleteHandler uploadCompleteHandler) {
    return result -> {
      uploadCompleteHandler.notify(result);
      LOGGER.info("Uploading of profile complete");
    };
  }
}
