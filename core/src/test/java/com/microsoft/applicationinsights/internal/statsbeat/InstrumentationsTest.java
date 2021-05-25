package com.microsoft.applicationinsights.internal.statsbeat;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class InstrumentationsTest {

    private static final Set<String> instrumentations;
    static {
        instrumentations = new HashSet<>();
        instrumentations.add("io.opentelemetry.javaagent.jdbc");
        instrumentations.add("io.opentelemetry.javaagent.tomcat-7.0");
        instrumentations.add("io.opentelemetry.javaagent.http-url-connection");
    }

    private static final long EXPECTED_INSTRUMENTATION = (long)(Math.pow(2, 13) + Math.pow(2, 21) + Math.pow(2, 57)); // Exponents are keys from StatsbeatHelper.INSTRUMENTATION_MAP.)

    @Test
    public void testEncodeAndDecodeInstrumentations() {
        long longVal = Instrumentations.encode(instrumentations);
        assertEquals(EXPECTED_INSTRUMENTATION, longVal);
        Set<String> result = StatsbeatTestUtils.decodeInstrumentations(longVal);
        assertEquals(instrumentations, result);
    }
}
