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

package com.microsoft.applicationinsights.agent.internal.init;

import com.azure.monitor.opentelemetry.exporter.AiOperationNameSpanProcessor;
import com.google.auto.service.AutoService;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.internal.exporter.AzureMonitorMetricExporter;
import com.microsoft.applicationinsights.agent.internal.exporter.Exporter;
import com.microsoft.applicationinsights.agent.internal.exporter.LoggerExporter;
import com.microsoft.applicationinsights.agent.internal.legacyheaders.AiLegacyHeaderSpanProcessor;
import com.microsoft.applicationinsights.agent.internal.legacyheaders.DelegatingPropagator;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithLogProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithSpanProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.LogExporterWithAttributeProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.MySpanData;
import com.microsoft.applicationinsights.agent.internal.processors.SpanExporterWithAttributeProcessor;
import com.microsoft.applicationinsights.agent.internal.sampling.DelegatingSampler;
import com.microsoft.applicationinsights.agent.internal.sampling.Samplers;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLogEmitterProviderBuilder;
import io.opentelemetry.sdk.logs.export.BatchLogProcessor;
import io.opentelemetry.sdk.logs.export.LogExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@AutoService(AutoConfigurationCustomizerProvider.class)
public class OpenTelemetryConfigurer implements AutoConfigurationCustomizerProvider {

  @Override
  public void customize(AutoConfigurationCustomizer autoConfiguration) {
    TelemetryClient telemetryClient = TelemetryClient.getActive();
    if (telemetryClient == null) {
      // agent failed during startup
      return;
    }

    Configuration configuration = MainEntryPoint.getConfiguration();

    autoConfiguration
        .addTracerProviderCustomizer(
            (builder, config) -> configureTracing(builder, telemetryClient, config, configuration))
        .addLogEmitterProviderCustomizer(
            (builder, config) -> configureLogging(builder, telemetryClient, configuration))
        .addMeterProviderCustomizer(
            (builder, config) -> configureMetrics(builder, telemetryClient, configuration));
  }

  @SuppressFBWarnings(
      value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD",
      justification = "this method is only called once during initialization")
  private static SdkTracerProviderBuilder configureTracing(
      SdkTracerProviderBuilder tracerProvider,
      TelemetryClient telemetryClient,
      ConfigProperties config,
      Configuration configuration) {

    tracerProvider.setSampler(DelegatingSampler.getInstance());

    if (configuration.connectionString != null) {
      if (!configuration.preview.disablePropagation) {
        DelegatingPropagator.getInstance()
            .setUpStandardDelegate(
                configuration.preview.additionalPropagators,
                configuration.preview.legacyRequestIdPropagation.enabled);
      }
      DelegatingSampler.getInstance()
          .setDelegate(Samplers.getSampler(configuration.sampling.percentage, configuration));
    } else {
      // in Azure Functions, we configure later on, once we know user has opted in to tracing
      // (note: the default for DelegatingPropagator is to not propagate anything
      // and the default for DelegatingSampler is to not sample anything)
    }

    // operation name span processor is only applied on span start, so doesn't need to be chained
    // with the batch span processor
    tracerProvider.addSpanProcessor(new AiOperationNameSpanProcessor());
    // inherited attributes span processor is only applied on span start, so doesn't need to be
    // chained with the batch span processor
    if (!configuration.preview.inheritedAttributes.isEmpty()) {
      tracerProvider.addSpanProcessor(
          new InheritedAttributesSpanProcessor(configuration.preview.inheritedAttributes));
    }
    // legacy span processor is only applied on span start, so doesn't need to be chained with the
    // batch span processor
    // it is used to pass legacy attributes from the context (extracted by the AiLegacyPropagator)
    // to the span attributes (since there is no way to update attributes on span directly from
    // propagator)
    if (configuration.preview.legacyRequestIdPropagation.enabled) {
      tracerProvider.addSpanProcessor(new AiLegacyHeaderSpanProcessor());
    }
    // instrumentation key overrides span processor is only applied on span start, so doesn't need
    // to be chained with the batch span processor
    if (!configuration.preview.instrumentationKeyOverrides.isEmpty()) {
      tracerProvider.addSpanProcessor(
          new InheritedInstrumentationKeySpanProcessor(
              configuration.preview.instrumentationKeyOverrides));
    }
    // role name overrides span processor is only applied on span start, so doesn't need
    // to be chained with the batch span processor
    if (!configuration.preview.roleNameOverrides.isEmpty()) {
      tracerProvider.addSpanProcessor(
          new InheritedRoleNameSpanProcessor(configuration.preview.roleNameOverrides));
    }

    String tracesExporter = config.getString("otel.traces.exporter");
    if ("none".equals(tracesExporter)) { // "none" is the default set in ConfigOverride
      SpanExporter spanExporter =
          createSpanExporter(
              telemetryClient, configuration, configuration.preview.captureHttpServer4xxAsError);

      // using BatchSpanProcessor in order to get off of the application thread as soon as possible
      BatchSpanProcessor batchSpanProcessor =
          BatchSpanProcessor.builder(spanExporter)
              .setScheduleDelay(getBatchProcessorDelay())
              .build();

      tracerProvider.addSpanProcessor(
          new TelemetryClientFlushingSpanProcessor(batchSpanProcessor, telemetryClient));
    }

    return tracerProvider;
  }

  private static SpanExporter createSpanExporter(
      TelemetryClient telemetryClient,
      Configuration configuration,
      boolean captureHttpServer4xxAsError) {

    SpanExporter spanExporter = new Exporter(telemetryClient, captureHttpServer4xxAsError);
    List<ProcessorConfig> processorConfigs = getSpanProcessorConfigs(configuration);
    // NOTE if changing the span processor to something async, flush it in the shutdown hook before
    // flushing TelemetryClient
    if (!processorConfigs.isEmpty()) {
      // Reversing the order of processors before passing it Span processor
      Collections.reverse(processorConfigs);
      for (ProcessorConfig processorConfig : processorConfigs) {
        switch (processorConfig.type) {
          case ATTRIBUTE:
            spanExporter = new SpanExporterWithAttributeProcessor(processorConfig, spanExporter);
            break;
          case SPAN:
            spanExporter = new ExporterWithSpanProcessor(processorConfig, spanExporter);
            break;
          default:
            throw new IllegalStateException(
                "Not an expected ProcessorType: " + processorConfig.type);
        }
      }

      // this is temporary until semantic attributes stabilize and we make breaking change
      // then can use java.util.functions.Predicate<Attributes>
      spanExporter = new BackCompatHttpUrlProcessor(spanExporter);
    }

    return spanExporter;
  }

  private static List<ProcessorConfig> getSpanProcessorConfigs(Configuration configuration) {
    return configuration.preview.processors.stream()
        .filter(
            processor ->
                processor.type == Configuration.ProcessorType.ATTRIBUTE
                    || processor.type == Configuration.ProcessorType.SPAN)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private static SdkLogEmitterProviderBuilder configureLogging(
      SdkLogEmitterProviderBuilder builder,
      TelemetryClient telemetryClient,
      Configuration configuration) {

    LogExporter logExporter = createLogExporter(telemetryClient, configuration);

    // using BatchLogProcessor in order to get off of the application thread as soon as possible
    BatchLogProcessor batchLogProcessor =
        BatchLogProcessor.builder(logExporter).setScheduleDelay(getBatchProcessorDelay()).build();

    TelemetryClientFlushingLogProcessor telemetryClientFlushingLogProcessor =
        new TelemetryClientFlushingLogProcessor(batchLogProcessor, telemetryClient);

    // inherited attributes log processor also handles operation name, ikey and role name attributes
    // and these all need access to Span.current(), so must be run before passing off to the
    // BatchLogProcessor
    return builder.addLogProcessor(
        new InheritedAttributesLogProcessor(
            configuration.preview.inheritedAttributes, telemetryClientFlushingLogProcessor));
  }

  private static LogExporter createLogExporter(
      TelemetryClient telemetryClient, Configuration configuration) {
    LogExporter logExporter =
        new LoggerExporter(
            telemetryClient, configuration.preview.captureLoggingLevelAsCustomDimension);
    List<ProcessorConfig> processorConfigs = getLogProcessorConfigs(configuration);
    if (!processorConfigs.isEmpty()) {
      // Reversing the order of processors before passing it Log processor
      Collections.reverse(processorConfigs);
      for (ProcessorConfig processorConfig : processorConfigs) {
        switch (processorConfig.type) {
          case ATTRIBUTE:
            logExporter = new LogExporterWithAttributeProcessor(processorConfig, logExporter);
            break;
          case LOG:
            logExporter = new ExporterWithLogProcessor(processorConfig, logExporter);
            break;
          default:
            throw new IllegalStateException(
                "Not an expected ProcessorType: " + processorConfig.type);
        }
      }
    }
    return logExporter;
  }

  private static List<ProcessorConfig> getLogProcessorConfigs(Configuration configuration) {
    return configuration.preview.processors.stream()
        .filter(
            processor ->
                processor.type == Configuration.ProcessorType.ATTRIBUTE
                    || processor.type == Configuration.ProcessorType.LOG)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private static Duration getBatchProcessorDelay() {
    String delayMillisStr = System.getenv("APPLICATIONINSIGHTS_PREVIEW_BSP_SCHEDULE_DELAY");
    if (delayMillisStr != null) {
      return Duration.ofMillis(Integer.parseInt(delayMillisStr));
    } else {
      // using small interval because need to convert to TelemetryItem as soon as possible to grab
      // data for live metrics. the real batching is done at a lower level
      // (not using batch size 1 because that seems to cause poor performance on small containers)
      return Duration.ofMillis(100);
    }
  }

  private static SdkMeterProviderBuilder configureMetrics(
      SdkMeterProviderBuilder builder,
      TelemetryClient telemetryClient,
      Configuration configuration) {
    return builder.registerMetricReader(
        PeriodicMetricReader.builder(new AzureMonitorMetricExporter(telemetryClient))
            .setInterval(Duration.ofSeconds(configuration.preview.metricIntervalSeconds))
            .build());
  }

  private static class BackCompatHttpUrlProcessor implements SpanExporter {

    private final SpanExporter delegate;

    private BackCompatHttpUrlProcessor(SpanExporter delegate) {
      this.delegate = delegate;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
      List<SpanData> copy = new ArrayList<>();
      for (SpanData span : spans) {
        copy.add(addBackCompatHttpUrl(span));
      }
      return delegate.export(copy);
    }

    private static SpanData addBackCompatHttpUrl(SpanData span) {
      Attributes attributes = span.getAttributes();
      if (attributes.get(SemanticAttributes.HTTP_URL) != null) {
        // already has http.url
        return span;
      }
      String httpUrl = Exporter.getHttpUrlFromServerSpan(attributes);
      if (httpUrl == null) {
        return span;
      }
      AttributesBuilder builder = attributes.toBuilder();
      builder.put(SemanticAttributes.HTTP_URL, httpUrl);
      return new MySpanData(span, builder.build());
    }

    @Override
    public CompletableResultCode flush() {
      return delegate.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
      return delegate.shutdown();
    }
  }
}
