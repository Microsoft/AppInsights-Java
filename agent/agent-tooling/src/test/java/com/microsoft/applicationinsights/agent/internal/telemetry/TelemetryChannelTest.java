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

package com.microsoft.applicationinsights.agent.internal.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.core.http.HttpClient;
import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpPipelineBuilder;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.policy.HttpPipelinePolicy;
import com.azure.core.util.FluxUtil;
import com.microsoft.applicationinsights.agent.internal.MockHttpResponse;
import com.microsoft.applicationinsights.agent.internal.exporter.models.DataPointType;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MetricDataPoint;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MetricsData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MonitorBase;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.httpclient.RedirectPolicy;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalFileCache;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalFileWriter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TelemetryChannelTest {
  private TelemetryChannel telemetryChannel;
  private LocalFileCache localFileCache;
  private RecordingHttpClient recordingHttpClient;
  private static final AtomicInteger requestCount = new AtomicInteger();
  private static final String INSTRUMENTATION_KEY = "00000000-0000-0000-0000-0FEEDDADBEEF";
  private static final String REDIRECT_INSTRUMENTATION_KEY = "00000000-0000-0000-0000-0FEEDDADBEEE";
  private static final String END_POINT_URL = "http://foo.bar";
  private static final String REDIRECT_URL = "http://foo.bar.redirect";

  @TempDir File tempFolder;

  @BeforeEach
  @SuppressWarnings("CatchAndPrintStackTrace")
  public void setup() throws MalformedURLException {
    recordingHttpClient =
        new RecordingHttpClient(
            request -> {
              if (request.getUrl().toString().contains(REDIRECT_URL)) {
                return Mono.just(new MockHttpResponse(request, 200));
              }
              Flux<ByteBuffer> requestBody = request.getBody();
              ByteArrayOutputStream bos = new ByteArrayOutputStream();
              byte[] compressed = FluxUtil.collectBytesInByteBufferStream(requestBody).block();
              final int BUFFER_SIZE = compressed.length;
              String requestBodyString = null;
              try {
                ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
                GZIPInputStream gis = new GZIPInputStream(bis, BUFFER_SIZE);
                StringBuilder sb = new StringBuilder();
                byte[] data = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = gis.read(data)) != -1) {
                  sb.append(new String(data, 0, bytesRead, Charset.defaultCharset()));
                }
                bis.close();
                bos.close();
                gis.close();
                requestBodyString = new String(sb);
              } catch (IOException e) {
                e.printStackTrace();
              }
              if (requestBodyString.contains(INSTRUMENTATION_KEY)) {
                return Mono.just(new MockHttpResponse(request, 200));
              }
              Map<String, String> headers = new HashMap<>();
              headers.put("Location", REDIRECT_URL);
              HttpHeaders httpHeaders = new HttpHeaders(headers);
              return Mono.just(new MockHttpResponse(request, 307, httpHeaders));
            });
    List<HttpPipelinePolicy> policies = new ArrayList<>();
    policies.add(new RedirectPolicy());
    HttpPipelineBuilder pipelineBuilder =
        new HttpPipelineBuilder()
            .policies(policies.toArray(new HttpPipelinePolicy[0]))
            .httpClient(recordingHttpClient);
    localFileCache = new LocalFileCache();
    telemetryChannel =
        new TelemetryChannel(
            pipelineBuilder.build(),
            new URL(END_POINT_URL),
            new LocalFileWriter(localFileCache, tempFolder));
  }

  @AfterEach
  public void reset() {
    requestCount.set(0);
  }

  @Test
  public void singleIkeyTest() {
    // given
    List<TelemetryItem> telemetryItems = new ArrayList<>();
    telemetryItems.add(createMetricTelemetry("metric" + 1, 1, INSTRUMENTATION_KEY));

    // when
    CompletableResultCode completableResultCode = telemetryChannel.send(telemetryItems);

    // then
    assertThat(completableResultCode.isSuccess()).isEqualTo(true);
    assertThat(recordingHttpClient.getCount()).isEqualTo(1);
  }

  @Test
  public void dualIkeyTest() {
    // given
    List<TelemetryItem> telemetryItems = new ArrayList<>();
    telemetryItems.add(createMetricTelemetry("metric" + 1, 1, INSTRUMENTATION_KEY));
    telemetryItems.add(createMetricTelemetry("metric" + 2, 2, REDIRECT_INSTRUMENTATION_KEY));

    // when
    CompletableResultCode completableResultCode = telemetryChannel.send(telemetryItems);

    // then
    assertThat(completableResultCode.isSuccess()).isEqualTo(true);
    assertThat(recordingHttpClient.getCount()).isEqualTo(3);
  }

  @Test
  public void singleIkeyBatchTest() {
    // given
    List<TelemetryItem> telemetryItems = new ArrayList<>();
    telemetryItems.add(createMetricTelemetry("metric" + 1, 1, INSTRUMENTATION_KEY));
    telemetryItems.add(createMetricTelemetry("metric" + 2, 2, INSTRUMENTATION_KEY));

    // when
    CompletableResultCode completableResultCode = telemetryChannel.send(telemetryItems);

    // then
    assertThat(completableResultCode.isSuccess()).isEqualTo(true);
    assertThat(recordingHttpClient.getCount()).isEqualTo(1);
  }

  @Test
  public void dualIkeyBatchTest() {
    // given
    List<TelemetryItem> telemetryItems = new ArrayList<>();
    telemetryItems.add(createMetricTelemetry("metric" + 1, 1, INSTRUMENTATION_KEY));
    telemetryItems.add(createMetricTelemetry("metric" + 2, 2, INSTRUMENTATION_KEY));
    telemetryItems.add(createMetricTelemetry("metric" + 3, 3, REDIRECT_INSTRUMENTATION_KEY));
    telemetryItems.add(createMetricTelemetry("metric" + 4, 4, REDIRECT_INSTRUMENTATION_KEY));

    // when
    CompletableResultCode completableResultCode = telemetryChannel.send(telemetryItems);

    // then
    assertThat(completableResultCode.isSuccess()).isEqualTo(true);
    assertThat(recordingHttpClient.getCount()).isEqualTo(3);
  }

  @Test
  public void dualIkeyBatchWithDelayTest() {
    // given
    List<TelemetryItem> telemetryItems = new ArrayList<>();
    telemetryItems.add(createMetricTelemetry("metric" + 1, 1, INSTRUMENTATION_KEY));
    telemetryItems.add(createMetricTelemetry("metric" + 2, 2, INSTRUMENTATION_KEY));
    telemetryItems.add(createMetricTelemetry("metric" + 3, 3, REDIRECT_INSTRUMENTATION_KEY));
    telemetryItems.add(createMetricTelemetry("metric" + 4, 4, REDIRECT_INSTRUMENTATION_KEY));

    // when
    CompletableResultCode completableResultCode = telemetryChannel.send(telemetryItems);

    // then
    assertThat(completableResultCode.isSuccess()).isEqualTo(true);
    assertThat(recordingHttpClient.getCount()).isEqualTo(3);

    completableResultCode = telemetryChannel.send(telemetryItems);

    // then
    // the redirect url should be cached and should not invoke another redirect.
    assertThat(completableResultCode.isSuccess()).isEqualTo(true);
    assertThat(recordingHttpClient.getCount()).isEqualTo(5);
  }

  private static TelemetryItem createMetricTelemetry(
      String name, int value, String instrumentationKey) {
    TelemetryItem telemetry = new TelemetryItem();
    telemetry.setVersion(1);
    telemetry.setName("Metric");
    telemetry.setInstrumentationKey(instrumentationKey);
    Map<String, String> tags = new HashMap<>();
    tags.put("ai.internal.sdkVersion", "test_version");
    tags.put("ai.internal.nodeName", "test_role_name");
    tags.put("ai.cloud.roleInstance", "test_cloud_name");
    telemetry.setTags(tags);

    MetricsData data = new MetricsData();
    List<MetricDataPoint> dataPoints = new ArrayList<>();
    MetricDataPoint dataPoint = new MetricDataPoint();
    dataPoint.setDataPointType(DataPointType.MEASUREMENT);
    dataPoint.setName(name);
    dataPoint.setValue(value);
    dataPoint.setCount(1);
    dataPoints.add(dataPoint);

    Map<String, String> properties = new HashMap<>();
    properties.put("state", "blocked");

    data.setMetrics(dataPoints);
    data.setProperties(properties);

    MonitorBase monitorBase = new MonitorBase();
    monitorBase.setBaseType("MetricData");
    monitorBase.setBaseData(data);
    telemetry.setData(monitorBase);
    telemetry.setTime(new Date().toString());

    return telemetry;
  }

  static class RecordingHttpClient implements HttpClient {

    private final AtomicInteger count = new AtomicInteger();
    private final Function<HttpRequest, Mono<HttpResponse>> handler;

    RecordingHttpClient(Function<HttpRequest, Mono<HttpResponse>> handler) {
      this.handler = handler;
    }

    @Override
    public Mono<HttpResponse> send(HttpRequest httpRequest) {
      count.getAndIncrement();
      return handler.apply(httpRequest);
    }

    int getCount() {
      return count.get();
    }

    void resetCount() {
      count.set(0);
    }
  }
}
