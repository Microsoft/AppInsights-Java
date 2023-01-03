// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.google.auto.service.AutoService;
import com.microsoft.applicationinsights.agent.internal.httpclient.LazyHttpClient;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import javax.annotation.Nullable;

@AutoService(AgentListener.class)
public class AfterAgentListener implements AgentListener {

  @Nullable private static volatile AppIdSupplier appIdSupplier;

  public static void setAppIdSupplier(AppIdSupplier appIdSupplier) {
    AfterAgentListener.appIdSupplier = appIdSupplier;
  }

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    // only safe now to resolve app id because SSL initialization
    // triggers loading of java.util.logging (starting with Java 8u231)
    // and JBoss/Wildfly need to install their own JUL manager before JUL is initialized.

    if (appIdSupplier != null) {
      appIdSupplier.updateAppId();
    }

    LazyHttpClient.safeToInitLatch.countDown();

    PerformanceCounterInitializer.initialize(FirstEntryPoint.getConfiguration());
  }
}
