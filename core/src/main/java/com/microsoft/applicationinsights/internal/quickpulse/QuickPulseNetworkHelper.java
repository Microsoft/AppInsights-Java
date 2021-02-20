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

package com.microsoft.applicationinsights.internal.quickpulse;

import java.util.Date;

import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;

/**
 * Created by gupele on 12/12/2016.
 */
final class QuickPulseNetworkHelper {
    private final static long TICKS_AT_EPOCH = 621355968000000000L;
    private static final String HEADER_TRANSMISSION_TIME = "x-ms-qps-transmission-time";
    private final static String QP_STATUS_HEADER = "x-ms-qps-subscribed";
    private final static String QPS_SERVICE_POLLING_INTERVAL_HINT  = "x-ms-qps-service-polling-interval-hint";
    private final static String QPS_SERVICE_ENDPOINT_REDIRECT   = "x-ms-qps-service-endpoint-redirect";

    public HttpPost buildRequest(Date currentDate, String address) {
        final long ticks = currentDate.getTime() * 10000 + TICKS_AT_EPOCH;

        HttpPost request = new HttpPost(address);
        request.addHeader(HEADER_TRANSMISSION_TIME, String.valueOf(ticks));
        return request;
    }

    public boolean isSuccess(HttpResponse response) {
        final int responseCode = response.getStatusLine().getStatusCode();
        return responseCode == 200;
    }

    public QuickPulseHeaderInfo getQuickPulseHeaderInfo(HttpResponse response) {
        Header[] headers = response.getAllHeaders();
        QuickPulseStatus status = QuickPulseStatus.ERROR;
        long servicePollingIntervalHint = -1;
        String serviceEndpointRedirect = null;
        final QuickPulseHeaderInfo quickPulseHeaderInfo;

        for (Header header: headers) {
            if (QP_STATUS_HEADER.equals(header.getName())) {
                final String qpStatus = header.getValue();
                if ("true".equalsIgnoreCase(qpStatus)) {
                    status =  QuickPulseStatus.QP_IS_ON;
                } else {
                    status = QuickPulseStatus.QP_IS_OFF;
                }
            } else if (QPS_SERVICE_POLLING_INTERVAL_HINT.equals(header.getName())) {
                final String servicePollingIntervalHintHeaderValue = header.getValue();
                if (!LocalStringsUtils.isNullOrEmpty(servicePollingIntervalHintHeaderValue)) {
                    servicePollingIntervalHint = Long.parseLong(servicePollingIntervalHintHeaderValue);
                }
            } else if (QPS_SERVICE_ENDPOINT_REDIRECT.equals(header.getName())) {
                serviceEndpointRedirect = header.getValue();
            }
        }
        quickPulseHeaderInfo = new QuickPulseHeaderInfo(status, serviceEndpointRedirect, servicePollingIntervalHint);
        return quickPulseHeaderInfo;
    }

    public QuickPulseStatus getQuickPulseStatus(HttpResponse response) {
        Header header = response.getFirstHeader(QP_STATUS_HEADER);
        if (header != null) {
            final String toPost = header.getValue();
            if ("true".equalsIgnoreCase(toPost)) {
                return QuickPulseStatus.QP_IS_ON;
            } else {
                return QuickPulseStatus.QP_IS_OFF;
            }
        }

        return QuickPulseStatus.ERROR;
    }
}
