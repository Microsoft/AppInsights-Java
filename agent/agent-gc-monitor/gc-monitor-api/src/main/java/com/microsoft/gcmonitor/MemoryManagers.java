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

package com.microsoft.gcmonitor;

import com.microsoft.gcmonitor.garbagecollectors.GarbageCollector;
import com.microsoft.gcmonitor.garbagecollectors.GarbageCollectors;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a group of memory managers that as a group manages the JVMs heap. Typically, for most
 * collectors this would consist of a Young generational manager and a Tenured generation manger.
 */
public enum MemoryManagers {
  PARALLEL_SCAVENGE(GarbageCollectors.PsMarkSweep.class, GarbageCollectors.PsScavenge.class),

  CONCURRENT_MARK_SWEEP(
      GarbageCollectors.ConcurrentMarkSweep.class, GarbageCollectors.ParNew.class),

  MARK_SWEEP(GarbageCollectors.MarkSweepCompact.class, GarbageCollectors.Copy.class),

  G1(GarbageCollectors.G1OldGeneration.class, GarbageCollectors.G1YoungGeneration.class),

  SHENANDOAH(GarbageCollectors.ShenandoahCycles.class, GarbageCollectors.ShenandoahPauses.class),

  ZGC(GarbageCollectors.Zgc.class);

  private final Class<? extends GarbageCollector>[] managers;

  public static MemoryManagers of(MemoryManagement manager) {
    Set<GarbageCollector> collectorIdentifiers = manager.getCollectors();
    for (MemoryManagers group : values()) {
      if (group.isComposedOf(collectorIdentifiers)) {
        return group;
      }
    }
    throw new IllegalArgumentException(
        "Unable to find garbage collector group for the memory manager");
  }

  MemoryManagers(Class<? extends GarbageCollector>... allCollectors) {
    this.managers = allCollectors;
  }

  private boolean isComposedOf(Set<GarbageCollector> collectors) {
    Set<Class<? extends GarbageCollector>> collectorClasses =
        collectors.stream().map(GarbageCollector::getClass).collect(Collectors.toSet());
    return collectorClasses.containsAll(Arrays.asList(managers));
  }
}
