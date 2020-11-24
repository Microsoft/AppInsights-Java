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

package com.microsoft.applicationinsights.agent.internal;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.DefaultContextPropagators;
import io.opentelemetry.instrumentation.api.aiappid.AiHttpTraceContext;
import io.opentelemetry.instrumentation.api.aiconnectionstring.AiConnectionString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionStringAccessor implements AiConnectionString.Accessor {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionStringAccessor.class);

    @Override
    public boolean hasValue() {
        // check for instrumentation key value is sufficient here because it's updated each time when the connection string is updated.
        String instrumentationKey = TelemetryConfiguration.getActive().getInstrumentationKey();
        return !Strings.isNullOrEmpty(instrumentationKey);
    }

    @Override
    public void setValue(String value) {
        if (!Strings.isNullOrEmpty(value)) {
            TelemetryConfiguration.getActive().setConnectionString(value);
            // now that we know user has opted in to tracing, need to set up propagator
            OpenTelemetry.setGlobalPropagators(
                    DefaultContextPropagators.builder().addTextMapPropagator(AiHttpTraceContext.getInstance()).build());
            logger.info("Set connection string lazily for the Azure Function Consumption Plan.");
        }
    }
}
