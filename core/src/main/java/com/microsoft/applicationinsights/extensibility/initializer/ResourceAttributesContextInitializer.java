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


package com.microsoft.applicationinsights.extensibility.initializer;

import java.util.Map;

import org.apache.commons.text.StringSubstitutor;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;

/**
 * Initializer for SDK version.
 */
public final class ResourceAttributesContextInitializer implements ContextInitializer {

    private final Map<String, String> resourceAttributes;
    private final StringSubstitutor substitutor = new StringSubstitutor(System.getenv());

    public ResourceAttributesContextInitializer(Map<String, String> resourceAttributes) {
        this.resourceAttributes = resourceAttributes;
    }

    @Override
    public void initialize(TelemetryContext context) {
        for (Map.Entry<String, String> entry: resourceAttributes.entrySet()) {
            String key = entry.getKey();
            if (key.equals("service.version")) {
                context.getComponent().setVersion(substitutor.replace(entry.getValue()));
            } else {
                context.getProperties().put(key, substitutor.replace(entry.getValue()));
            }
        }
    }
}
