package com.microsoft.applicationinsights.agent.internal.wasbootstrap;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.agent.Exporter;
import com.microsoft.applicationinsights.agent.internal.Global;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithAttributeProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithLogProcessor;
import com.microsoft.applicationinsights.agent.internal.processors.ExporterWithSpanProcessor;
import com.microsoft.applicationinsights.agent.internal.propagator.DelegatingPropagator;
import com.microsoft.applicationinsights.agent.internal.sampling.DelegatingSampler;
import com.microsoft.applicationinsights.agent.internal.sampling.Samplers;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorConfig;
import io.opentelemetry.sdk.autoconfigure.spi.SdkTracerProviderConfigurer;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OpenTelemetryConfigurer implements SdkTracerProviderConfigurer {

    @Override
    public void configure(SdkTracerProviderBuilder tracerProvider) {
        TelemetryClient telemetryClient = Global.getTelemetryClient();
        if (telemetryClient == null) {
            // agent failed during startup
            return;
        }

        Configuration config = MainEntryPoint.getConfiguration();

        tracerProvider.setSampler(DelegatingSampler.getInstance());

        if (config.connectionString != null) {
            DelegatingPropagator.getInstance().setUpStandardDelegate();
            DelegatingSampler.getInstance().setDelegate(Samplers.getSampler(config.sampling.percentage, config));
        } else {
            // in Azure Functions, we configure later on, once we know user has opted in to tracing
            // (note: the default for DelegatingPropagator is to not propagate anything
            // and the default for DelegatingSampler is to not sample anything)
        }

        List<ProcessorConfig> processors = config.preview.processors.stream()
                .filter(processor -> processor.type != Configuration.ProcessorType.METRIC_FILTER)
                .collect(Collectors.toCollection(ArrayList::new));
        // Reversing the order of processors before passing it to SpanProcessor
        Collections.reverse(processors);

        // NOTE if changing the span processor to something async, flush it in the shutdown hook before flushing TelemetryClient
        if (!processors.isEmpty()) {
            SpanExporter currExporter = new Exporter(telemetryClient);
            for (ProcessorConfig processorConfig : processors) {
                if (processorConfig.type != null) { // Added this condition to resolve spotbugs NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD issue
                    switch (processorConfig.type) {
                        case ATTRIBUTE:
                            currExporter = new ExporterWithAttributeProcessor(processorConfig, currExporter);
                            break;
                        case SPAN:
                            currExporter = new ExporterWithSpanProcessor(processorConfig, currExporter);
                            break;
                        case LOG:
                            currExporter = new ExporterWithLogProcessor(processorConfig, currExporter);
                            break;
                        default:
                            throw new IllegalStateException("Not an expected ProcessorType: " + processorConfig.type);
                    }
                }
            }

            tracerProvider.addSpanProcessor(SimpleSpanProcessor.create(currExporter));

        } else {
            tracerProvider.addSpanProcessor(SimpleSpanProcessor.create(new Exporter(telemetryClient)));
        }
    }
}
