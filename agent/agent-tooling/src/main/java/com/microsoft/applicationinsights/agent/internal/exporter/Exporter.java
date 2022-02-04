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

package com.microsoft.applicationinsights.agent.internal.exporter;

import static io.opentelemetry.api.common.AttributeKey.longKey;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.microsoft.applicationinsights.agent.internal.common.OperationLogger;
import com.microsoft.applicationinsights.agent.internal.common.Strings;
import com.microsoft.applicationinsights.agent.internal.exporter.models.ContextTagKeys;
import com.microsoft.applicationinsights.agent.internal.exporter.models.SeverityLevel;
import com.microsoft.applicationinsights.agent.internal.exporter.models2.ExceptionTelemetry;
import com.microsoft.applicationinsights.agent.internal.exporter.models2.MessageTelemetry;
import com.microsoft.applicationinsights.agent.internal.exporter.models2.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.agent.internal.exporter.models2.RequestTelemetry;
import com.microsoft.applicationinsights.agent.internal.exporter.models2.Telemetry;
import com.microsoft.applicationinsights.agent.internal.telemetry.FormattedDuration;
import com.microsoft.applicationinsights.agent.internal.telemetry.FormattedTime;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryClient;
import com.microsoft.applicationinsights.agent.internal.telemetry.TelemetryUtil;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributeType;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.instrumentation.api.aisdk.AiAppId;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Exporter implements SpanExporter {

  private static final Logger logger = LoggerFactory.getLogger(Exporter.class);

  private static final Set<String> SQL_DB_SYSTEMS;

  private static final Trie<Boolean> STANDARD_ATTRIBUTE_PREFIX_TRIE;

  // TODO (trask) this can go away once new indexer is rolled out to gov clouds
  private static final AttributeKey<List<String>> AI_REQUEST_CONTEXT_KEY =
      AttributeKey.stringArrayKey("http.response.header.request_context");

  public static final AttributeKey<String> AI_OPERATION_NAME_KEY =
      AttributeKey.stringKey("applicationinsights.internal.operation_name");

  private static final AttributeKey<Boolean> AI_LOG_KEY =
      AttributeKey.booleanKey("applicationinsights.internal.log");

  public static final AttributeKey<String> AI_LEGACY_PARENT_ID_KEY =
      AttributeKey.stringKey("applicationinsights.internal.legacy_parent_id");
  public static final AttributeKey<String> AI_LEGACY_ROOT_ID_KEY =
      AttributeKey.stringKey("applicationinsights.internal.legacy_root_id");

  // this is only used by the 2.x web interop bridge
  // for ThreadContext.getRequestTelemetryContext().getRequestTelemetry().setSource()
  private static final AttributeKey<String> AI_SPAN_SOURCE_KEY =
      AttributeKey.stringKey("applicationinsights.internal.source");
  private static final AttributeKey<String> AI_SESSION_ID_KEY =
      AttributeKey.stringKey("applicationinsights.internal.session_id");
  private static final AttributeKey<String> AI_DEVICE_OS_KEY =
      AttributeKey.stringKey("applicationinsights.internal.operating_system");
  private static final AttributeKey<String> AI_DEVICE_OS_VERSION_KEY =
      AttributeKey.stringKey("applicationinsights.internal.operating_system_version");

  private static final AttributeKey<String> AI_LOG_LEVEL_KEY =
      AttributeKey.stringKey("applicationinsights.internal.log_level");
  private static final AttributeKey<String> AI_LOGGER_NAME_KEY =
      AttributeKey.stringKey("applicationinsights.internal.logger_name");
  private static final AttributeKey<String> AI_LOG_ERROR_STACK_KEY =
      AttributeKey.stringKey("applicationinsights.internal.log_error_stack");

  private static final AttributeKey<String> AZURE_NAMESPACE =
      AttributeKey.stringKey("az.namespace");
  private static final AttributeKey<String> AZURE_SDK_PEER_ADDRESS =
      AttributeKey.stringKey("peer.address");
  private static final AttributeKey<String> AZURE_SDK_MESSAGE_BUS_DESTINATION =
      AttributeKey.stringKey("message_bus.destination");
  private static final AttributeKey<Long> AZURE_SDK_ENQUEUED_TIME =
      AttributeKey.longKey("x-opt-enqueued-time");

  private static final AttributeKey<Long> KAFKA_RECORD_QUEUE_TIME_MS =
      longKey("kafka.record.queue_time_ms");
  private static final AttributeKey<Long> KAFKA_OFFSET = longKey("kafka.offset");

  private static final OperationLogger exportingSpanLogger =
      new OperationLogger(Exporter.class, "Exporting span");

  static {
    Set<String> dbSystems = new HashSet<>();
    dbSystems.add(SemanticAttributes.DbSystemValues.DB2);
    dbSystems.add(SemanticAttributes.DbSystemValues.DERBY);
    dbSystems.add(SemanticAttributes.DbSystemValues.MARIADB);
    dbSystems.add(SemanticAttributes.DbSystemValues.MSSQL);
    dbSystems.add(SemanticAttributes.DbSystemValues.MYSQL);
    dbSystems.add(SemanticAttributes.DbSystemValues.ORACLE);
    dbSystems.add(SemanticAttributes.DbSystemValues.POSTGRESQL);
    dbSystems.add(SemanticAttributes.DbSystemValues.SQLITE);
    dbSystems.add(SemanticAttributes.DbSystemValues.OTHER_SQL);
    dbSystems.add(SemanticAttributes.DbSystemValues.HSQLDB);
    dbSystems.add(SemanticAttributes.DbSystemValues.H2);

    SQL_DB_SYSTEMS = Collections.unmodifiableSet(dbSystems);

    // TODO need to keep this list in sync as new semantic conventions are defined
    STANDARD_ATTRIBUTE_PREFIX_TRIE =
        Trie.<Boolean>newBuilder()
            .put("http.", true)
            .put("db.", true)
            .put("message.", true)
            .put("messaging.", true)
            .put("rpc.", true)
            .put("enduser.", true)
            .put("net.", true)
            .put("peer.", true)
            .put("exception.", true)
            .put("thread.", true)
            .put("faas.", true)
            .build();
  }

  private final TelemetryClient telemetryClient;
  private final boolean captureHttpServer4xxAsError;

  public Exporter(TelemetryClient telemetryClient, boolean captureHttpServer4xxAsError) {
    this.telemetryClient = telemetryClient;
    this.captureHttpServer4xxAsError = captureHttpServer4xxAsError;
  }

  @Override
  public CompletableResultCode export(Collection<SpanData> spans) {
    if (Strings.isNullOrEmpty(TelemetryClient.getActive().getInstrumentationKey())) {
      logger.debug("Instrumentation key is null or empty.");
      return CompletableResultCode.ofSuccess();
    }
    boolean failure = false;
    for (SpanData span : spans) {
      logger.debug("exporting span: {}", span);
      try {
        internalExport(span);
        exportingSpanLogger.recordSuccess();
      } catch (Throwable t) {
        exportingSpanLogger.recordFailure(t.getMessage(), t);
        failure = true;
      }
    }
    // batching, retry, throttling, and writing to disk on failure occur downstream
    // for simplicity not reporting back success/failure from this layer
    // only that it was successfully delivered to the next layer
    return failure ? CompletableResultCode.ofFailure() : CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }

  private void internalExport(SpanData span) {
    SpanKind kind = span.getKind();
    String instrumentationName = span.getInstrumentationLibraryInfo().getName();
    telemetryClient
        .getStatsbeatModule()
        .getInstrumentationStatsbeat()
        .addInstrumentation(instrumentationName);
    if (kind == SpanKind.INTERNAL) {
      Boolean isLog = span.getAttributes().get(AI_LOG_KEY);
      if (isLog != null && isLog) {
        exportLogSpan(span);
      } else if (instrumentationName.startsWith("io.opentelemetry.spring-scheduling-")
          && !span.getParentSpanContext().isValid()) {
        // TODO (trask) AI mapping: need semantic convention for determining whether to map INTERNAL
        // to request or dependency (or need clarification to use SERVER for this)
        exportRequest(span);
      } else {
        exportRemoteDependency(span, true);
      }
    } else if (kind == SpanKind.CLIENT || kind == SpanKind.PRODUCER) {
      exportRemoteDependency(span, false);
    } else if (kind == SpanKind.CONSUMER
        && "receive".equals(span.getAttributes().get(SemanticAttributes.MESSAGING_OPERATION))) {
      exportRemoteDependency(span, false);
    } else if (kind == SpanKind.SERVER || kind == SpanKind.CONSUMER) {
      exportRequest(span);
    } else {
      throw new UnsupportedOperationException(kind.name());
    }
  }

  private void exportRemoteDependency(SpanData span, boolean inProc) {
    RemoteDependencyTelemetry telemetry = telemetryClient.newRemoteDependencyTelemetry();

    float samplingPercentage = getSamplingPercentage(span.getSpanContext().getTraceState());

    // set standard properties
    setOperationTags(telemetry, span);
    setTime(telemetry, span.getStartEpochNanos());
    setSampleRate(telemetry, samplingPercentage);
    setExtraAttributes(telemetry, span.getAttributes());
    addLinks(telemetry, span.getLinks());

    // set dependency-specific properties
    telemetry.setId(span.getSpanId());
    telemetry.setName(getDependencyName(span));
    telemetry.setDuration(
        FormattedDuration.fromNanos(span.getEndEpochNanos() - span.getStartEpochNanos()));
    telemetry.setSuccess(getSuccess(span));

    if (inProc) {
      telemetry.setType("InProc");
    } else {
      applySemanticConventions(span, telemetry);
    }

    // export
    telemetryClient.trackAsync(telemetry);
    exportEvents(span, null, samplingPercentage);
  }

  private static final Set<String> DEFAULT_HTTP_SPAN_NAMES =
      new HashSet<>(
          Arrays.asList(
              "HTTP OPTIONS",
              "HTTP GET",
              "HTTP HEAD",
              "HTTP POST",
              "HTTP PUT",
              "HTTP DELETE",
              "HTTP TRACE",
              "HTTP CONNECT",
              "HTTP PATCH"));

  // the backend product prefers more detailed (but possibly infinite cardinality) name for http
  // dependencies
  private static String getDependencyName(SpanData span) {
    String name = span.getName();

    String method = span.getAttributes().get(SemanticAttributes.HTTP_METHOD);
    if (method == null) {
      return name;
    }

    if (!DEFAULT_HTTP_SPAN_NAMES.contains(name)) {
      return name;
    }

    String url = span.getAttributes().get(SemanticAttributes.HTTP_URL);
    if (url == null) {
      return name;
    }

    String path = UrlParser.getPathFromUrl(url);
    if (path == null) {
      return name;
    }
    return path.isEmpty() ? method + " /" : method + " " + path;
  }

  private static void applySemanticConventions(SpanData span, RemoteDependencyTelemetry telemetry) {
    Attributes attributes = span.getAttributes();
    String httpMethod = attributes.get(SemanticAttributes.HTTP_METHOD);
    if (httpMethod != null) {
      applyHttpClientSpan(attributes, telemetry);
      return;
    }
    String rpcSystem = attributes.get(SemanticAttributes.RPC_SYSTEM);
    if (rpcSystem != null) {
      applyRpcClientSpan(attributes, telemetry, rpcSystem);
      return;
    }
    String dbSystem = attributes.get(SemanticAttributes.DB_SYSTEM);
    if (dbSystem != null) {
      applyDatabaseClientSpan(attributes, telemetry, dbSystem);
      return;
    }
    String azureNamespace = attributes.get(AZURE_NAMESPACE);
    if ("Microsoft.EventHub".equals(azureNamespace)) {
      applyEventHubsSpan(attributes, telemetry);
      return;
    }
    if ("Microsoft.ServiceBus".equals(azureNamespace)) {
      applyServiceBusSpan(attributes, telemetry);
      return;
    }
    String messagingSystem = attributes.get(SemanticAttributes.MESSAGING_SYSTEM);
    if (messagingSystem != null) {
      applyMessagingClientSpan(attributes, telemetry, messagingSystem, span.getKind());
      return;
    }

    // passing max value because we don't know what the default port would be in this case,
    // so we always want the port included
    String target = getTargetFromPeerAttributes(attributes, Integer.MAX_VALUE);
    if (target != null) {
      telemetry.setTarget(target);
      return;
    }

    // with no target, the App Map falls back to creating a node based on the telemetry name,
    // which is very confusing, e.g. when multiple unrelated nodes all point to a single node
    // because they had dependencies with the same telemetry name
    //
    // so we mark these as InProc, even though they aren't INTERNAL spans,
    // in order to prevent App Map from considering them
    telemetry.setType("InProc");
  }

  private void exportLogSpan(SpanData span) {
    String errorStack = span.getAttributes().get(AI_LOG_ERROR_STACK_KEY);
    if (errorStack == null) {
      trackMessage(span);
    } else {
      trackTraceAsException(span, errorStack);
    }
  }

  private void trackMessage(SpanData span) {
    MessageTelemetry telemetry = telemetryClient.newMessageTelemetry();

    Attributes attributes = span.getAttributes();

    // set standard properties
    setTime(telemetry, span.getStartEpochNanos());
    setOperationTags(telemetry, span);
    setSampleRate(telemetry, span);
    setExtraAttributes(telemetry, attributes);

    // set message-specific properties
    String level = attributes.get(AI_LOG_LEVEL_KEY);
    String loggerName = attributes.get(AI_LOGGER_NAME_KEY);
    String threadName = attributes.get(SemanticAttributes.THREAD_NAME);

    telemetry.setSeverityLevel(toSeverityLevel(level));
    telemetry.setMessage(span.getName());

    setLoggerProperties(telemetry, level, loggerName, threadName);

    // export
    telemetryClient.trackAsync(telemetry);
  }

  private void trackTraceAsException(SpanData span, String errorStack) {
    ExceptionTelemetry telemetry = telemetryClient.newExceptionTelemetry();

    Attributes attributes = span.getAttributes();

    // set standard properties
    setOperationTags(telemetry, span);
    setTime(telemetry, span.getStartEpochNanos());
    setSampleRate(telemetry, span);
    setExtraAttributes(telemetry, attributes);

    // set exception-specific properties
    String level = attributes.get(AI_LOG_LEVEL_KEY);
    String loggerName = attributes.get(AI_LOGGER_NAME_KEY);
    String threadName = attributes.get(SemanticAttributes.THREAD_NAME);

    telemetry.setExceptions(Exceptions.minimalParse(errorStack));
    telemetry.setSeverityLevel(toSeverityLevel(level));
    telemetry.addProperty("Logger Message", span.getName());
    setLoggerProperties(telemetry, level, loggerName, threadName);

    // export
    telemetryClient.trackAsync(telemetry);
  }

  private static void setOperationTags(Telemetry telemetry, SpanData span) {
    setOperationId(telemetry, span.getTraceId());
    setOperationParentId(telemetry, span.getParentSpanContext().getSpanId());
    setOperationName(telemetry, span.getAttributes());
  }

  private static void setOperationId(Telemetry telemetry, String traceId) {
    telemetry.addTag(ContextTagKeys.AI_OPERATION_ID.toString(), traceId);
  }

  private static void setOperationParentId(Telemetry telemetry, String parentSpanId) {
    if (SpanId.isValid(parentSpanId)) {
      telemetry.addTag(ContextTagKeys.AI_OPERATION_PARENT_ID.toString(), parentSpanId);
    }
  }

  private static void setOperationName(Telemetry telemetry, Attributes attributes) {
    String operationName = attributes.get(AI_OPERATION_NAME_KEY);
    if (operationName != null) {
      setOperationName(telemetry, operationName);
    }
  }

  private static void setOperationName(Telemetry telemetry, String operationName) {
    telemetry.addTag(ContextTagKeys.AI_OPERATION_NAME.toString(), operationName);
  }

  private static void setLoggerProperties(
      Telemetry telemetry, String level, String loggerName, String threadName) {
    if (level != null) {
      // TODO are these needed? level is already reported as severityLevel, sourceType maybe needed
      // for exception telemetry only?
      telemetry.addProperty("SourceType", "Logger");
      telemetry.addProperty("LoggingLevel", level);
    }
    if (loggerName != null) {
      telemetry.addProperty("LoggerName", loggerName);
    }
    if (threadName != null) {
      telemetry.addProperty("ThreadName", threadName);
    }
  }

  private static void applyHttpClientSpan(
      Attributes attributes, RemoteDependencyTelemetry telemetry) {

    String target = getTargetForHttpClientSpan(attributes);

    String targetAppId = getTargetAppId(attributes);

    if (targetAppId == null || AiAppId.getAppId().equals(targetAppId)) {
      telemetry.setType("Http");
      telemetry.setTarget(target);
    } else {
      // using "Http (tracked component)" is important for dependencies that go cross-component
      // (have an appId in their target field)
      // if you use just HTTP, Breeze will remove appid from the target
      // TODO (trask) remove this once confirmed by zakima that it is no longer needed
      telemetry.setType("Http (tracked component)");
      telemetry.setTarget(target + " | " + targetAppId);
    }

    Long httpStatusCode = attributes.get(SemanticAttributes.HTTP_STATUS_CODE);
    if (httpStatusCode != null) {
      telemetry.setResultCode(Long.toString(httpStatusCode));
    }

    String url = attributes.get(SemanticAttributes.HTTP_URL);
    telemetry.setData(url);
  }

  @Nullable
  private static String getTargetAppId(Attributes attributes) {
    List<String> requestContextList = attributes.get(AI_REQUEST_CONTEXT_KEY);
    if (requestContextList == null || requestContextList.isEmpty()) {
      return null;
    }
    String requestContext = requestContextList.get(0);
    int index = requestContext.indexOf('=');
    if (index == -1) {
      return null;
    }
    return requestContext.substring(index + 1);
  }

  private static String getTargetForHttpClientSpan(Attributes attributes) {
    // from the spec, at least one of the following sets of attributes is required:
    // * http.url
    // * http.scheme, http.host, http.target
    // * http.scheme, net.peer.name, net.peer.port, http.target
    // * http.scheme, net.peer.ip, net.peer.port, http.target
    String target = getTargetFromPeerService(attributes);
    if (target != null) {
      return target;
    }
    // note http.host includes the port (at least when non-default)
    target = attributes.get(SemanticAttributes.HTTP_HOST);
    if (target != null) {
      String scheme = attributes.get(SemanticAttributes.HTTP_SCHEME);
      if ("http".equals(scheme)) {
        if (target.endsWith(":80")) {
          target = target.substring(0, target.length() - 3);
        }
      } else if ("https".equals(scheme)) {
        if (target.endsWith(":443")) {
          target = target.substring(0, target.length() - 4);
        }
      }
      return target;
    }
    String url = attributes.get(SemanticAttributes.HTTP_URL);
    if (url != null) {
      target = UrlParser.getTargetFromUrl(url);
      if (target != null) {
        return target;
      }
    }
    String scheme = attributes.get(SemanticAttributes.HTTP_SCHEME);
    int defaultPort;
    if ("http".equals(scheme)) {
      defaultPort = 80;
    } else if ("https".equals(scheme)) {
      defaultPort = 443;
    } else {
      defaultPort = 0;
    }
    target = getTargetFromNetAttributes(attributes, defaultPort);
    if (target != null) {
      return target;
    }
    // this should not happen, just a failsafe
    return "Http";
  }

  @Nullable
  private static String getTargetFromPeerAttributes(Attributes attributes, int defaultPort) {
    String target = getTargetFromPeerService(attributes);
    if (target != null) {
      return target;
    }
    return getTargetFromNetAttributes(attributes, defaultPort);
  }

  @Nullable
  private static String getTargetFromPeerService(Attributes attributes) {
    // do not append port to peer.service
    return attributes.get(SemanticAttributes.PEER_SERVICE);
  }

  @Nullable
  private static String getTargetFromNetAttributes(Attributes attributes, int defaultPort) {
    String target = getHostFromNetAttributes(attributes);
    if (target == null) {
      return null;
    }
    // append net.peer.port to target
    Long port = attributes.get(SemanticAttributes.NET_PEER_PORT);
    if (port != null && port != defaultPort) {
      return target + ":" + port;
    }
    return target;
  }

  @Nullable
  private static String getHostFromNetAttributes(Attributes attributes) {
    String host = attributes.get(SemanticAttributes.NET_PEER_NAME);
    if (host != null) {
      return host;
    }
    return attributes.get(SemanticAttributes.NET_PEER_IP);
  }

  private static void applyRpcClientSpan(
      Attributes attributes, RemoteDependencyTelemetry telemetry, String rpcSystem) {
    telemetry.setType(rpcSystem);
    String target = getTargetFromPeerAttributes(attributes, 0);
    // not appending /rpc.service for now since that seems too fine-grained
    if (target == null) {
      target = rpcSystem;
    }
    telemetry.setTarget(target);
  }

  private static void applyDatabaseClientSpan(
      Attributes attributes, RemoteDependencyTelemetry telemetry, String dbSystem) {
    String dbStatement = attributes.get(SemanticAttributes.DB_STATEMENT);
    if (dbStatement == null) {
      dbStatement = attributes.get(SemanticAttributes.DB_OPERATION);
    }
    String type;
    if (SQL_DB_SYSTEMS.contains(dbSystem)) {
      if (dbSystem.equals(SemanticAttributes.DbSystemValues.MYSQL)) {
        type = "mysql"; // this has special icon in portal
      } else if (dbSystem.equals(SemanticAttributes.DbSystemValues.POSTGRESQL)) {
        type = "postgresql"; // this has special icon in portal
      } else {
        type = "SQL";
      }
    } else {
      type = dbSystem;
    }
    telemetry.setType(type);
    telemetry.setData(dbStatement);
    String target =
        nullAwareConcat(
            getTargetFromPeerAttributes(attributes, getDefaultPortForDbSystem(dbSystem)),
            attributes.get(SemanticAttributes.DB_NAME),
            " | ");
    if (target == null) {
      target = dbSystem;
    }
    telemetry.setTarget(target);
  }

  private static void applyMessagingClientSpan(
      Attributes attributes,
      RemoteDependencyTelemetry telemetry,
      String messagingSystem,
      SpanKind spanKind) {
    if (spanKind == SpanKind.PRODUCER) {
      telemetry.setType("Queue Message | " + messagingSystem);
    } else {
      // e.g. CONSUMER kind (without remote parent) and CLIENT kind
      telemetry.setType(messagingSystem);
    }
    String destination = attributes.get(SemanticAttributes.MESSAGING_DESTINATION);
    if (destination != null) {
      telemetry.setTarget(destination);
    } else {
      telemetry.setTarget(messagingSystem);
    }
  }

  // special case needed until Azure SDK moves to OTel semantic conventions
  private static void applyEventHubsSpan(
      Attributes attributes, RemoteDependencyTelemetry telemetry) {
    telemetry.setType("Microsoft.EventHub");
    telemetry.setTarget(getAzureSdkTargetSource(attributes));
  }

  // special case needed until Azure SDK moves to OTel semantic conventions
  private static void applyServiceBusSpan(
      Attributes attributes, RemoteDependencyTelemetry telemetry) {
    // TODO(trask) change this to Microsoft.ServiceBus once that is supported in U/X E2E view
    telemetry.setType("AZURE SERVICE BUS");
    telemetry.setTarget(getAzureSdkTargetSource(attributes));
  }

  private static String getAzureSdkTargetSource(Attributes attributes) {
    String peerAddress = attributes.get(AZURE_SDK_PEER_ADDRESS);
    String destination = attributes.get(AZURE_SDK_MESSAGE_BUS_DESTINATION);
    return peerAddress + "/" + destination;
  }

  private static int getDefaultPortForDbSystem(String dbSystem) {
    // jdbc default ports are from
    // io.opentelemetry.javaagent.instrumentation.jdbc.JdbcConnectionUrlParser
    // TODO (trask) make the ports constants (at least in JdbcConnectionUrlParser) so they can be
    // used here
    switch (dbSystem) {
      case SemanticAttributes.DbSystemValues.MONGODB:
        return 27017;
      case SemanticAttributes.DbSystemValues.CASSANDRA:
        return 9042;
      case SemanticAttributes.DbSystemValues.REDIS:
        return 6379;
      case SemanticAttributes.DbSystemValues.MARIADB:
      case SemanticAttributes.DbSystemValues.MYSQL:
        return 3306;
      case SemanticAttributes.DbSystemValues.MSSQL:
        return 1433;
      case SemanticAttributes.DbSystemValues.DB2:
        return 50000;
      case SemanticAttributes.DbSystemValues.ORACLE:
        return 1521;
      case SemanticAttributes.DbSystemValues.H2:
        return 8082;
      case SemanticAttributes.DbSystemValues.DERBY:
        return 1527;
      case SemanticAttributes.DbSystemValues.POSTGRESQL:
        return 5432;
      default:
        return 0;
    }
  }

  private void exportRequest(SpanData span) {
    RequestTelemetry telemetry = telemetryClient.newRequestTelemetry();

    Attributes attributes = span.getAttributes();
    long startEpochNanos = span.getStartEpochNanos();
    float samplingPercentage = getSamplingPercentage(span.getSpanContext().getTraceState());

    // set standard properties
    telemetry.setId(span.getSpanId());
    setTime(telemetry, startEpochNanos);
    setSampleRate(telemetry, samplingPercentage);
    setExtraAttributes(telemetry, attributes);
    addLinks(telemetry, span.getLinks());

    String operationName = getOperationName(span);
    telemetry.addTag(ContextTagKeys.AI_OPERATION_NAME.toString(), operationName);
    telemetry.addTag(ContextTagKeys.AI_OPERATION_ID.toString(), span.getTraceId());

    // see behavior specified at https://github.com/microsoft/ApplicationInsights-Java/issues/1174
    String aiLegacyParentId = span.getAttributes().get(AI_LEGACY_PARENT_ID_KEY);
    if (aiLegacyParentId != null) {
      // this was the real (legacy) parent id, but it didn't fit span id format
      telemetry.addTag(ContextTagKeys.AI_OPERATION_PARENT_ID.toString(), aiLegacyParentId);
    } else if (span.getParentSpanContext().isValid()) {
      telemetry.addTag(
          ContextTagKeys.AI_OPERATION_PARENT_ID.toString(),
          span.getParentSpanContext().getSpanId());
    }
    String aiLegacyRootId = span.getAttributes().get(AI_LEGACY_ROOT_ID_KEY);
    if (aiLegacyRootId != null) {
      telemetry.addTag("ai_legacyRootID", aiLegacyRootId);
    }

    // set request-specific properties
    telemetry.setName(operationName);
    telemetry.setDuration(FormattedDuration.fromNanos(span.getEndEpochNanos() - startEpochNanos));
    telemetry.setSuccess(getSuccess(span));

    String httpUrl = getHttpUrlFromServerSpan(attributes);
    if (httpUrl != null) {
      telemetry.setUrl(httpUrl);
    }

    Long httpStatusCode = attributes.get(SemanticAttributes.HTTP_STATUS_CODE);
    if (httpStatusCode == null) {
      httpStatusCode = attributes.get(SemanticAttributes.RPC_GRPC_STATUS_CODE);
    }
    if (httpStatusCode != null) {
      telemetry.setResponseCode(Long.toString(httpStatusCode));
    } else {
      telemetry.setResponseCode("0");
    }

    String locationIp = attributes.get(SemanticAttributes.HTTP_CLIENT_IP);
    if (locationIp == null) {
      // only use net.peer.ip if http.client_ip is not available
      locationIp = attributes.get(SemanticAttributes.NET_PEER_IP);
    }
    if (locationIp != null) {
      telemetry.addTag(ContextTagKeys.AI_LOCATION_IP.toString(), locationIp);
    }

    telemetry.setSource(getSource(attributes, span.getSpanContext()));

    String sessionId = attributes.get(AI_SESSION_ID_KEY);
    if (sessionId != null) {
      // this is only used by the 2.x web interop bridge for
      // ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getContext().getSession().setId()
      telemetry.addTag(ContextTagKeys.AI_SESSION_ID.toString(), sessionId);
    }
    String deviceOs = attributes.get(AI_DEVICE_OS_KEY);
    if (deviceOs != null) {
      // this is only used by the 2.x web interop bridge for
      // ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getContext().getDevice().setOperatingSystem()
      telemetry.addTag(ContextTagKeys.AI_DEVICE_OS.toString(), deviceOs);
    }
    String deviceOsVersion = attributes.get(AI_DEVICE_OS_VERSION_KEY);
    if (deviceOsVersion != null) {
      // this is only used by the 2.x web interop bridge for
      // ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().getContext().getDevice().setOperatingSystemVersion()
      telemetry.addTag(ContextTagKeys.AI_DEVICE_OS_VERSION.toString(), deviceOsVersion);
    }

    // TODO(trask)? for batch consumer, enqueuedTime should be the average of this attribute
    //  across all links
    Long enqueuedTime = attributes.get(AZURE_SDK_ENQUEUED_TIME);
    if (enqueuedTime != null) {
      long timeSinceEnqueuedMillis =
          Math.max(
              0L, NANOSECONDS.toMillis(span.getStartEpochNanos()) - SECONDS.toMillis(enqueuedTime));
      telemetry.addMeasurement("timeSinceEnqueued", (double) timeSinceEnqueuedMillis);
    }
    Long timeSinceEnqueuedMillis = attributes.get(KAFKA_RECORD_QUEUE_TIME_MS);
    if (timeSinceEnqueuedMillis != null) {
      telemetry.addMeasurement("timeSinceEnqueued", (double) timeSinceEnqueuedMillis);
    }

    // export
    telemetryClient.trackAsync(telemetry);
    exportEvents(span, operationName, samplingPercentage);
  }

  private boolean getSuccess(SpanData span) {
    switch (span.getStatus().getStatusCode()) {
      case ERROR:
        return false;
      case OK:
        // instrumentation never sets OK, so this is explicit user override
        return true;
      case UNSET:
        if (captureHttpServer4xxAsError) {
          Long statusCode = span.getAttributes().get(SemanticAttributes.HTTP_STATUS_CODE);
          return statusCode == null || statusCode < 400;
        }
        return true;
    }
    return true;
  }

  @Nullable
  public static String getHttpUrlFromServerSpan(Attributes attributes) {
    String httpUrl = attributes.get(SemanticAttributes.HTTP_URL);
    if (httpUrl != null) {
      return httpUrl;
    }
    String scheme = attributes.get(SemanticAttributes.HTTP_SCHEME);
    if (scheme == null) {
      return null;
    }
    String host = attributes.get(SemanticAttributes.HTTP_HOST);
    if (host == null) {
      return null;
    }
    String target = attributes.get(SemanticAttributes.HTTP_TARGET);
    if (target == null) {
      return null;
    }
    return scheme + "://" + host + target;
  }

  @Nullable
  private static String getSource(Attributes attributes, SpanContext spanContext) {
    // this is only used by the 2.x web interop bridge
    // for ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry().setSource()
    String source = attributes.get(AI_SPAN_SOURCE_KEY);
    if (source != null) {
      return source;
    }

    source = spanContext.getTraceState().get("az");

    if (source != null && !AiAppId.getAppId().equals(source)) {
      return source;
    }
    if (isAzureQueue(attributes)) {
      return getAzureSdkTargetSource(attributes);
    }
    String messagingSystem = attributes.get(SemanticAttributes.MESSAGING_SYSTEM);
    if (messagingSystem != null) {
      // TODO (trask) AI mapping: should this pass default port for messaging.system?
      source =
          nullAwareConcat(
              getTargetFromPeerAttributes(attributes, 0),
              attributes.get(SemanticAttributes.MESSAGING_DESTINATION),
              "/");
      if (source != null) {
        return source;
      }
      // fallback
      return messagingSystem;
    }
    return null;
  }

  private static boolean isAzureQueue(Attributes attributes) {
    String azureNamespace = attributes.get(AZURE_NAMESPACE);
    return "Microsoft.EventHub".equals(azureNamespace)
        || "Microsoft.ServiceBus".equals(azureNamespace);
  }

  private static String getOperationName(SpanData span) {
    String spanName = span.getName();
    String httpMethod = span.getAttributes().get(SemanticAttributes.HTTP_METHOD);
    if (httpMethod != null && !httpMethod.isEmpty() && spanName.startsWith("/")) {
      return httpMethod + " " + spanName;
    }
    return spanName;
  }

  private static String nullAwareConcat(String str1, String str2, String separator) {
    if (str1 == null) {
      return str2;
    }
    if (str2 == null) {
      return str1;
    }
    return str1 + separator + str2;
  }

  private void exportEvents(
      SpanData span, @Nullable String operationName, float samplingPercentage) {
    for (EventData event : span.getEvents()) {
      String instrumentationLibraryName = span.getInstrumentationLibraryInfo().getName();
      boolean lettuce51 = instrumentationLibraryName.equals("io.opentelemetry.lettuce-5.1");
      if (lettuce51 && event.getName().startsWith("redis.encode.")) {
        // special case as these are noisy and come from the underlying library itself
        continue;
      }
      boolean grpc16 = instrumentationLibraryName.equals("io.opentelemetry.grpc-1.6");
      if (grpc16 && event.getName().equals("message")) {
        // OpenTelemetry semantic conventions define semi-noisy grpc events
        // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/semantic_conventions/rpc.md#events
        //
        // we want to suppress these (at least by default)
        continue;
      }

      if (event.getAttributes().get(SemanticAttributes.EXCEPTION_TYPE) != null
          || event.getAttributes().get(SemanticAttributes.EXCEPTION_MESSAGE) != null) {
        // TODO (trask) map OpenTelemetry exception to Application Insights exception better
        String stacktrace = event.getAttributes().get(SemanticAttributes.EXCEPTION_STACKTRACE);
        if (stacktrace != null) {
          trackException(stacktrace, span, operationName, samplingPercentage);
        }
        return;
      }

      MessageTelemetry telemetry = telemetryClient.newMessageTelemetry();

      // set standard properties
      setOperationId(telemetry, span.getTraceId());
      setOperationParentId(telemetry, span.getSpanId());
      if (operationName != null) {
        setOperationName(telemetry, operationName);
      } else {
        setOperationName(telemetry, span.getAttributes());
      }
      setTime(telemetry, event.getEpochNanos());
      setExtraAttributes(telemetry, event.getAttributes());
      setSampleRate(telemetry, samplingPercentage);

      // set message-specific properties
      telemetry.setMessage(event.getName());

      telemetryClient.trackAsync(telemetry);
    }
  }

  private void trackException(
      String errorStack, SpanData span, @Nullable String operationName, float samplingPercentage) {
    ExceptionTelemetry telemetry = telemetryClient.newExceptionTelemetry();

    // set standard properties
    setOperationId(telemetry, span.getTraceId());
    setOperationParentId(telemetry, span.getSpanId());
    if (operationName != null) {
      setOperationName(telemetry, operationName);
    } else {
      setOperationName(telemetry, span.getAttributes());
    }
    setTime(telemetry, span.getEndEpochNanos());
    setSampleRate(telemetry, samplingPercentage);

    // set exception-specific properties
    telemetry.setExceptions(Exceptions.minimalParse(errorStack));

    telemetryClient.trackAsync(telemetry);
  }

  private static void setTime(Telemetry telemetry, long epochNanos) {
    telemetry.setTime(FormattedTime.offSetDateTimeFromEpochNanos(epochNanos));
  }

  private static void setSampleRate(Telemetry telemetry, SpanData span) {
    setSampleRate(telemetry, getSamplingPercentage(span.getSpanContext().getTraceState()));
  }

  private static void setSampleRate(Telemetry telemetry, float samplingPercentage) {
    if (samplingPercentage != 100) {
      telemetry.setSampleRate(samplingPercentage);
    }
  }

  private static float getSamplingPercentage(TraceState traceState) {
    return TelemetryUtil.getSamplingPercentage(traceState, 100, true);
  }

  private static void addLinks(Telemetry telemetry, List<LinkData> links) {
    if (links.isEmpty()) {
      return;
    }
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    boolean first = true;
    for (LinkData link : links) {
      if (!first) {
        sb.append(",");
      }
      sb.append("{\"operation_Id\":\"");
      sb.append(link.getSpanContext().getTraceId());
      sb.append("\",\"id\":\"");
      sb.append(link.getSpanContext().getSpanId());
      sb.append("\"}");
      first = false;
    }
    sb.append("]");
    telemetry.addProperty("_MS.links", sb.toString());
  }

  private static void setExtraAttributes(Telemetry telemetry, Attributes attributes) {
    attributes.forEach(
        (key, value) -> {
          String stringKey = key.getKey();
          if (stringKey.startsWith("applicationinsights.internal.")) {
            return;
          }
          if (stringKey.equals(AZURE_NAMESPACE.getKey())
              || stringKey.equals(AZURE_SDK_MESSAGE_BUS_DESTINATION.getKey())
              || stringKey.equals(AZURE_SDK_ENQUEUED_TIME.getKey())) {
            // these are from azure SDK (AZURE_SDK_PEER_ADDRESS gets filtered out automatically
            // since it uses the otel "peer." prefix)
            return;
          }
          if (stringKey.equals(KAFKA_RECORD_QUEUE_TIME_MS.getKey())
              || stringKey.equals(KAFKA_OFFSET.getKey())) {
            return;
          }
          if (stringKey.equals(AI_REQUEST_CONTEXT_KEY.getKey())) {
            return;
          }
          // special case mappings
          if (stringKey.equals(SemanticAttributes.ENDUSER_ID.getKey()) && value instanceof String) {
            telemetry.addTag(ContextTagKeys.AI_USER_ID.toString(), (String) value);
            return;
          }
          if (stringKey.equals(SemanticAttributes.HTTP_USER_AGENT.getKey())
              && value instanceof String) {
            telemetry.addTag("ai.user.userAgent", (String) value);
            return;
          }
          if (stringKey.equals("ai.preview.instrumentation_key") && value instanceof String) {
            telemetry.setInstrumentationKey((String) value);
            return;
          }
          if (stringKey.equals("ai.preview.service_name") && value instanceof String) {
            telemetry.addTag(ContextTagKeys.AI_CLOUD_ROLE.toString(), (String) value);
            return;
          }
          if (stringKey.equals("ai.preview.service_instance_id") && value instanceof String) {
            telemetry.addTag(ContextTagKeys.AI_CLOUD_ROLE_INSTANCE.toString(), (String) value);
            return;
          }
          if (stringKey.equals("ai.preview.service_version") && value instanceof String) {
            telemetry.addTag(ContextTagKeys.AI_APPLICATION_VER.toString(), (String) value);
            return;
          }
          if (STANDARD_ATTRIBUTE_PREFIX_TRIE.getOrDefault(stringKey, false)
              && !stringKey.startsWith("http.request.header.")
              && !stringKey.startsWith("http.response.header.")) {
            return;
          }
          String val = convertToString(value, key.getType());
          if (value != null) {
            telemetry.addProperty(key.getKey(), val);
          }
        });
  }

  @Nullable
  private static String convertToString(Object value, AttributeType type) {
    switch (type) {
      case STRING:
      case BOOLEAN:
      case LONG:
      case DOUBLE:
        return String.valueOf(value);
      case STRING_ARRAY:
      case BOOLEAN_ARRAY:
      case LONG_ARRAY:
      case DOUBLE_ARRAY:
        return join((List<?>) value);
    }
    logger.warn("unexpected attribute type: {}", type);
    return null;
  }

  private static <T> String join(List<T> values) {
    StringBuilder sb = new StringBuilder();
    for (Object val : values) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(val);
    }
    return sb.toString();
  }

  @Nullable
  private static SeverityLevel toSeverityLevel(String level) {
    if (level == null) {
      return null;
    }
    switch (level) {
      case "FATAL":
        return SeverityLevel.CRITICAL;
      case "ERROR":
      case "SEVERE":
        return SeverityLevel.ERROR;
      case "WARN":
      case "WARNING":
        return SeverityLevel.WARNING;
      case "INFO":
        return SeverityLevel.INFORMATION;
      case "DEBUG":
      case "TRACE":
      case "CONFIG":
      case "FINE":
      case "FINER":
      case "FINEST":
      case "ALL":
        return SeverityLevel.VERBOSE;
      default:
        // TODO (trask) AI mapping: is this a good fallback?
        logger.debug("Unexpected level {}, using VERBOSE level as default", level);
        return SeverityLevel.VERBOSE;
    }
  }
}
