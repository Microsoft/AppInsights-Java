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

public final class StaticBackOffTimesContainerTest {
    @Test
    public void testBackOffs() {
        long[] backOffs = new StaticBackOffTimesPolicy().getBackOffTimeoutsInMillis();
        assertNotNull(backOffs);
        assertEquals(StaticBackOffTimesPolicy.NUMBER_OF_BACK_OFFS, backOffs.length);
        int couples = StaticBackOffTimesPolicy.NUMBER_OF_BACK_OFFS;
        long oddValue = -1;
        for (int i = 0; i < couples; ++i) {
            if (i % 2 == 0) {
                assertEquals(BackOffTimesPolicy.MIN_TIME_TO_BACK_OFF_IN_MILLS, backOffs[i]);
            } else {
                if (i == 1) {
                    oddValue = backOffs[i];
                    assertTrue(oddValue > 0);
                }

                assertEquals(oddValue, backOffs[i]);
            }
        }
    }
}