package com.microsoft.gcmonitor.garbagecollectors;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

/**
 * Collection of all known GarbageCollectors
 */
public enum GarbageCollectors {
    PS_MARK_SWEEP(PSMarkSweep.NAME, PSMarkSweep::new),
    PS_SCAVENGE(PSScavenge.NAME, PSScavenge::new),
    G1_OLD_GENERATION(G1OldGeneration.NAME, G1OldGeneration::new),
    G1_YOUNG_GENERATION(G1YoungGeneration.NAME, G1YoungGeneration::new),
    CONCURRENT_MARK_SWEEP(ConcurrentMarkSweep.NAME, ConcurrentMarkSweep::new),
    PAR_NEW(ParNew.NAME, ParNew::new),
    MARK_SWEEP_COMPACT(MarkSweepCompact.NAME, MarkSweepCompact::new),
    COPY(Copy.NAME, Copy::new),
    CODE_CACHE_MANAGER(CodeCacheManager.NAME, CodeCacheManager::new),
    METASPACE_MANAGER(MetaspaceManager.NAME, MetaspaceManager::new),
    SHENANDOAH_CYCLES(ShenandoahCycles.NAME, ShenandoahCycles::new),
    SHENANDOAH_PAUSES(ShenandoahPauses.NAME, ShenandoahPauses::new),
    Z_GC(ZGC.NAME, ZGC::new);

    interface GarbageCollectorFactory extends Function<GarbageCollectorStats, GarbageCollector> {
    }

    final String name;
    final GarbageCollectorFactory factoryFunction;

    GarbageCollectors(String name, GarbageCollectorFactory factory) {
        this.name = name;
        this.factoryFunction = factory;
    }

    private static Optional<GarbageCollectors> findCollectorFor(String gcName) {
        return Arrays.stream(GarbageCollectors.values())
                .filter(collector -> collector.name.equals(gcName))
                .findFirst();
    }

    /**
     * Factory to instantiate a GarbageCollector based on its name
     */
    public static GarbageCollector create(String name, GarbageCollectorStats proxy) {
        Optional<GarbageCollectors> factory = GarbageCollectors.findCollectorFor(name);
        if (factory.isPresent()) {
            return factory.get().factoryFunction.apply(proxy);
        } else {
            throw new IllegalArgumentException("Could not find factory for " + name);
        }
    }

    public static class PSMarkSweep extends GarbageCollector {
        public static final String NAME = "PS MarkSweep";

        public PSMarkSweep(GarbageCollectorStats proxy) {
            super(proxy, NAME, true, true, false);
        }
    }

    public static class PSScavenge extends GarbageCollector {
        public static final String NAME = "PS Scavenge";

        public PSScavenge(GarbageCollectorStats proxy) {
            super(proxy, NAME, true, false, true);
        }
    }

    public static class G1OldGeneration extends GarbageCollector {
        public static final String NAME = "G1 Old Generation";

        public G1OldGeneration(GarbageCollectorStats proxy) {
            super(proxy, NAME, true, true, false);
        }
    }

    public static class G1YoungGeneration extends GarbageCollector {
        public static final String NAME = "G1 Young Generation";

        public G1YoungGeneration(GarbageCollectorStats proxy) {
            super(proxy, NAME, true, false, true);
        }
    }

    public static class ConcurrentMarkSweep extends GarbageCollector {
        public static final String NAME = "ConcurrentMarkSweep";

        public ConcurrentMarkSweep(GarbageCollectorStats proxy) {
            super(proxy, NAME, true, true, false);
        }
    }

    public static class ParNew extends GarbageCollector {
        public static final String NAME = "ParNew";

        public ParNew(GarbageCollectorStats proxy) {
            super(proxy, NAME, true, false, true);
        }
    }

    public static class MarkSweepCompact extends GarbageCollector {
        public static final String NAME = "MarkSweepCompact";

        public MarkSweepCompact(GarbageCollectorStats proxy) {
            super(proxy, NAME, true, true, false);
        }
    }

    public static class Copy extends GarbageCollector {
        public static final String NAME = "Copy";

        public Copy(GarbageCollectorStats proxy) {
            super(proxy, NAME, true, false, true);
        }
    }

    public static class CodeCacheManager extends GarbageCollector {
        public static final String NAME = "CodeCacheManager";

        public CodeCacheManager(GarbageCollectorStats proxy) {
            super(proxy, NAME, true, false, false);
        }
    }

    public static class MetaspaceManager extends GarbageCollector {
        public static final String NAME = "Metaspace Manager";

        public MetaspaceManager(GarbageCollectorStats proxy) {
            super(proxy, NAME, true, false, false);
        }
    }

    public static class ShenandoahCycles extends GarbageCollector {
        public static final String NAME = "Shenandoah Cycles";

        public ShenandoahCycles(GarbageCollectorStats proxy) {
            super(proxy, NAME, true, true, true);
        }
    }

    public static class ShenandoahPauses extends GarbageCollector {
        public static final String NAME = "Shenandoah Pauses";

        public ShenandoahPauses(GarbageCollectorStats proxy) {
            super(proxy, NAME, true, true, true);
        }
    }

    public static class ZGC extends GarbageCollector {
        public static final String NAME = "ZGC";

        public ZGC(GarbageCollectorStats proxy) {
            super(proxy, NAME, true, true, true);
        }
    }
}
