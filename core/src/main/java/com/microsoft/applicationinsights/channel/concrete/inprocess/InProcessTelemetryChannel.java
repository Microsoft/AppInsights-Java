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

package com.microsoft.applicationinsights.channel.concrete.inprocess;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.channel.concrete.TelemetryChannelBase;
import com.microsoft.applicationinsights.internal.channel.ConfiguredTransmitterFactory;
import com.microsoft.applicationinsights.internal.statsbeat.StatsbeatTelemetry;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * An implementation of {@link com.microsoft.applicationinsights.channel.TelemetryChannel}
 *
 * <p>The channel holds two main entities:
 *
 * <p>A buffer for incoming {@link com.microsoft.applicationinsights.telemetry.Telemetry} instances
 * A transmitter
 *
 * <p>The buffer is stores incoming telemetry instances. Every new buffer starts a timer. When the
 * timer expires, or when the buffer is 'full' (whichever happens first), the transmitter will pick
 * up that buffer and will handle its sending to the server. For example, a transmitter will be
 * responsible for compressing, sending and activate a policy in case of failures.
 *
 * <p>The model here is:
 *
 * <p>Use application threads to populate the buffer Use channel's threads to send buffers to the
 * server
 *
 * <p>Created by gupele on 12/17/2014.
 */
public final class InProcessTelemetryChannel extends TelemetryChannelBase<Telemetry> {

    private static final Logger logger = LoggerFactory.getLogger(InProcessTelemetryChannel.class);

    public InProcessTelemetryChannel(TelemetryConfiguration configuration) {
        super(configuration);
    }

    public InProcessTelemetryChannel(TelemetryConfiguration configuration, Map<String, String> channelConfig) {
        super(configuration, channelConfig);
    }

    @Override
    protected boolean doSend(Telemetry telemetry) {
        // this is temporary until we are convinced that telemetry are never re-used by codeless agent
        if (telemetry.previouslyUsed()) {
            throw new IllegalStateException("Telemetry was previously used: " + telemetry);
        }
        // TODO Prepare for AAD support for Statsbeat's iKey
        if (telemetry instanceof StatsbeatTelemetry) {
            statsTelemetryBuffer.add((StatsbeatTelemetry) telemetry);
            logger.debug("############################ add StatsbeatTelemetry to statsTelemetryBuffer");
        } else {
            telemetryBuffer.add(telemetry);
            logger.debug("############################ add regular MetricTelemetry to telemetryBuffer");
        }
        return true;
    }

    @Override
    protected ConfiguredTransmitterFactory<Telemetry> createTransmitterFactory() {
        return new InProcessTelemetryTransmitterFactory();
    }

}
