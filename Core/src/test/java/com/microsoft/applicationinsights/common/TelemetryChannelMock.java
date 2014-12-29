package com.microsoft.applicationinsights.common;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.microsoft.applicationinsights.channel.Telemetry;
import com.microsoft.applicationinsights.channel.TelemetryChannel;

/**
 * Telemetry channel mock which provides the events that has been sent by the client.
 */
public class TelemetryChannelMock implements TelemetryChannel {

    private List<Telemetry> sentItems = new LinkedList<Telemetry>();

    @Override
    public boolean isDeveloperMode() {
        return false;
    }

    /**
     * Sets a flag indicating whether this channel is in developer mode.
     *
     * @param value
     */
    @Override
    public void setDeveloperMode(boolean value) {

    }

    /**
     * Sends a Telemetry instance through the channel.
     *
     * @param item
     */
    @Override
    public void send(Telemetry item) {
        sentItems.add(item);
    }

    @Override
    public void stop(long timeout, TimeUnit timeUnit) {

    }

    public List<Telemetry> getSentItems() {
        return sentItems;
    }
}
