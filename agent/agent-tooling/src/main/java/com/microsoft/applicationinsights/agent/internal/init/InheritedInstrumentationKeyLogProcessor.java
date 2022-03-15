package com.microsoft.applicationinsights.agent.internal.init;

import com.microsoft.applicationinsights.agent.internal.processors.MyLogData;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.LogProcessor;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.trace.ReadableSpan;

public class InheritedInstrumentationKeyLogProcessor implements LogProcessor {

  private static final AttributeKey<String> INSTRUMENTATION_KEY_KEY =
      AttributeKey.stringKey("ai.preview.instrumentation_key");

  private final LogProcessor delegate;

  public InheritedInstrumentationKeyLogProcessor(LogProcessor delegate) {
    this.delegate = delegate;
  }

  @Override
  public void emit(LogData log) {
    Span currentSpan = Span.current();
    if (!(currentSpan instanceof ReadableSpan)) {
      return;
    }

    ReadableSpan readableSpan = (ReadableSpan) currentSpan;
    String instrumentationKey = readableSpan.getAttribute(INSTRUMENTATION_KEY_KEY);
    if (instrumentationKey != null) {
      log = new MyLogData(
          log,
          log.getAttributes().toBuilder()
              .put(INSTRUMENTATION_KEY_KEY, instrumentationKey)
              .build());
    }

    delegate.emit(log);
  }

  @Override
  public CompletableResultCode shutdown() {
    return delegate.shutdown();
  }

  @Override
  public CompletableResultCode forceFlush() {
    return delegate.forceFlush();
  }

  @Override
  public void close() {
    delegate.close();
  }
}
