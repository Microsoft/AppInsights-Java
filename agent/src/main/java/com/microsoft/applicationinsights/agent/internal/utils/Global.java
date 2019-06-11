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

package com.microsoft.applicationinsights.agent.internal.utils;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.extensibility.ContextInitializer;
import com.microsoft.applicationinsights.extensibility.context.CloudContext;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.glowroot.instrumentation.engine.bytecode.api.ThreadContextThreadLocal;

// global state used instead of passing these to various classes (e.g. ThreadContextImpl, SpanImpl) in order
// to reduce memory footprint
public class Global {

    public static boolean isOutboundW3CEnabled;
    public static boolean isOutboundW3CBackCompatEnabled;

    public static boolean isInboundW3CEnabled;

    private static volatile @Nullable TelemetryClient telemetryClient;

    // priority
    // * configured tag
    // * WEBSITE_SITE_NAME
    // * spring.application.name
    // * servlet context root
    private static @Nullable String cloudRole;

    private static final ThreadContextThreadLocal TCTL = new ThreadContextThreadLocal();

    // e.g. the one used by metrics, and user created TelemetryClients
    // this is using map/set that is not thread safe, so must be synchronized appropriately below
    private static final Set<TelemetryContext> otherTelemetryContexts =
            Collections.newSetFromMap(new WeakHashMap<TelemetryContext, Boolean>());

    private Global() {
    }

    public static @Nullable TelemetryClient getTelemetryClient() {
        return telemetryClient;
    }

    public static void setTelemetryClient(TelemetryClient telemetryClient) {
        if (Global.telemetryClient == null) {
            Global.telemetryClient = telemetryClient;
        }
    }

    public static ThreadContextThreadLocal getThreadContextThreadLocal() {
        return TCTL;
    }

    public static ThreadContextThreadLocal.Holder getThreadContextHolder() {
        return TCTL.getHolder();
    }

    // called via bytecode, see SpringApplicationClassFileTransformer
    public static void setCloudRole(@Nullable String cloudRole) {
        if (cloudRole == null) {
            return;
        }
        Global.cloudRole = cloudRole;
        setCloudRole(telemetryClient.getContext().getCloud());
        synchronized (otherTelemetryContexts) {
            for (TelemetryContext context : otherTelemetryContexts) {
                setCloudRole(context.getCloud());
            }
        }
    }

    private static void setCloudRole(CloudContext cloudContext) {
        if (cloudContext.getRole() == null) {
            cloudContext.setRole(cloudRole);
        }
    }

    public static class CloudRoleContextInitializer implements ContextInitializer {

        @Override
        public void initialize(TelemetryContext context) {
            synchronized (otherTelemetryContexts) {
                otherTelemetryContexts.add(context);
            }
            setCloudRole(context.getCloud());
        }
    }
}
