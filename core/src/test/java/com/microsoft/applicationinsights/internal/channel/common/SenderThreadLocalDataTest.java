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

package com.microsoft.applicationinsights.internal.channel.common;

import org.junit.Test;

import static org.junit.Assert.*;

public final class SenderThreadLocalDataTest {
    @Test(expected = NullPointerException.class)
    public void testNotSupplyingBackOffTimesContainer() {
        new SenderThreadLocalBackOffData(null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithEmptyBackOffTimesContainer() {
        new SenderThreadLocalBackOffData(new long[]{}, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithEmptyNegativeAddSeconds() {
        new SenderThreadLocalBackOffData(new long[]{1000}, -1);
    }

    @Test
    public void testStateAfterCtor() {
        final SenderThreadLocalBackOffData sender = createSenderThreadLocalData(new long[] {1000});

        assertFalse(sender.isTryingToSend());
    }

    @Test
    public void testOnDoneSending() {
        final SenderThreadLocalBackOffData sender = createSenderThreadLocalData(new long[] {1000});
        verifyOnDoneSending(sender);
    }

    private SenderThreadLocalBackOffData createSenderThreadLocalData(long[] backOffs) {
        return new SenderThreadLocalBackOffData(backOffs, 0);
    }

    private static void verifyOnDoneSending(SenderThreadLocalBackOffData sender) {
        sender.onDoneSending();

        assertFalse(sender.isTryingToSend());
    }
}
