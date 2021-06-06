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

package com.microsoft.applicationinsights.agent.internal.processors;

import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.NameConfig;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorConfig;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorType;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ToAttributeConfig;
import com.microsoft.applicationinsights.customExceptions.FriendlyException;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExporterWithLogProcessorTest {

    private final Tracer tracer = OpenTelemetrySdk.builder().build().getTracer("test");

    @Test(expected = FriendlyException.class)
    public void noBodyObjectTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.LOG;
        config.id = "noBodyObjectTest";
        SpanExporter exampleExporter = new ExporterWithLogProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("my trace")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("applicationinsights.internal.log", true)
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);
    }

    @Test(expected = FriendlyException.class)
    public void inValidConfigTestWithNoFromOrToAttributesTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.LOG;
        config.id = "inValidConfigTestWithToAttributesNoRules";
        config.body = new NameConfig();
        SpanExporter exampleExporter = new ExporterWithLogProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("logA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("applicationinsights.internal.log", true)
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);
    }

    @Test(expected = FriendlyException.class)
    public void inValidConfigTestWithToAttributesNoRulesTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.LOG;
        config.id = "inValidConfigTestWithToAttributesNoRules";
        config.body = new NameConfig();
        config.body.toAttributes = new ToAttributeConfig();
        SpanExporter exampleExporter = new ExporterWithLogProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("applicationinsights.internal.log", true)
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);
    }

    @Test
    public void SimpleRenameLogMessageTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.LOG;
        config.id = "SimpleRenameLogMessage";
        config.body = new NameConfig();
        config.body.fromAttributes = Arrays.asList("db.svc", "operation", "id");
        SpanExporter exampleExporter = new ExporterWithLogProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("logA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("db.svc", "location")
                .setAttribute("operation", "get")
                .setAttribute("id", "1234")
                .setAttribute("applicationinsights.internal.log", true)
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);

        // verify that resulting logs are filtered in the way we want
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertEquals("locationget1234", resultSpan.getName());

    }

    @Test
    public void SimpleRenameLogWithSeparatorTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.LOG;
        config.id = "SimpleRenameLogWithSeparator";
        config.body = new NameConfig();
        config.body.fromAttributes = Arrays.asList("db.svc", "operation", "id");
        config.body.separator = "::";
        SpanExporter exampleExporter = new ExporterWithLogProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("db.svc", "location")
                .setAttribute("operation", "get")
                .setAttribute("id", "1234")
                .setAttribute("applicationinsights.internal.log", true)
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);

        // verify that resulting logs are filtered in the way we want
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertEquals("location::get::1234", resultSpan.getName());

    }

    @Test
    public void SimpleRenameLogWithMissingKeysTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.LOG;
        config.id = "SimpleRenameLogWithMissingKeys";
        config.body = new NameConfig();
        config.body.fromAttributes = Arrays.asList("db.svc", "operation", "id");
        config.body.separator = "::";
        SpanExporter exampleExporter = new ExporterWithLogProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("db.svc", "location")
                .setAttribute("operation", "get")
                .setAttribute("applicationinsights.internal.log", true)
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);

        // verify that resulting logs are filtered in the way we want
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertEquals("svcA", resultSpan.getName());

    }

    @Test(expected = FriendlyException.class)
    public void InvalidRegexInRulesTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.LOG;
        config.id = "InvalidRegexInRules";
        config.body = new NameConfig();
        ToAttributeConfig toAttributeConfig = new ToAttributeConfig();
        toAttributeConfig.rules = new ArrayList<>();
        toAttributeConfig.rules.add("***");
        config.body.toAttributes = toAttributeConfig;
        SpanExporter exampleExporter = new ExporterWithLogProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("/api/v1/document/12345678/update")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("db.svc", "location")
                .setAttribute("operation", "get")
                .setAttribute("id", "1234")
                .setAttribute("applicationinsights.internal.log", true)
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);

        // verify that resulting spans are filtered in the way we want
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertNotNull(Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("documentId"))));
        assertEquals("12345678", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("documentId"))));
        assertEquals("/api/v1/document/{documentId}/update", resultSpan.getName());
    }

    @Test
    public void SimpleToAttributesTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.LOG;
        config.id = "SimpleToAttributes";
        config.body = new NameConfig();
        ToAttributeConfig toAttributeConfig = new ToAttributeConfig();
        toAttributeConfig.rules = new ArrayList<>();
        toAttributeConfig.rules.add("^/api/v1/document/(?<documentId>.*)/update$");
        config.body.toAttributes = toAttributeConfig;
        SpanExporter exampleExporter = new ExporterWithLogProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("/api/v1/document/12345678/update")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("db.svc", "location")
                .setAttribute("operation", "get")
                .setAttribute("id", "1234")
                .setAttribute("applicationinsights.internal.log", true)
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);

        // verify that resulting logs are filtered in the way we want
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertNotNull(Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("documentId"))));
        assertEquals("12345678", Objects.requireNonNull(resultSpan.getAttributes().get(AttributeKey.stringKey("documentId"))));
        assertEquals("/api/v1/document/{documentId}/update", resultSpan.getName());
    }

    @Test
    public void MultiRuleToAttributesTest() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.LOG;
        config.id = "MultiRuleToAttributes";
        config.body = new NameConfig();
        ToAttributeConfig toAttributeConfig = new ToAttributeConfig();
        toAttributeConfig.rules = new ArrayList<>();
        toAttributeConfig.rules.add("Password=(?<password1>[^ ]+)");
        toAttributeConfig.rules.add("Pass=(?<password2>[^ ]+)");
        config.body.toAttributes = toAttributeConfig;
        SpanExporter exampleExporter = new ExporterWithLogProcessor(config, mockExporter);

        Span spanA = tracer.spanBuilder("yyyPassword=123 aba Pass=555 xyx Pass=777 zzz")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("db.svc", "location")
                .setAttribute("operation", "get")
                .setAttribute("id", "1234")
                .setAttribute("applicationinsights.internal.log", true)
                .startSpan();
        Span spanB = tracer.spanBuilder("yyyPassword=**** aba")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("db.svc", "location")
                .setAttribute("operation", "get")
                .setAttribute("id", "1234")
                .setAttribute("password","234")
                .setAttribute("applicationinsights.internal.log", true)
                .startSpan();

        SpanData spanAData = ((ReadableSpan) spanA).toSpanData();
        SpanData spanBData = ((ReadableSpan) spanB).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanAData);
        spans.add(spanBData);
        exampleExporter.export(spans);

        // verify that resulting spans are filtered in the way we want
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpanA = result.get(0);
        SpanData resultSpanB = result.get(1);
        assertNotNull(Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("password1"))));
        assertEquals("123", Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("password1"))));
        assertNotNull(Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("password2"))));
        assertEquals("555", Objects.requireNonNull(resultSpanA.getAttributes().get(AttributeKey.stringKey("password2"))));
        assertEquals("yyyPassword={password1} aba Pass={password2} xyx Pass=777 zzz", resultSpanA.getName());
        assertNotNull(Objects.requireNonNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("password1"))));
        assertEquals("****", Objects.requireNonNull(resultSpanB.getAttributes().get(AttributeKey.stringKey("password1"))));
        assertEquals("yyyPassword={password1} aba", resultSpanB.getName());
    }

    @Test
    public void SimpleRenameLogTestWithSpanProcessor() {
        MockExporter mockExporter = new MockExporter();
        ProcessorConfig config = new ProcessorConfig();
        config.type = ProcessorType.LOG;
        config.id = "SimpleRenameSpan";
        config.body = new NameConfig();
        config.body.fromAttributes = Arrays.asList("db.svc", "operation", "id");
        SpanExporter exampleExporter = new ExporterWithLogProcessor(config, mockExporter);

        Span span = tracer.spanBuilder("svcA")
                .setAttribute("one", "1")
                .setAttribute("two", 2L)
                .setAttribute("db.svc", "location")
                .setAttribute("operation", "get")
                .setAttribute("id", "1234")
                .startSpan();

        SpanData spanData = ((ReadableSpan) span).toSpanData();

        List<SpanData> spans = new ArrayList<>();
        spans.add(spanData);
        exampleExporter.export(spans);

        // verify that resulting logs are not modified
        List<SpanData> result = mockExporter.getSpans();
        SpanData resultSpan = result.get(0);
        assertEquals("svcA", resultSpan.getName());

    }
}
