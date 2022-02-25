/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.aisdk;

public class AiAppId {

  // backwards propagation of appId
  public static final String RESPONSE_HEADER_NAME = "Request-Context";

  private static volatile Supplier supplier;

  static {
    String testingAppId = System.getProperty("ai.internal.testing.appId");
    if (testingAppId != null) {
      supplier = () -> testingAppId;
    }
  }

  public static void setSupplier(Supplier supplier) {
    AiAppId.supplier = supplier;
  }

  public static String getAppId() {
    return supplier == null ? "" : supplier.get();
  }

  public interface Supplier {
    String get();
  }
}
