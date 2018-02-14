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

package com.microsoft.applicationinsights.internal.channel;

/**
 * Created by gupele on 12/21/2014.
 */
public interface TransmitterFactory {
	/** 
	 * Creates the {@link TelemetriesTransmitter} for use by the {@link TelemetryChannel}
	 * @param endpoint HTTP Endpoint to send telemetry to
	 * @param maxTransmissionStorageCapacity Max amount of disk space in KB for persistent storage to use
	 * @param throttlingIsEnabled Allow the network telemetry sender to be throttled 
	 * @return The {@link TelemetriesTransmitter} object
	 */
    TelemetriesTransmitter create(String endpoint, String maxTransmissionStorageCapacity, boolean throttlingIsEnabled);
	/** 
	 * Creates the {@link TelemetriesTransmitter} for use by the {@link TelemetryChannel}
	 * @param endpoint HTTP Endpoint to send telemetry to
	 * @param maxTransmissionStorageCapacity Max amount of disk space in KB for persistent storage to use
	 * @param throttlingIsEnabled Allow the network telemetry sender to be throttled 
	 * @param maxInstantRetries Number of instant retries in case of a temporary network outage
	 * @return The {@link TelemetriesTransmitter} object
	 */
    TelemetriesTransmitter create(String endpoint, String maxTransmissionStorageCapacity, boolean throttlingIsEnabled, int maxInstantRetries);
}
