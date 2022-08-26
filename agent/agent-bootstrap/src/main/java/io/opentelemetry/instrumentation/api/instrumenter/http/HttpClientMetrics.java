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

package io.opentelemetry.instrumentation.api.instrumenter.http;

import static io.opentelemetry.instrumentation.api.instrumenter.BootstrapSemanticAttributes.IS_PRE_AGGREGATED;
import static io.opentelemetry.instrumentation.api.instrumenter.BootstrapSemanticAttributes.IS_SYNTHETIC;
import static java.util.logging.Level.FINE;

import com.google.auto.value.AutoValue;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.instrumentation.api.instrumenter.OperationListener;
import io.opentelemetry.instrumentation.api.instrumenter.OperationMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.UserAgents;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * {@link OperationListener} which keeps track of <a
 * href="https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/metrics/semantic_conventions/http-metrics.md#http-client">HTTP
 * client metrics</a>.
 */
public final class HttpClientMetrics implements OperationListener {

  private static final double NANOS_PER_MS = TimeUnit.MILLISECONDS.toNanos(1);

  private static final ContextKey<State> HTTP_CLIENT_REQUEST_METRICS_STATE =
      ContextKey.named("http-client-request-metrics-state");

  private static final Logger logger = Logger.getLogger(HttpClientMetrics.class.getName());

  /**
   * Returns a {@link OperationMetrics} which can be used to enable recording of {@link
   * HttpClientMetrics} on an {@link
   * io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder}.
   */
  public static OperationMetrics get() {
    return HttpClientMetrics::new;
  }

  private final DoubleHistogram duration;
  private final LongHistogram requestSize;
  private final LongHistogram responseSize;

  private HttpClientMetrics(Meter meter) {
    duration =
        meter
            .histogramBuilder("http.client.duration")
            .setUnit("ms")
            .setDescription("The duration of the outbound HTTP request")
            .build();
    requestSize =
        meter
            .histogramBuilder("http.client.request.size")
            .setUnit("By")
            .setDescription("The size of HTTP request messages")
            .ofLongs()
            .build();
    responseSize =
        meter
            .histogramBuilder("http.client.response.size")
            .setUnit("By")
            .setDescription("The size of HTTP response messages")
            .ofLongs()
            .build();
  }

  @Override
  public Context onStart(Context context, Attributes startAttributes, long startNanos) {
    return context.with(
        HTTP_CLIENT_REQUEST_METRICS_STATE,
        new AutoValue_HttpClientMetrics_State(startAttributes, startNanos));
  }

  @Override
  public void onEnd(Context context, Attributes endAttributes, long endNanos) {
    State state = context.get(HTTP_CLIENT_REQUEST_METRICS_STATE);
    if (state == null) {
      logger.log(
          FINE,
          "No state present when ending context {0}. Cannot record HTTP request metrics.",
          context);
      return;
    }
    Attributes durationAndSizeAttributes =
        TemporaryMetricsView.applyClientDurationAndSizeView(state.startAttributes(), endAttributes);

    // START APPLICATION INSIGHTS CODE

    // this is needed for detecting telemetry signals that will trigger pre-aggregated metrics via
    // auto instrumentations
    Span.fromContext(context).setAttribute(IS_PRE_AGGREGATED, true);

    Attributes durationAttributes =
        durationAndSizeAttributes.toBuilder()
            .put(IS_SYNTHETIC, UserAgents.isBot(endAttributes, state.startAttributes()))
            .build();

    // END APPLICATION INSIGHTS CODE

    this.duration.record(
        (endNanos - state.startTimeNanos()) / NANOS_PER_MS, durationAttributes, context);

    Long requestLength =
        getAttribute(
            SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH, endAttributes, state.startAttributes());
    if (requestLength != null) {
      requestSize.record(requestLength, durationAndSizeAttributes);
    }
    Long responseLength =
        getAttribute(
            SemanticAttributes.HTTP_RESPONSE_CONTENT_LENGTH,
            endAttributes,
            state.startAttributes());
    if (responseLength != null) {
      responseSize.record(responseLength, durationAndSizeAttributes);
    }
  }

  @Nullable
  private static <T> T getAttribute(AttributeKey<T> key, Attributes... attributesList) {
    for (Attributes attributes : attributesList) {
      T value = attributes.get(key);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  @AutoValue
  abstract static class State {

    abstract Attributes startAttributes();

    abstract long startTimeNanos();
  }
}
