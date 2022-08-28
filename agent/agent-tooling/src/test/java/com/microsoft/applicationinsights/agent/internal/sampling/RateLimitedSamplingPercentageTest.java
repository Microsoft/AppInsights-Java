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

// Includes work from:
/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.microsoft.applicationinsights.agent.internal.sampling;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;

// uses tests from OpenTelemetry Java Contrib's ConsistentRateLimitingSampler
// (https://github.com/open-telemetry/opentelemetry-java-contrib/blob/main/consistent-sampling/src/test/java/io/opentelemetry/contrib/samplers/ConsistentRateLimitingSamplerTest.java)
class RateLimitedSamplingPercentageTest {

  private long[] nanoTime;
  private LongSupplier nanoTimeSupplier;

  @BeforeEach
  void init() {
    nanoTime = new long[] {0L};
    nanoTimeSupplier = () -> nanoTime[0];
  }

  private void advanceTime(long nanosIncrement) {
    nanoTime[0] += nanosIncrement;
  }

  private long getCurrentTimeNanos() {
    return nanoTime[0];
  }

  @Test
  void testConstantRate() {

    double targetSpansPerSecondLimit = 1000;
    double adaptationTimeSeconds = 5;

    RateLimitedSamplingPercentage samplingPercentage =
        new RateLimitedSamplingPercentage(
            targetSpansPerSecondLimit, adaptationTimeSeconds, nanoTimeSupplier, false);

    long nanosBetweenSpans = TimeUnit.MICROSECONDS.toNanos(100);
    int numSpans = 1000000;

    List<Long> spanSampledNanos = new ArrayList<>();

    for (int i = 0; i < numSpans; ++i) {
      advanceTime(nanosBetweenSpans);
      if (ThreadLocalRandom.current().nextDouble() < samplingPercentage.get() / 100) {
        spanSampledNanos.add(getCurrentTimeNanos());
      }
    }

    long numSampledSpansInLast5Seconds =
        spanSampledNanos.stream()
            .filter(x -> x > TimeUnit.SECONDS.toNanos(95) && x <= TimeUnit.SECONDS.toNanos(100))
            .count();

    assertThat(numSampledSpansInLast5Seconds / 5.)
        .isCloseTo(targetSpansPerSecondLimit, Percentage.withPercentage(5));
  }

  @Test
  void testRateIncrease() {

    double targetSpansPerSecondLimit = 1000;
    double adaptationTimeSeconds = 5;

    RateLimitedSamplingPercentage samplingPercentage =
        new RateLimitedSamplingPercentage(
            targetSpansPerSecondLimit, adaptationTimeSeconds, nanoTimeSupplier, false);

    long nanosBetweenSpans1 = TimeUnit.MICROSECONDS.toNanos(100);
    long nanosBetweenSpans2 = TimeUnit.MICROSECONDS.toNanos(10);
    int numSpans1 = 500000;
    int numSpans2 = 5000000;

    List<Long> spanSampledNanos = new ArrayList<>();

    for (int i = 0; i < numSpans1; ++i) {
      advanceTime(nanosBetweenSpans1);
      if (ThreadLocalRandom.current().nextDouble() < samplingPercentage.get() / 100) {
        spanSampledNanos.add(getCurrentTimeNanos());
      }
    }
    for (int i = 0; i < numSpans2; ++i) {
      advanceTime(nanosBetweenSpans2);
      if (ThreadLocalRandom.current().nextDouble() < samplingPercentage.get() / 100) {
        spanSampledNanos.add(getCurrentTimeNanos());
      }
    }

    long numSampledSpansWithin5SecondsBeforeChange =
        spanSampledNanos.stream()
            .filter(x -> x > TimeUnit.SECONDS.toNanos(45) && x <= TimeUnit.SECONDS.toNanos(50))
            .count();
    long numSampledSpansWithin5SecondsAfterChange =
        spanSampledNanos.stream()
            .filter(x -> x > TimeUnit.SECONDS.toNanos(50) && x <= TimeUnit.SECONDS.toNanos(55))
            .count();
    long numSampledSpansInLast5Seconds =
        spanSampledNanos.stream()
            .filter(x -> x > TimeUnit.SECONDS.toNanos(95) && x <= TimeUnit.SECONDS.toNanos(100))
            .count();

    assertThat(numSampledSpansWithin5SecondsBeforeChange / 5.)
        .isCloseTo(targetSpansPerSecondLimit, Percentage.withPercentage(5));
    assertThat(numSampledSpansWithin5SecondsAfterChange / 5.)
        .isGreaterThan(2. * targetSpansPerSecondLimit);
    assertThat(numSampledSpansInLast5Seconds / 5.)
        .isCloseTo(targetSpansPerSecondLimit, Percentage.withPercentage(5));
  }

  @Test
  void testRateDecrease() {

    double targetSpansPerSecondLimit = 1000;
    double adaptationTimeSeconds = 5;

    RateLimitedSamplingPercentage samplingPercentage =
        new RateLimitedSamplingPercentage(
            targetSpansPerSecondLimit, adaptationTimeSeconds, nanoTimeSupplier, false);

    long nanosBetweenSpans1 = TimeUnit.MICROSECONDS.toNanos(10);
    long nanosBetweenSpans2 = TimeUnit.MICROSECONDS.toNanos(100);
    int numSpans1 = 5000000;
    int numSpans2 = 500000;

    List<Long> spanSampledNanos = new ArrayList<>();

    for (int i = 0; i < numSpans1; ++i) {
      advanceTime(nanosBetweenSpans1);
      if (ThreadLocalRandom.current().nextDouble() < samplingPercentage.get() / 100) {
        spanSampledNanos.add(getCurrentTimeNanos());
      }
    }
    for (int i = 0; i < numSpans2; ++i) {
      advanceTime(nanosBetweenSpans2);
      if (ThreadLocalRandom.current().nextDouble() < samplingPercentage.get() / 100) {
        spanSampledNanos.add(getCurrentTimeNanos());
      }
    }

    long numSampledSpansWithin5SecondsBeforeChange =
        spanSampledNanos.stream()
            .filter(x -> x > TimeUnit.SECONDS.toNanos(45) && x <= TimeUnit.SECONDS.toNanos(50))
            .count();
    long numSampledSpansWithin5SecondsAfterChange =
        spanSampledNanos.stream()
            .filter(x -> x > TimeUnit.SECONDS.toNanos(50) && x <= TimeUnit.SECONDS.toNanos(55))
            .count();
    long numSampledSpansInLast5Seconds =
        spanSampledNanos.stream()
            .filter(x -> x > TimeUnit.SECONDS.toNanos(95) && x <= TimeUnit.SECONDS.toNanos(100))
            .count();

    assertThat(numSampledSpansWithin5SecondsBeforeChange / 5.)
        .isCloseTo(targetSpansPerSecondLimit, Percentage.withPercentage(5));
    assertThat(numSampledSpansWithin5SecondsAfterChange / 5.)
        .isLessThan(0.5 * targetSpansPerSecondLimit);
    assertThat(numSampledSpansInLast5Seconds / 5.)
        .isCloseTo(targetSpansPerSecondLimit, Percentage.withPercentage(5));
  }
}
