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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import com.microsoft.applicationinsights.agent.internal.common.PropertyHelper;
import com.microsoft.applicationinsights.agent.internal.common.Strings;
import com.microsoft.applicationinsights.agent.internal.configuration.Configuration;
import com.microsoft.applicationinsights.agent.internal.exporter.models.ContextTagKeys;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MessageData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MetricDataPoint;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MetricsData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MonitorBase;
import com.microsoft.applicationinsights.agent.internal.exporter.models.MonitorDomain;
import com.microsoft.applicationinsights.agent.internal.exporter.models.PageViewData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.RemoteDependencyData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.RequestData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryEventData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryExceptionData;
import com.microsoft.applicationinsights.agent.internal.exporter.models.TelemetryItem;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalFileCache;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalFileLoader;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalFileSender;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalFileWriter;
import com.microsoft.applicationinsights.agent.internal.localstorage.LocalStorageUtils;
import com.microsoft.applicationinsights.agent.internal.quickpulse.QuickPulseDataCollector;
import com.microsoft.applicationinsights.agent.internal.statsbeat.StatsbeatModule;
import io.opentelemetry.instrumentation.api.cache.Cache;
import io.opentelemetry.sdk.common.CompletableResultCode;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.text.StringSubstitutor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class TelemetryClient {

  private static final String EVENT_TELEMETRY_NAME = "Event";
  private static final String EXCEPTION_TELEMETRY_NAME = "Exception";
  private static final String MESSAGE_TELEMETRY_NAME = "Message";
  private static final String METRIC_TELEMETRY_NAME = "Metric";
  private static final String PAGE_VIEW_TELEMETRY_NAME = "PageView";
  private static final String REMOTE_DEPENDENCY_TELEMETRY_NAME = "RemoteDependency";
  private static final String REQUEST_TELEMETRY_NAME = "Request";

  private static volatile @MonotonicNonNull TelemetryClient active;

  private final Set<String> nonFilterableMetricNames = new HashSet<>();

  @Nullable private volatile String instrumentationKey;
  private volatile @MonotonicNonNull String roleName;
  private volatile @MonotonicNonNull String roleInstance;
  private volatile @MonotonicNonNull String statsbeatInstrumentationKey;

  private final EndpointProvider endpointProvider = new EndpointProvider();

  // globalTags contain:
  // * cloud role name
  // * cloud role instance
  // * sdk version
  // * application version (if provided in customDimensions)
  private final Map<String, String> globalTags;
  // contains customDimensions from json configuration
  private final Map<String, String> globalProperties;

  private final List<MetricFilter> metricFilters;

  private final Cache<String, String> ikeyEndpointMap;
  private final StatsbeatModule statsbeatModule;
  private final boolean readOnlyFileSystem;
  private final int generalExportQueueCapacity;
  private final int metricsExportQueueCapacity;

  @Nullable private final Configuration.AadAuthentication aadAuthentication;

  private final Object channelInitLock = new Object();
  private volatile @MonotonicNonNull BatchSpanProcessor generalChannelBatcher;
  private volatile @MonotonicNonNull BatchSpanProcessor metricsChannelBatcher;
  private volatile @MonotonicNonNull BatchSpanProcessor statsbeatChannelBatcher;

  public static TelemetryClient.Builder builder() {
    return new TelemetryClient.Builder();
  }

  // only used by tests
  public static TelemetryClient createForTest() {
    return builder()
        .setCustomDimensions(new HashMap<>())
        .setMetricFilters(new ArrayList<>())
        .setIkeyEndpointMap(Cache.bounded(100))
        .setStatsbeatModule(new StatsbeatModule(null))
        .build();
  }

  public TelemetryClient(Builder builder) {
    this.globalTags = builder.globalTags;
    this.globalProperties = builder.globalProperties;
    this.metricFilters = builder.metricFilters;
    this.ikeyEndpointMap = builder.ikeyEndpointMap;
    this.statsbeatModule = builder.statsbeatModule;
    this.readOnlyFileSystem = builder.readOnlyFileSystem;
    this.generalExportQueueCapacity = builder.generalExportQueueCapacity;
    this.metricsExportQueueCapacity = builder.metricsExportQueueCapacity;
    this.aadAuthentication = builder.aadAuthentication;
  }

  public static TelemetryClient getActive() {
    if (active == null) {
      throw new IllegalStateException("agent was not initialized");
    }

    return active;
  }

  public static void setActive(TelemetryClient telemetryClient) {
    if (active != null) {
      throw new IllegalStateException("Already initialized");
    }
    TelemetryClient.active = telemetryClient;
  }

  public void trackAsync(TelemetryItem telemetry) {
    if (Strings.isNullOrEmpty(instrumentationKey)) {
      return;
    }

    MonitorDomain data = telemetry.getData().getBaseData();
    if (data instanceof MetricsData) {
      MetricsData metricsData = (MetricsData) data;
      if (metricsData.getMetrics().isEmpty()) {
        throw new AssertionError("MetricsData has no metric point");
      }
      MetricDataPoint point = metricsData.getMetrics().get(0);
      String metricName = point.getName();
      if (!nonFilterableMetricNames.contains(metricName)) {
        for (MetricFilter metricFilter : metricFilters) {
          if (!metricFilter.matches(metricName)) {
            // user configuration filtered out this metric name
            return;
          }
        }
      }

      if (!Double.isFinite(point.getValue())) {
        // TODO (trask) add test for this
        // breeze doesn't like these values
        return;
      }
    }

    if (telemetry.getTime() == null) {
      // this is easy to forget when adding new telemetry
      throw new AssertionError("telemetry item is missing time");
    }

    QuickPulseDataCollector.INSTANCE.add(telemetry);

    TelemetryObservers.INSTANCE.getObservers().forEach(consumer -> consumer.accept(telemetry));

    // batching, retry, throttling, and writing to disk on failure occur downstream
    // for simplicity not reporting back success/failure from this layer
    // only that it was successfully delivered to the next layer
    if (data instanceof MetricsData) {
      getMetricsChannelBatcher().trackAsync(telemetry);
    } else {
      getGeneralChannelBatcher().trackAsync(telemetry);
    }
  }

  public void trackStatsbeatAsync(TelemetryItem telemetry) {
    // batching, retry, throttling, and writing to disk on failure occur downstream
    // for simplicity not reporting back success/failure from this layer
    // only that it was successfully delivered to the next layer
    getStatsbeatChannelBatcher().trackAsync(telemetry);
  }

  public CompletableResultCode flushChannelBatcher() {
    if (generalChannelBatcher != null) {
      return generalChannelBatcher.forceFlush();
    } else {
      return CompletableResultCode.ofSuccess();
    }
  }

  private BatchSpanProcessor getGeneralChannelBatcher() {
    if (generalChannelBatcher == null) {
      synchronized (channelInitLock) {
        if (generalChannelBatcher == null) {
          generalChannelBatcher = initChannelBatcher(generalExportQueueCapacity, 512, "general");
        }
      }
    }
    return generalChannelBatcher;
  }

  // metrics get flooded every 60 seconds by default, so need much larger queue size to avoid
  // dropping telemetry (they are much smaller so a larger queue size and larger batch size are ok)
  private BatchSpanProcessor getMetricsChannelBatcher() {
    if (metricsChannelBatcher == null) {
      synchronized (channelInitLock) {
        if (metricsChannelBatcher == null) {
          metricsChannelBatcher = initChannelBatcher(metricsExportQueueCapacity, 2048, "metrics");
        }
      }
    }
    return metricsChannelBatcher;
  }

  private BatchSpanProcessor initChannelBatcher(
      int exportQueueCapacity, int maxExportBatchSize, String queueName) {
    LocalFileLoader localFileLoader = null;
    LocalFileWriter localFileWriter = null;
    if (!readOnlyFileSystem) {
      File telemetryFolder = LocalStorageUtils.getOfflineTelemetryFolder();
      LocalFileCache localFileCache = new LocalFileCache(telemetryFolder);
      localFileLoader =
          new LocalFileLoader(
              localFileCache, telemetryFolder, statsbeatModule.getNonessentialStatsbeat());
      localFileWriter =
          new LocalFileWriter(
              localFileCache, telemetryFolder, statsbeatModule.getNonessentialStatsbeat());
    }

    TelemetryChannel channel =
        TelemetryChannel.create(
            endpointProvider.getIngestionEndpointUrl(),
            localFileWriter,
            ikeyEndpointMap,
            statsbeatModule,
            false,
            aadAuthentication);

    if (!readOnlyFileSystem) {
      LocalFileSender.start(localFileLoader, channel);
    }

    return BatchSpanProcessor.builder(channel)
        .setMaxQueueSize(exportQueueCapacity)
        .setMaxExportBatchSize(maxExportBatchSize)
        .build(queueName);
  }

  public BatchSpanProcessor getStatsbeatChannelBatcher() {
    if (statsbeatChannelBatcher == null) {
      synchronized (channelInitLock) {
        if (statsbeatChannelBatcher == null) {
          File statsbeatFolder;
          LocalFileLoader localFileLoader = null;
          LocalFileWriter localFileWriter = null;
          if (!readOnlyFileSystem) {
            statsbeatFolder = LocalStorageUtils.getOfflineStatsbeatFolder();
            LocalFileCache localFileCache = new LocalFileCache(statsbeatFolder);
            localFileLoader = new LocalFileLoader(localFileCache, statsbeatFolder, null);
            localFileWriter = new LocalFileWriter(localFileCache, statsbeatFolder, null);
          }

          TelemetryChannel channel =
              TelemetryChannel.create(
                  endpointProvider.getStatsbeatEndpointUrl(),
                  localFileWriter,
                  ikeyEndpointMap,
                  statsbeatModule,
                  true,
                  null);

          if (!readOnlyFileSystem) {
            LocalFileSender.start(localFileLoader, channel);
          }

          statsbeatChannelBatcher = BatchSpanProcessor.builder(channel).build("statsbeat");
        }
      }
    }
    return statsbeatChannelBatcher;
  }

  /** Gets or sets the default instrumentation key for the application. */
  public String getInstrumentationKey() {
    return instrumentationKey;
  }

  /** Gets or sets the default instrumentation key for the application. */
  public void setInstrumentationKey(@Nullable String key) {
    instrumentationKey = key;
  }

  public String getStatsbeatInstrumentationKey() {
    return statsbeatInstrumentationKey;
  }

  public void setStatsbeatInstrumentationKey(String key) {
    statsbeatInstrumentationKey = key;
  }

  @Nullable
  public String getRoleName() {
    return roleName;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
    globalTags.put(ContextTagKeys.AI_CLOUD_ROLE.toString(), roleName);
  }

  public String getRoleInstance() {
    return roleInstance;
  }

  public void setRoleInstance(String roleInstance) {
    this.roleInstance = roleInstance;
    globalTags.put(ContextTagKeys.AI_CLOUD_ROLE_INSTANCE.toString(), roleInstance);
  }

  public void setConnectionString(String connectionString) {
    try {
      ConnectionString.parseInto(connectionString, this);
    } catch (InvalidConnectionStringException e) {
      throw new IllegalArgumentException("Invalid connection string", e);
    } catch (MalformedURLException e) {
      throw new IllegalStateException("Invalid endpoint urls.", e);
    }
  }

  public EndpointProvider getEndpointProvider() {
    return endpointProvider;
  }

  public Configuration.AadAuthentication getAadAuthentication() {
    return aadAuthentication;
  }

  public StatsbeatModule getStatsbeatModule() {
    return statsbeatModule;
  }

  public void addNonFilterableMetricNames(String... metricNames) {
    nonFilterableMetricNames.addAll(asList(metricNames));
  }

  // must be called before setting any telemetry tags or data properties
  //
  // telemetry tags will be non-null after this call
  // data properties may or may not be non-null after this call
  public void initEventTelemetry(TelemetryItem telemetry, TelemetryEventData data) {
    if (telemetry.getTags() != null) {
      throw new AssertionError("must not set telemetry tags before calling init");
    }
    if (data.getProperties() != null) {
      throw new AssertionError("must not set data properties before calling init");
    }
    initTelemetry(telemetry, data, EVENT_TELEMETRY_NAME, "EventData");
    if (!globalProperties.isEmpty()) {
      data.setProperties(new HashMap<>(globalProperties));
    }
  }

  // must be called before setting any telemetry tags or data properties
  //
  // telemetry tags will be non-null after this call
  // data properties may or may not be non-null after this call
  public void initExceptionTelemetry(TelemetryItem telemetry, TelemetryExceptionData data) {
    if (telemetry.getTags() != null) {
      throw new AssertionError("must not set telemetry tags before calling init");
    }
    if (data.getProperties() != null) {
      throw new AssertionError("must not set data properties before calling init");
    }
    initTelemetry(telemetry, data, EXCEPTION_TELEMETRY_NAME, "ExceptionData");
    if (!globalProperties.isEmpty()) {
      data.setProperties(new HashMap<>(globalProperties));
    }
  }

  // must be called before setting any telemetry tags or data properties
  //
  // telemetry tags will be non-null after this call
  // data properties may or may not be non-null after this call
  public void initMessageTelemetry(TelemetryItem telemetry, MessageData data) {
    if (telemetry.getTags() != null) {
      throw new AssertionError("must not set telemetry tags before calling init");
    }
    if (data.getProperties() != null) {
      throw new AssertionError("must not set data properties before calling init");
    }
    initTelemetry(telemetry, data, MESSAGE_TELEMETRY_NAME, "MessageData");
    if (!globalProperties.isEmpty()) {
      data.setProperties(new HashMap<>(globalProperties));
    }
  }

  // must be called before setting any telemetry tags or data properties
  //
  // telemetry tags will be non-null after this call
  // data properties may or may not be non-null after this call
  // FIXME (trask) azure sdk exporter: rename MetricsData to MetricData to match the telemetryName
  //  and baseType?
  public void initMetricTelemetry(
      TelemetryItem telemetry, MetricsData data, MetricDataPoint point) {
    if (telemetry.getTags() != null) {
      throw new AssertionError("must not set telemetry tags before calling init");
    }
    if (data.getProperties() != null) {
      throw new AssertionError("must not set data properties before calling init");
    }
    initTelemetry(telemetry, data, METRIC_TELEMETRY_NAME, "MetricData");
    if (!globalProperties.isEmpty()) {
      data.setProperties(new HashMap<>(globalProperties));
    }
    data.setMetrics(singletonList(point));
  }

  // must be called before setting any telemetry tags or data properties
  //
  // telemetry tags will be non-null after this call
  // data properties may or may not be non-null after this call
  public void initPageViewTelemetry(TelemetryItem telemetry, PageViewData data) {
    if (telemetry.getTags() != null) {
      throw new AssertionError("must not set telemetry tags before calling init");
    }
    if (data.getProperties() != null) {
      throw new AssertionError("must not set data properties before calling init");
    }
    initTelemetry(telemetry, data, PAGE_VIEW_TELEMETRY_NAME, "PageViewData");
    if (!globalProperties.isEmpty()) {
      data.setProperties(new HashMap<>(globalProperties));
    }
  }

  // must be called before setting any telemetry tags or data properties
  //
  // telemetry tags will be non-null after this call
  // data properties may or may not be non-null after this call
  public void initRemoteDependencyTelemetry(TelemetryItem telemetry, RemoteDependencyData data) {
    if (telemetry.getTags() != null) {
      throw new AssertionError("must not set telemetry tags before calling init");
    }
    if (data.getProperties() != null) {
      throw new AssertionError("must not set data properties before calling init");
    }
    initTelemetry(telemetry, data, REMOTE_DEPENDENCY_TELEMETRY_NAME, "RemoteDependencyData");
    if (!globalProperties.isEmpty()) {
      data.setProperties(new HashMap<>(globalProperties));
    }
  }

  // must be called before setting any telemetry tags or data properties
  //
  // telemetry tags will be non-null after this call
  // data properties may or may not be non-null after this call
  public void initRequestTelemetry(TelemetryItem telemetry, RequestData data) {
    if (telemetry.getTags() != null) {
      throw new AssertionError("must not set telemetry tags before calling init");
    }
    if (data.getProperties() != null) {
      throw new AssertionError("must not set data properties before calling init");
    }
    initTelemetry(telemetry, data, REQUEST_TELEMETRY_NAME, "RequestData");
    if (!globalProperties.isEmpty()) {
      data.setProperties(new HashMap<>(globalProperties));
    }
  }

  private void initTelemetry(
      TelemetryItem telemetry, MonitorDomain data, String telemetryName, String baseType) {
    telemetry.setVersion(1);
    telemetry.setName(telemetryName);
    telemetry.setInstrumentationKey(instrumentationKey);
    telemetry.setTags(new HashMap<>(globalTags));

    data.setVersion(2);

    MonitorBase monitorBase = new MonitorBase();
    telemetry.setData(monitorBase);
    monitorBase.setBaseType(baseType);
    monitorBase.setBaseData(data);
  }

  public static class Builder {

    private Map<String, String> globalTags;
    private Map<String, String> globalProperties;
    private List<MetricFilter> metricFilters;
    private Cache<String, String> ikeyEndpointMap;
    private StatsbeatModule statsbeatModule;
    private boolean readOnlyFileSystem;
    private int generalExportQueueCapacity;
    private int metricsExportQueueCapacity;
    @Nullable private Configuration.AadAuthentication aadAuthentication;

    public Builder setCustomDimensions(Map<String, String> customDimensions) {
      StringSubstitutor substitutor = new StringSubstitutor(System.getenv());
      Map<String, String> globalProperties = new HashMap<>();
      Map<String, String> globalTags = new HashMap<>();
      for (Map.Entry<String, String> entry : customDimensions.entrySet()) {
        String key = entry.getKey();
        if (key.equals("service.version")) {
          globalTags.put(
              ContextTagKeys.AI_APPLICATION_VER.toString(), substitutor.replace(entry.getValue()));
        } else {
          globalProperties.put(key, substitutor.replace(entry.getValue()));
        }
      }

      globalTags.put(
          ContextTagKeys.AI_INTERNAL_SDK_VERSION.toString(),
          PropertyHelper.getQualifiedSdkVersionString());

      this.globalProperties = globalProperties;
      this.globalTags = globalTags;

      return this;
    }

    public Builder setMetricFilters(List<MetricFilter> metricFilters) {
      this.metricFilters = metricFilters;
      return this;
    }

    public Builder setIkeyEndpointMap(Cache<String, String> ikeyEndpointMap) {
      this.ikeyEndpointMap = ikeyEndpointMap;
      return this;
    }

    public Builder setStatsbeatModule(StatsbeatModule statsbeatModule) {
      this.statsbeatModule = statsbeatModule;
      return this;
    }

    public Builder setReadOnlyFileSystem(boolean readOnlyFileSystem) {
      this.readOnlyFileSystem = readOnlyFileSystem;
      return this;
    }

    public Builder setGeneralExportQueueSize(int generalExportQueueCapacity) {
      this.generalExportQueueCapacity = generalExportQueueCapacity;
      return this;
    }

    public Builder setMetricsExportQueueSize(int metricsExportQueueCapacity) {
      this.metricsExportQueueCapacity = metricsExportQueueCapacity;
      return this;
    }

    public Builder setAadAuthentication(Configuration.AadAuthentication aadAuthentication) {
      this.aadAuthentication = aadAuthentication;
      return this;
    }

    public TelemetryClient build() {
      return new TelemetryClient(this);
    }
  }
}
