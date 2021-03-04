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

import com.microsoft.applicationinsights.internal.channel.TransmissionOutputAsync;
import org.junit.Test;

import org.mockito.Mockito;

import static org.mockito.Matchers.anyObject;

public class NonBlockingDispatcherTest {

    @Test(expected = NullPointerException.class)
    public void nullTest() {
        new NonBlockingDispatcher(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyTest() {
        new NonBlockingDispatcher(new TransmissionOutputAsync[]{});
    }

    @Test(expected = NullPointerException.class)
    public void testNullDispatch() {
        createDispatcher().dispatch(null);
    }

    @Test
    public void testDispatchSuccessOfFirst() {
        TransmissionOutputAsync mockOutput1 = Mockito.mock(TransmissionOutputAsync.class);
        Mockito.doReturn(true).when(mockOutput1).sendAsync(anyObject());

        TransmissionOutputAsync mockOutput2 = Mockito.mock(TransmissionOutputAsync.class);

        NonBlockingDispatcher tested = new NonBlockingDispatcher(new TransmissionOutputAsync[] {mockOutput1, mockOutput2});

        Transmission transmission = new Transmission(new byte[2], "mockType", "mockEncoding");
        tested.dispatch(transmission);

        Mockito.verify(mockOutput1, Mockito.times(1)).sendAsync(anyObject());
        Mockito.verify(mockOutput2, Mockito.never()).sendAsync(anyObject());
    }

    @Test
    public void testDispatchFailureOfFirst() {
        TransmissionOutputAsync mockOutput1 = Mockito.mock(TransmissionOutputAsync.class);
        Mockito.doReturn(false).when(mockOutput1).sendAsync(anyObject());

        TransmissionOutputAsync mockOutput2 = Mockito.mock(TransmissionOutputAsync.class);
        Mockito.doReturn(true).when(mockOutput2).sendAsync(anyObject());

        NonBlockingDispatcher tested = new NonBlockingDispatcher(new TransmissionOutputAsync[] {mockOutput1, mockOutput2});

        Transmission transmission = new Transmission(new byte[2], "mockType", "mockEncoding");
        tested.dispatch(transmission);

        Mockito.verify(mockOutput1, Mockito.times(1)).sendAsync(anyObject());
        Mockito.verify(mockOutput2, Mockito.times(1)).sendAsync(anyObject());
    }

    private NonBlockingDispatcher createDispatcher() {
        TransmissionOutputAsync mockOutput1 = Mockito.mock(TransmissionOutputAsync.class);
        TransmissionOutputAsync mockOutput2 = Mockito.mock(TransmissionOutputAsync.class);

        return new NonBlockingDispatcher(new TransmissionOutputAsync[] {mockOutput1, mockOutput2});
    }
}