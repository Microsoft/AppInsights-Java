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

package com.microsoft.applicationinsights.internal.shutdown;

import java.io.Closeable;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.ChannelFetcher;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * The class is responsible for all shutdown activities done in the SDK.
 *
 * Created by gupele on 2/2/2015.
 */
public enum SDKShutdownActivity {
    INSTANCE;

    /**
     * An helper class that does the cleanup
     *
     * The following are a MUST and should be kept under any future change:
     * 1. The class should not throw an exception
     * 2. The class 'run' method should exit as soon as possible
     */
    private static class SDKShutdownAction implements Runnable {
        private boolean stopped = false;

        private final List<ChannelFetcher> fetchers = new ArrayList<ChannelFetcher>();
        private final List<Stoppable> stoppables = new ArrayList<Stoppable>();
        private final List<Closeable> closeables = new ArrayList<Closeable>();

        public synchronized void register(ChannelFetcher fetcher) {
            fetchers.add(fetcher);
        }

        public synchronized void register(Stoppable stoppable) {
            stoppables.add(stoppable);
        }

        public synchronized void register(Closeable closeable) {
            closeables.add(closeable);
        }

        @Override
        public synchronized void run() {
            if (stopped) {
                // For making sure the JVM exists ASAP.
                return;
            }

            try {
                stopChannels();
                stopStoppables();
                closeClosables();
            } finally {
                // As the last step, the SDK gracefully closes the Internal Logger
                stopInternalLogger();
            }

            stopped = true;
        }

        /**
         * Make sure no exception is thrown!
         */
        private void stopInternalLogger() {
            try {
                InternalLogger.INSTANCE.stop();
            } catch (Throwable t) {
            }
        }

        /**
         * Make sure no exception is thrown!
         */
        private void stopChannels() {
            for (ChannelFetcher fetcher : fetchers) {
                try {
                    TelemetryChannel channelToStop = fetcher.fetch();
                    if (channelToStop != null) {
                        channelToStop.stop(1L, TimeUnit.SECONDS);
                    }
                } catch (Throwable t) {
                    InternalLogger.INSTANCE.error("Failed to stop channel: '%s'", t.toString());
                    InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(t));
                }
            }
        }
        /**
         * Make sure no exception is thrown!
         */
        private void stopStoppables() {
            for (Stoppable stoppable : stoppables) {
                try {
                    stoppable.stop(1L, TimeUnit.SECONDS);
                } catch (Throwable t) {
                    InternalLogger.INSTANCE.error("Failed to stop stoppable class '%s': '%s'", stoppable.getClass().getName(), t.toString());
                    InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(t));
                }
            }
        }
        private void closeClosables() {
            for (Closeable c : closeables) {
                try {
                    c.close();
                } catch (ThreadDeath td) {
                    throw td;
                } catch (Throwable t) {
                    try {
                        InternalLogger.INSTANCE.error("Failed to close closeable class '%s': %s", c.getClass().getName(), t.toString());
                        InternalLogger.INSTANCE.trace("Stack trace: %s", ExceptionUtils.getStackTrace(t));
                    } catch (ThreadDeath td2) {
                        throw td2;
                    } catch (Throwable t2) {
                        // chomp
                    }
                }
            }
        }
    }

    private static volatile SDKShutdownAction shutdownAction;

    public void register(ChannelFetcher fetcher) {
        getShutdownAction().register(fetcher);
    }

    public void register(Stoppable stoppable) {
        getShutdownAction().register(stoppable);
    }

    public void register(Closeable closable) {
        getShutdownAction().register(closable);
    }

    public void stopAll() {
        getShutdownAction().run();
    }

    private SDKShutdownAction getShutdownAction() {
        if (shutdownAction == null) {
            synchronized (this) {
                if (shutdownAction == null) {
                    try {
                        shutdownAction = new SDKShutdownAction();
                        Runtime.getRuntime().addShutdownHook(new Thread(shutdownAction, SDKShutdownActivity.class.getSimpleName()));
                    } catch (Exception e) {
                        InternalLogger.INSTANCE.error("Error while adding shutdown hook in getShutDownThread call");
                        InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(e));
                    }
                }
            }
        }

        return shutdownAction;
    }
}
