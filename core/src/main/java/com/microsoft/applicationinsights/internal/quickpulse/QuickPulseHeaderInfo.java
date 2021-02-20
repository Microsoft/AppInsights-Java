package com.microsoft.applicationinsights.internal.quickpulse;

public class QuickPulseHeaderInfo {

    private QuickPulseStatus quickPulseStatus;
    private String QpsServiceEndpointRedirect;
    private long QpsServicePollingInterval;

    public QuickPulseHeaderInfo(QuickPulseStatus quickPulseStatus,
                                String QpsServiceEndpointRedirect, long QpsServicePollingIntervalHint) {

        this.quickPulseStatus = quickPulseStatus;
        this.QpsServiceEndpointRedirect = QpsServiceEndpointRedirect;
        this.QpsServicePollingInterval = QpsServicePollingIntervalHint;
    }

    public QuickPulseHeaderInfo(QuickPulseStatus quickPulseStatus) {
        this.quickPulseStatus = quickPulseStatus;
        this.QpsServiceEndpointRedirect = null;
        this.QpsServicePollingInterval = -1;
    }

    public long getQpsServicePollingInterval() {
        return QpsServicePollingInterval;
    }

    public String getQpsServiceEndpointRedirect() {
        return QpsServiceEndpointRedirect;
    }

    public QuickPulseStatus getQuickPulseStatus() {
        return quickPulseStatus;
    }
}
