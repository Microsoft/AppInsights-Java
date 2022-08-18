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

package com.azure.monitor.opentelemetry.exporter.implementation.preaggregatedmetrics;

import com.azure.monitor.opentelemetry.exporter.implementation.builders.AbstractTelemetryBuilder;
import com.azure.monitor.opentelemetry.exporter.implementation.models.ContextTagKeys;

public abstract class BaseExtractor {

  // visible for testing
  public static final String MS_METRIC_ID = "_MS.metricId";
  public static final String MS_IS_AUTOCOLLECTED = "_MS.IsAutocollected";
  public static final String MS_PROCESSED_BY_METRIC_EXTRACTORS = "_MS.ProcessedByMetricExtractors";
  public static final String TRUE = "True";
  public static final String FALSE = "False";
  public static final String OPERATION_SYNTHETIC = "operation/synthetic";
  public static final String CLOUD_ROLE_NAME = "cloud/roleName";
  public static final String CLOUD_ROLE_INSTANCE = "cloud/roleInstance";

  protected final AbstractTelemetryBuilder telemetryBuilder;

  public BaseExtractor(AbstractTelemetryBuilder telemetryBuilder, boolean isSynthetic) {
    this.telemetryBuilder = telemetryBuilder;
    extractCommon(isSynthetic);
  }

  private void extractCommon(boolean isSynthetic) {
    telemetryBuilder.addProperty(MS_IS_AUTOCOLLECTED, TRUE);
    // this flag will inform the ingestion service to stop post-aggregation
    telemetryBuilder.addProperty(MS_PROCESSED_BY_METRIC_EXTRACTORS, TRUE);

    if (telemetryBuilder.build().getTags() != null) {
      String cloudName =
          telemetryBuilder.build().getTags().get(ContextTagKeys.AI_CLOUD_ROLE.toString());
      if (cloudName != null && !cloudName.isEmpty()) {
        telemetryBuilder.addProperty(CLOUD_ROLE_NAME, cloudName);
      }

      String cloudRoleInstance =
          telemetryBuilder.build().getTags().get(ContextTagKeys.AI_CLOUD_ROLE_INSTANCE.toString());
      if (cloudRoleInstance != null && !cloudRoleInstance.isEmpty()) {
        telemetryBuilder.addProperty(CLOUD_ROLE_INSTANCE, cloudRoleInstance);
      }
    }

    telemetryBuilder.addProperty(OPERATION_SYNTHETIC, isSynthetic ? TRUE : FALSE);
  }

  public abstract void extract();
}
