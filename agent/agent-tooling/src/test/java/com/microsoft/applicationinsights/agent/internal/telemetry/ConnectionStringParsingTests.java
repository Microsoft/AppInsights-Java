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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.microsoft.applicationinsights.agent.internal.configuration.DefaultEndpoints;
import com.microsoft.applicationinsights.agent.internal.telemetry.ConnectionString.EndpointPrefixes;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConnectionStringParsingTests {

  @Test
  void minimalString() throws Exception {
    final String ikey = "fake-ikey";
    final String cs = "InstrumentationKey=" + ikey;

    ConnectionString parsed = ConnectionString.parse(cs);
    assertThat(parsed.getInstrumentationKey()).isEqualTo(ikey);
    assertThat(parsed.getIngestionEndpoint())
        .isEqualTo(
            new URL(
                DefaultEndpoints.INGESTION_ENDPOINT + "/" + EndpointProvider.INGESTION_URL_PATH));
    assertThat(parsed.getLiveEndpoint())
        .isEqualTo(new URL(DefaultEndpoints.LIVE_ENDPOINT + "/" + EndpointProvider.LIVE_URL_PATH));
  }

  @Test // this test does not use this.config
  void appIdUrlIsConstructedWithIkeyFromIngestionEndpoint() throws MalformedURLException {
    EndpointProvider ep = new EndpointProvider();
    String ikey = "fake-ikey";
    final String host = "http://123.com";
    ep.setIngestionEndpoint(new URL(host));
    assertThat(ep.getAppIdEndpointUrl(ikey))
        .isEqualTo(
            new URL(
                host
                    + "/"
                    + EndpointProvider.API_PROFILES_APP_ID_URL_PREFIX
                    + ikey
                    + EndpointProvider.API_PROFILES_APP_ID_URL_SUFFIX));
  }

  @Test
  void appIdUrlWithPathKeepsIt() throws MalformedURLException {
    EndpointProvider ep = new EndpointProvider();
    String ikey = "fake-ikey";
    String url = "http://123.com/path/321";
    ep.setIngestionEndpoint(new URL(url));
    assertThat(ep.getAppIdEndpointUrl(ikey))
        .isEqualTo(
            new URL(
                url
                    + "/"
                    + EndpointProvider.API_PROFILES_APP_ID_URL_PREFIX
                    + ikey
                    + EndpointProvider.API_PROFILES_APP_ID_URL_SUFFIX));

    ep.setIngestionEndpoint(new URL(url + "/"));
    assertThat(ep.getAppIdEndpointUrl(ikey))
        .isEqualTo(
            new URL(
                url
                    + "/"
                    + EndpointProvider.API_PROFILES_APP_ID_URL_PREFIX
                    + ikey
                    + EndpointProvider.API_PROFILES_APP_ID_URL_SUFFIX));
  }

  @Test
  void ikeyWithSuffix() throws Exception {
    final String ikey = "fake-ikey";
    final String suffix = "ai.example.com";
    final String cs = "InstrumentationKey=" + ikey + ";EndpointSuffix=" + suffix;
    URL expectedIngestionEndpointUrl =
        new URL(
            "https://"
                + EndpointPrefixes.INGESTION_ENDPOINT_PREFIX
                + "."
                + suffix
                + "/"
                + EndpointProvider.INGESTION_URL_PATH);
    URL expectedLiveEndpoint =
        new URL(
            "https://"
                + EndpointPrefixes.LIVE_ENDPOINT_PREFIX
                + "."
                + suffix
                + "/"
                + EndpointProvider.LIVE_URL_PATH);

    ConnectionString parsed = ConnectionString.parse(cs);
    assertThat(parsed.getInstrumentationKey()).isEqualTo(ikey);
    assertThat(parsed.getIngestionEndpoint()).isEqualTo(expectedIngestionEndpointUrl);
    assertThat(parsed.getLiveEndpoint()).isEqualTo(expectedLiveEndpoint);
  }

  @Test
  void suffixWithPathRetainsThePath() throws Exception {
    final String ikey = "fake-ikey";
    final String suffix = "ai.example.com/my-proxy-app/doProxy";
    final String cs = "InstrumentationKey=" + ikey + ";EndpointSuffix=" + suffix;
    URL expectedIngestionEndpointUrl =
        new URL(
            "https://"
                + EndpointPrefixes.INGESTION_ENDPOINT_PREFIX
                + "."
                + suffix
                + "/"
                + EndpointProvider.INGESTION_URL_PATH);
    URL expectedLiveEndpoint =
        new URL(
            "https://"
                + EndpointPrefixes.LIVE_ENDPOINT_PREFIX
                + "."
                + suffix
                + "/"
                + EndpointProvider.LIVE_URL_PATH);

    ConnectionString parsed = ConnectionString.parse(cs);
    assertThat(parsed.getInstrumentationKey()).isEqualTo(ikey);
    assertThat(parsed.getIngestionEndpoint()).isEqualTo(expectedIngestionEndpointUrl);
    assertThat(parsed.getLiveEndpoint()).isEqualTo(expectedLiveEndpoint);
  }

  @Test
  void suffixSupportsPort() throws Exception {
    final String ikey = "fake-ikey";
    final String suffix = "ai.example.com:9999";
    final String cs = "InstrumentationKey=" + ikey + ";EndpointSuffix=" + suffix;
    URL expectedIngestionEndpointUrl =
        new URL(
            "https://"
                + EndpointPrefixes.INGESTION_ENDPOINT_PREFIX
                + "."
                + suffix
                + "/"
                + EndpointProvider.INGESTION_URL_PATH);
    URL expectedLiveEndpoint =
        new URL(
            "https://"
                + EndpointPrefixes.LIVE_ENDPOINT_PREFIX
                + "."
                + suffix
                + "/"
                + EndpointProvider.LIVE_URL_PATH);

    ConnectionString parsed = ConnectionString.parse(cs);
    assertThat(parsed.getInstrumentationKey()).isEqualTo(ikey);
    assertThat(parsed.getIngestionEndpoint()).isEqualTo(expectedIngestionEndpointUrl);
    assertThat(parsed.getLiveEndpoint()).isEqualTo(expectedLiveEndpoint);
  }

  @Test
  void ikeyWithExplicitEndpoints() throws Exception {
    final String ikey = "fake-ikey";
    URL expectedIngestionEndpoint = new URL("https://ingestion.example.com");
    URL expectedIngestionEndpointUrl =
        new URL("https://ingestion.example.com/" + EndpointProvider.INGESTION_URL_PATH);
    final String liveHost = "https://live.example.com";
    URL expectedLiveEndpoint = new URL(liveHost + "/" + EndpointProvider.LIVE_URL_PATH);

    String cs =
        "InstrumentationKey="
            + ikey
            + ";IngestionEndpoint="
            + expectedIngestionEndpoint
            + ";LiveEndpoint="
            + liveHost;

    ConnectionString parsed = ConnectionString.parse(cs);
    assertThat(parsed.getInstrumentationKey()).isEqualTo(ikey);
    assertThat(parsed.getIngestionEndpoint()).isEqualTo(expectedIngestionEndpointUrl);
    assertThat(parsed.getLiveEndpoint()).isEqualTo(expectedLiveEndpoint);
  }

  @Test
  void explicitEndpointOverridesSuffix() throws Exception {
    final String ikey = "fake-ikey";
    final String suffix = "ai.example.com";
    URL expectedIngestionEndpoint = new URL("https://ingestion.example.com");
    URL expectedIngestionEndpointUrl =
        new URL("https://ingestion.example.com/" + EndpointProvider.INGESTION_URL_PATH);
    URL expectedLiveEndpoint =
        new URL(
            "https://"
                + EndpointPrefixes.LIVE_ENDPOINT_PREFIX
                + "."
                + suffix
                + "/"
                + EndpointProvider.LIVE_URL_PATH);
    String cs =
        "InstrumentationKey="
            + ikey
            + ";IngestionEndpoint="
            + expectedIngestionEndpoint
            + ";EndpointSuffix="
            + suffix;

    ConnectionString parsed = ConnectionString.parse(cs);
    assertThat(parsed.getInstrumentationKey()).isEqualTo(ikey);
    assertThat(parsed.getIngestionEndpoint()).isEqualTo(expectedIngestionEndpointUrl);
    assertThat(parsed.getLiveEndpoint()).isEqualTo(expectedLiveEndpoint);
  }

  @Test
  void emptyPairIsIgnored() throws MalformedURLException {
    final String ikey = "fake-ikey";
    final String suffix = "ai.example.com";
    final String cs = "InstrumentationKey=" + ikey + ";;EndpointSuffix=" + suffix + ";";
    URL expectedIngestionEndpointUrl =
        new URL(
            "https://"
                + EndpointPrefixes.INGESTION_ENDPOINT_PREFIX
                + "."
                + suffix
                + "/"
                + EndpointProvider.INGESTION_URL_PATH);
    URL expectedLiveEndpoint =
        new URL(
            "https://"
                + EndpointPrefixes.LIVE_ENDPOINT_PREFIX
                + "."
                + suffix
                + "/"
                + EndpointProvider.LIVE_URL_PATH);

    ConnectionString parsed = ConnectionString.parse(cs);
    assertThat(parsed.getInstrumentationKey()).isEqualTo(ikey);
    assertThat(parsed.getIngestionEndpoint()).isEqualTo(expectedIngestionEndpointUrl);
    assertThat(parsed.getLiveEndpoint()).isEqualTo(expectedLiveEndpoint);
  }

  @Test
  void emptyKeyIsIgnored() throws MalformedURLException {
    final String ikey = "fake-ikey";
    final String cs = "InstrumentationKey=" + ikey + ";=1234";
    URL expectedIngestionEndpointUrl =
        new URL(DefaultEndpoints.INGESTION_ENDPOINT + "/" + EndpointProvider.INGESTION_URL_PATH);
    URL expectedLiveEndpoint =
        new URL(DefaultEndpoints.LIVE_ENDPOINT + "/" + EndpointProvider.LIVE_URL_PATH);

    ConnectionString parsed = ConnectionString.parse(cs);
    assertThat(parsed.getInstrumentationKey()).isEqualTo(ikey);
    assertThat(parsed.getIngestionEndpoint()).isEqualTo(expectedIngestionEndpointUrl);
    assertThat(parsed.getLiveEndpoint()).isEqualTo(expectedLiveEndpoint);
  }

  @Test
  void emptyValueIsSameAsUnset() throws Exception {
    final String ikey = "fake-ikey";
    final String cs = "InstrumentationKey=" + ikey + ";EndpointSuffix=";

    ConnectionString parsed = ConnectionString.parse(cs);
    assertThat(parsed.getInstrumentationKey()).isEqualTo(ikey);
    assertThat(parsed.getIngestionEndpoint())
        .isEqualTo(
            new URL(
                DefaultEndpoints.INGESTION_ENDPOINT + "/" + EndpointProvider.INGESTION_URL_PATH));
    assertThat(parsed.getLiveEndpoint())
        .isEqualTo(new URL(DefaultEndpoints.LIVE_ENDPOINT + "/" + EndpointProvider.LIVE_URL_PATH));
  }

  @Test
  void caseInsensitiveParsing() {
    final String ikey = "fake-ikey";
    final String live = "https://live.something.com";
    final String profiler = "https://prof.something.com";
    final String cs1 =
        "InstrumentationKey=" + ikey + ";LiveEndpoint=" + live + ";ProfilerEndpoint=" + profiler;
    final String cs2 =
        "instRUMentationkEY=" + ikey + ";LivEEndPOINT=" + live + ";ProFILErEndPOinT=" + profiler;

    ConnectionString parsed = ConnectionString.parse(cs1);
    ConnectionString parsed2 = ConnectionString.parse(cs2);

    assertThat(parsed2.getInstrumentationKey()).isEqualTo(parsed.getInstrumentationKey());
    assertThat(parsed2.getIngestionEndpoint()).isEqualTo(parsed.getIngestionEndpoint());
    assertThat(parsed2.getIngestionEndpoint()).isEqualTo(parsed.getIngestionEndpoint());
    assertThat(parsed2.getLiveEndpoint()).isEqualTo(parsed.getLiveEndpoint());
    assertThat(parsed2.getProfilerEndpoint()).isEqualTo(parsed.getProfilerEndpoint());
  }

  @Test
  void orderDoesNotMatter() {
    final String ikey = "fake-ikey";
    final String live = "https://live.something.com";
    final String profiler = "https://prof.something.com";
    final String snapshot = "https://whatever.snappy.com";
    final String cs1 =
        "InstrumentationKey="
            + ikey
            + ";LiveEndpoint="
            + live
            + ";ProfilerEndpoint="
            + profiler
            + ";SnapshotEndpoint="
            + snapshot;
    final String cs2 =
        "SnapshotEndpoint="
            + snapshot
            + ";ProfilerEndpoint="
            + profiler
            + ";InstrumentationKey="
            + ikey
            + ";LiveEndpoint="
            + live;

    ConnectionString parsed = ConnectionString.parse(cs1);
    ConnectionString parsed2 = ConnectionString.parse(cs2);

    assertThat(parsed2.getInstrumentationKey()).isEqualTo(parsed.getInstrumentationKey());
    assertThat(parsed2.getIngestionEndpoint()).isEqualTo(parsed.getIngestionEndpoint());
    assertThat(parsed2.getIngestionEndpoint()).isEqualTo(parsed.getIngestionEndpoint());
    assertThat(parsed2.getLiveEndpoint()).isEqualTo(parsed.getLiveEndpoint());
    assertThat(parsed2.getProfilerEndpoint()).isEqualTo(parsed.getProfilerEndpoint());
  }

  @Test
  void endpointWithNoSchemeIsInvalid() {
    assertThatThrownBy(
            () ->
                ConnectionString.parse(
                    "InstrumentationKey=fake-ikey;IngestionEndpoint=my-ai.example.com"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("IngestionEndpoint");
  }

  @Test
  void endpointWithPathMissingSchemeIsInvalid() {
    assertThatThrownBy(
            () ->
                ConnectionString.parse(
                    "InstrumentationKey=fake-ikey;IngestionEndpoint=my-ai.example.com/path/prefix"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("IngestionEndpoint");
  }

  @Test
  void endpointWithPortMissingSchemeIsInvalid() {
    assertThatThrownBy(
            () ->
                ConnectionString.parse(
                    "InstrumentationKey=fake-ikey;IngestionEndpoint=my-ai.example.com:9999"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("IngestionEndpoint");
  }

  @Test
  void httpEndpointKeepsScheme() throws Exception {
    ConnectionString parsed =
        ConnectionString.parse(
            "InstrumentationKey=fake-ikey;IngestionEndpoint=http://my-ai.example.com");
    assertThat(parsed.getIngestionEndpoint())
        .isEqualTo(new URL("http://my-ai.example.com/v2.1/track"));
  }

  @Test
  void emptyIkeyValueIsInvalid() {
    assertThatThrownBy(
            () ->
                ConnectionString.parse(
                    "InstrumentationKey=;IngestionEndpoint=https://ingestion.example.com;EndpointSuffix=ai.example.com"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void nonKeyValueStringIsInvalid() {
    assertThatThrownBy(() -> ConnectionString.parse(UUID.randomUUID().toString()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test // when more Authorization values are available, create a copy of this test. For example,
  // given "Authorization=Xyz", this would fail because the 'Xyz' key/value pair is missing.
  void missingInstrumentationKeyIsInvalid() {
    assertThatThrownBy(() -> ConnectionString.parse("LiveEndpoint=https://live.example.com"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void invalidUrlIsInvalidConnectionString() {
    assertThatThrownBy(
            () -> ConnectionString.parse("InstrumentationKey=fake-ikey;LiveEndpoint=httpx://host"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasCauseInstanceOf(MalformedURLException.class)
        .hasMessageContaining("LiveEndpoint");
  }

  @Test
  void giantValuesAreNotAllowed() {
    StringBuilder bigIkey = new StringBuilder();
    for (int i = 0; i < ConnectionString.CONNECTION_STRING_MAX_LENGTH * 2; i++) {
      bigIkey.append('0');
    }

    assertThatThrownBy(() -> ConnectionString.parse("InstrumentationKey=" + bigIkey))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(Integer.toString(ConnectionString.CONNECTION_STRING_MAX_LENGTH));
  }

  @Test
  void resetEndpointUrlTest() {
    String fakeConnectionString =
        "InstrumentationKey=fake-key;IngestionEndpoint=https://ingestion.example.com/;LiveEndpoint=https://live.example.com/";
    ConnectionString parsed = ConnectionString.parse(fakeConnectionString);

    assertThat(parsed.getIngestionEndpoint().toString())
        .isEqualTo("https://ingestion.example.com/v2.1/track");
    assertThat(parsed.getLiveEndpoint().toString())
        .isEqualTo("https://live.example.com/QuickPulseService.svc");

    String newFakeConnectionString =
        "InstrumentationKey=new-fake-key;IngestionEndpoint=https://new-ingestion.example.com/;LiveEndpoint=https://new-live.example.com/";
    parsed = ConnectionString.parse(newFakeConnectionString);

    assertThat(parsed.getIngestionEndpoint().toString())
        .isEqualTo("https://new-ingestion.example.com/v2.1/track");
    assertThat(parsed.getLiveEndpoint().toString())
        .isEqualTo("https://new-live.example.com/QuickPulseService.svc");

    String newerFakeConnectionString =
        "InstrumentationKey=newer-fake-key;IngestionEndpoint=https://newer-ingestion.example.com/;LiveEndpoint=https://newer-live.example.com/";
    parsed = ConnectionString.parse(newerFakeConnectionString);

    assertThat(parsed.getIngestionEndpoint().toString())
        .isEqualTo("https://newer-ingestion.example.com/v2.1/track");
    assertThat(parsed.getLiveEndpoint().toString())
        .isEqualTo("https://newer-live.example.com/QuickPulseService.svc");
  }
}
