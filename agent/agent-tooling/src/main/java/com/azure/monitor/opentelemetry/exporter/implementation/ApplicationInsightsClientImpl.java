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

package com.azure.monitor.opentelemetry.exporter.implementation;

import com.azure.core.annotation.BodyParam;
import com.azure.core.annotation.ExpectedResponses;
import com.azure.core.annotation.HeaderParam;
import com.azure.core.annotation.Host;
import com.azure.core.annotation.HostParam;
import com.azure.core.annotation.Post;
import com.azure.core.annotation.ReturnType;
import com.azure.core.annotation.ServiceInterface;
import com.azure.core.annotation.ServiceMethod;
import com.azure.core.annotation.UnexpectedResponseExceptionType;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.policy.CookiePolicy;
import com.azure.core.http.policy.RetryPolicy;
import com.azure.core.http.policy.UserAgentPolicy;
import com.azure.core.http.rest.Response;
import com.azure.core.http.rest.RestProxy;
import com.azure.core.util.Context;
import com.azure.core.util.FluxUtil;
import com.azure.core.util.serializer.JacksonAdapter;
import com.azure.core.util.serializer.SerializerAdapter;
import com.azure.monitor.opentelemetry.exporter.implementation.models.ExportResult;
import com.azure.monitor.opentelemetry.exporter.implementation.models.ExportResultException;
import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryItem;
import java.util.List;
import reactor.core.publisher.Mono;

/** Initializes a new instance of the ApplicationInsightsClient type. */
public final class ApplicationInsightsClientImpl {
  /** The proxy service used to perform REST calls. */
  private final ApplicationInsightsClientService service;

  /** Breeze endpoint: https://dc.services.visualstudio.com. */
  private final String host;

  /**
   * Gets Breeze endpoint: https://dc.services.visualstudio.com.
   *
   * @return the host value.
   */
  public String getHost() {
    return this.host;
  }

  /** The HTTP pipeline to send requests through. */
  private final HttpPipeline httpPipeline;

  /**
   * Gets The HTTP pipeline to send requests through.
   *
   * @return the httpPipeline value.
   */
  public HttpPipeline getHttpPipeline() {
    return this.httpPipeline;
  }

  /** The serializer to serialize an object into a string. */
  private final SerializerAdapter serializerAdapter;

  /**
   * Gets The serializer to serialize an object into a string.
   *
   * @return the serializerAdapter value.
   */
  public SerializerAdapter getSerializerAdapter() {
    return this.serializerAdapter;
  }

  /**
   * Initializes an instance of ApplicationInsightsClient client.
   *
   * @param host Breeze endpoint: https://dc.services.visualstudio.com.
   */
  ApplicationInsightsClientImpl(String host) {
    this(
        new HttpPipelineBuilder()
            .policies(new UserAgentPolicy(), new RetryPolicy(), new CookiePolicy())
            .build(),
        JacksonAdapter.createDefaultSerializerAdapter(),
        host);
  }

  /**
   * Initializes an instance of ApplicationInsightsClient client.
   *
   * @param httpPipeline The HTTP pipeline to send requests through.
   * @param host Breeze endpoint: https://dc.services.visualstudio.com.
   */
  ApplicationInsightsClientImpl(HttpPipeline httpPipeline, String host) {
    this(httpPipeline, JacksonAdapter.createDefaultSerializerAdapter(), host);
  }

  /**
   * Initializes an instance of ApplicationInsightsClient client.
   *
   * @param httpPipeline The HTTP pipeline to send requests through.
   * @param serializerAdapter The serializer to serialize an object into a string.
   * @param host Breeze endpoint: https://dc.services.visualstudio.com.
   */
  ApplicationInsightsClientImpl(
      HttpPipeline httpPipeline, SerializerAdapter serializerAdapter, String host) {
    this.httpPipeline = httpPipeline;
    this.serializerAdapter = serializerAdapter;
    this.host = host;
    this.service =
        RestProxy.create(
            ApplicationInsightsClientService.class, this.httpPipeline, this.getSerializerAdapter());
  }

  /**
   * The interface defining all the services for ApplicationInsightsClient to be used by the proxy
   * service to perform REST calls.
   */
  @Host("{Host}/v2.1")
  @ServiceInterface(name = "ApplicationInsightsC")
  private interface ApplicationInsightsClientService {
    @Post("/track")
    @ExpectedResponses({200, 206})
    @UnexpectedResponseExceptionType(
        value = ExportResultException.class,
        code = {400, 402, 429, 500, 503})
    @UnexpectedResponseExceptionType(ExportResultException.class)
    Mono<Response<ExportResult>> track(
        @HostParam("Host") String host,
        @BodyParam("application/json") List<TelemetryItem> body,
        @HeaderParam("Accept") String accept,
        Context context);
  }

  /**
   * This operation sends a sequence of telemetry events that will be monitored by Azure Monitor.
   *
   * @param body The list of telemetry events to track.
   * @throws IllegalArgumentException thrown if parameters fail the validation.
   * @throws ExportResultException thrown if the request is rejected by server.
   * @throws ExportResultException thrown if the request is rejected by server on status code 400,
   *     402, 429, 500, 503.
   * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
   * @return response containing the status of each telemetry item.
   */
  @ServiceMethod(returns = ReturnType.SINGLE)
  public Mono<Response<ExportResult>> trackWithResponseAsync(List<TelemetryItem> body) {
    final String accept = "application/json";
    return FluxUtil.withContext(context -> service.track(this.getHost(), body, accept, context));
  }

  /**
   * This operation sends a sequence of telemetry events that will be monitored by Azure Monitor.
   *
   * @param body The list of telemetry events to track.
   * @param context The context to associate with this operation.
   * @throws IllegalArgumentException thrown if parameters fail the validation.
   * @throws ExportResultException thrown if the request is rejected by server.
   * @throws ExportResultException thrown if the request is rejected by server on status code 400,
   *     402, 429, 500, 503.
   * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
   * @return response containing the status of each telemetry item.
   */
  @ServiceMethod(returns = ReturnType.SINGLE)
  public Mono<Response<ExportResult>> trackWithResponseAsync(
      List<TelemetryItem> body, Context context) {
    final String accept = "application/json";
    return service.track(this.getHost(), body, accept, context);
  }

  /**
   * This operation sends a sequence of telemetry events that will be monitored by Azure Monitor.
   *
   * @param body The list of telemetry events to track.
   * @throws IllegalArgumentException thrown if parameters fail the validation.
   * @throws ExportResultException thrown if the request is rejected by server.
   * @throws ExportResultException thrown if the request is rejected by server on status code 400,
   *     402, 429, 500, 503.
   * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
   * @return response containing the status of each telemetry item.
   */
  @ServiceMethod(returns = ReturnType.SINGLE)
  public Mono<ExportResult> trackAsync(List<TelemetryItem> body) {
    return trackWithResponseAsync(body)
        .flatMap(
            (Response<ExportResult> res) -> {
              if (res.getValue() != null) {
                return Mono.just(res.getValue());
              } else {
                return Mono.empty();
              }
            });
  }

  /**
   * This operation sends a sequence of telemetry events that will be monitored by Azure Monitor.
   *
   * @param body The list of telemetry events to track.
   * @param context The context to associate with this operation.
   * @throws IllegalArgumentException thrown if parameters fail the validation.
   * @throws ExportResultException thrown if the request is rejected by server.
   * @throws ExportResultException thrown if the request is rejected by server on status code 400,
   *     402, 429, 500, 503.
   * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent.
   * @return response containing the status of each telemetry item.
   */
  @ServiceMethod(returns = ReturnType.SINGLE)
  public Mono<ExportResult> trackAsync(List<TelemetryItem> body, Context context) {
    return trackWithResponseAsync(body, context)
        .flatMap(
            (Response<ExportResult> res) -> {
              if (res.getValue() != null) {
                return Mono.just(res.getValue());
              } else {
                return Mono.empty();
              }
            });
  }
}
