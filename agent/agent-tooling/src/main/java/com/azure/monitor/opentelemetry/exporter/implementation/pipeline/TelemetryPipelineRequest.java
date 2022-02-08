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

package com.azure.monitor.opentelemetry.exporter.implementation.pipeline;

import com.azure.core.http.HttpMethod;
import com.azure.core.http.HttpRequest;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.List;
import reactor.core.publisher.Flux;

public class TelemetryPipelineRequest {

  private volatile URL url;
  private final String instrumentationKey;
  private final List<ByteBuffer> telemetry;
  private final int contentLength;

  TelemetryPipelineRequest(URL url, String instrumentationKey, List<ByteBuffer> telemetry) {
    this.url = url;
    this.instrumentationKey = instrumentationKey;
    this.telemetry = telemetry;
    contentLength = telemetry.stream().mapToInt(ByteBuffer::limit).sum();
  }

  public URL getUrl() {
    return url;
  }

  void setUrl(URL url) {
    this.url = url;
  }

  public String getInstrumentationKey() {
    return instrumentationKey;
  }

  public List<ByteBuffer> getTelemetry() {
    return telemetry;
  }

  HttpRequest createHttpRequest() {
    HttpRequest request = new HttpRequest(HttpMethod.POST, url);
    request.setBody(Flux.fromIterable(telemetry));
    request.setHeader("Content-Length", Integer.toString(contentLength));

    // need to suppress the default User-Agent "ReactorNetty/dev", otherwise Breeze ingestionservice
    // will put that User-Agent header into the client_Browser field for all telemetry that doesn't
    // explicitly set it's own UserAgent (ideally Breeze would only have this behavior for ingestion
    // directly from browsers)
    // TODO (trask) not setting User-Agent header at all would be a better option, but haven't
    //  figured out how to do that yet
    request.setHeader("User-Agent", "");
    request.setHeader("Content-Encoding", "gzip");

    return request;
  }
}
