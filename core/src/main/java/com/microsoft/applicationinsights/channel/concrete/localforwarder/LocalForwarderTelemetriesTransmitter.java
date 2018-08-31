package com.microsoft.applicationinsights.channel.concrete.localforwarder;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.microsoft.applicationinsights.internal.channel.TelemetriesTransmitter;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import com.microsoft.localforwarder.library.inputs.contracts.AITelemetryServiceGrpc;
import com.microsoft.localforwarder.library.inputs.contracts.AITelemetryServiceGrpc.AITelemetryServiceStub;
import com.microsoft.localforwarder.library.inputs.contracts.AiResponse;
import com.microsoft.localforwarder.library.inputs.contracts.Telemetry;
import com.microsoft.localforwarder.library.inputs.contracts.TelemetryBatch;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class LocalForwarderTelemetriesTransmitter implements TelemetriesTransmitter<Telemetry> {

    private ManagedChannel channel;
    private AITelemetryServiceStub asyncService;

    private final ScheduledExecutorService executor;
    private final ExecutorService grpcServiceExecutor;

    private StreamObserver<AiResponse> responseObserver = new StreamObserver<AiResponse>() {
        private final LocalForwarderTelemetriesTransmitter thiz = LocalForwarderTelemetriesTransmitter.this;
        @Override
        public void onNext(AiResponse value) {
            trace("Response received: %s", value.toString());
        }

        @Override
        public void onError(Throwable t) {
            error("Error encountered:%n%s", ExceptionUtils.getStackTrace(t));
        }

        @Override
        public void onCompleted() {
            trace("Send completed.");
        }
    };

    @VisibleForTesting
    LocalForwarderTelemetriesTransmitter(ManagedChannelBuilder channelBuilder, boolean createDefaultGrpcExecutor, int instanceId) {
        Preconditions.checkNotNull(channelBuilder, "channelBuilder");

        if (createDefaultGrpcExecutor) {
            this.grpcServiceExecutor = new ThreadPoolExecutor(1, 10, 30, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(),
                    ThreadPoolUtils.createDaemonThreadFactory(LocalForwarderTelemetriesTransmitter.class, String.format("%d-grpcPool", instanceId)));
            channelBuilder.executor(this.grpcServiceExecutor);
        } else {
            this.grpcServiceExecutor = null;
        }
        this.executor = Executors.newScheduledThreadPool(1, ThreadPoolUtils.createDaemonThreadFactory(LocalForwarderTelemetriesTransmitter.class, instanceId));
        this.channel = channelBuilder.build();
        this.asyncService = AITelemetryServiceGrpc.newStub(channel);
    }

    @VisibleForTesting
    void setResponseObserver(StreamObserver<AiResponse> responseObserver) {
        this.responseObserver = responseObserver;
    }

    @Override
    public boolean scheduleSend(final TelemetriesFetcher<Telemetry> telemetriesFetcher, long value, TimeUnit timeUnit) {
        final Collection<Telemetry> telemetries = telemetriesFetcher.fetch();
        try {
            this.executor.schedule(getSenderRunnable(telemetries), value, timeUnit);
            return true;
        } catch (Exception e) {
            error("Error in scheduledSend. %d items not sent", telemetries.size());
        }
        return false;
    }

    private Runnable getSenderRunnable(final Collection<Telemetry> telemetries) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    StreamObserver<TelemetryBatch> requestObserver = asyncService.sendTelemetryBatch(responseObserver);
                    try {
                        requestObserver.onNext(TelemetryBatch.newBuilder().addAllItems(telemetries).build());
                        requestObserver.onCompleted();
                    } catch (ThreadDeath td) {
                        throw td;
                    } catch (Throwable t) {
                        requestObserver.onError(t);
                    }
                } catch (ThreadDeath td) {
                    throw td;
                } catch (Throwable t) {
                    error("Exception issuing sendTelemetryBatch:%n%s", ExceptionUtils.getStackTrace(t));
                }
            }
        };
    }

    @Override
    public boolean sendNow(Collection<Telemetry> telemetries) {
        try {
            this.executor.execute(getSenderRunnable(telemetries));
            return true;
        } catch (Exception e) {
            error("Exception from executor: ", ExceptionUtils.getStackTrace(e));
        }
        return false;
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {
        ThreadPoolUtils.stop(executor, timeout, timeUnit);
        ThreadPoolUtils.stop(grpcServiceExecutor, timeout, timeUnit);
        try {
            if (!channel.shutdown().awaitTermination(timeout, timeUnit)) {
                warn("grpcChannel did not terminate. Attempting forced shutdown.");
                channel.shutdownNow();
            }
            // gRPC has an unereliable thread shutdown process, but it's usually done in under 2 seconds. The await methods don't really work.
            TimeUnit tu = TimeUnit.SECONDS;
            long sleepTime = 2L; // maybe this should be configurable...
            InternalLogger.INSTANCE.trace("Sleeping for %d %s to wait for gRPC threads to terminate.", sleepTime, tu.name());
            tu.sleep(timeout);
        } catch (InterruptedException e) {
            channel.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void error(String format, Object... args) {
        InternalLogger.INSTANCE.error("LocalForwarder: "+format, args);
    }

    private void trace(String format, Object... args) {
        InternalLogger.INSTANCE.trace("LocalForwarder: "+format, args);
    }

    private void warn(String format, Object... args) {
        InternalLogger.INSTANCE.warn("LocalForwarder: "+format, args);
    }
}
