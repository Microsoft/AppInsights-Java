// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import com.microsoft.applicationinsights.agent.internal.profiler.upload.UploadCompleteHandler;
import com.microsoft.applicationinsights.agent.internal.profiler.upload.UploadResult;
import com.microsoft.applicationinsights.agent.internal.profiler.upload.UploadService;
import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import java.io.File;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Receives notifications of new profiles and uploads them to Service Profiler. */
class JfrUploadService {

  private static final Logger LOGGER = LoggerFactory.getLogger(JfrUploadService.class);

  private final UploadService jfrUploader;
  private final Supplier<String> appIdSupplier;

  JfrUploadService(UploadService jfrUploader, Supplier<String> appIdSupplier) {
    this.jfrUploader = jfrUploader;
    this.appIdSupplier = appIdSupplier;
  }

  void upload(
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
            "JFR-" + alertBreach.getType().name(),
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
