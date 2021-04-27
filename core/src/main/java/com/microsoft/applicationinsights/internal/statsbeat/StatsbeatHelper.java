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

package com.microsoft.applicationinsights.internal.statsbeat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.microsoft.applicationinsights.internal.statsbeat.Constants.JAVA_VENDOR_ADOPT_OPENJDK;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.JAVA_VENDOR_MICROSOFT;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.JAVA_VENDOR_ORACLE;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.JAVA_VENDOR_OTHER;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.JAVA_VENDOR_REDHAT;
import static com.microsoft.applicationinsights.internal.statsbeat.Constants.JAVA_VENDOR_ZULU;

public final class StatsbeatHelper {

    private static final Map<String, Integer> INSTRUMENTATION_MAP = new HashMap<String, Integer>() {{
        put("io.opentelemetry.javaagent.apache-httpasyncclient-4.1", 0);
        put("io.opentelemetry.javaagent.apache-httpclient-2.0", 1);
        put("io.opentelemetry.javaagent.apache-httpclient-4.0", 2);
        put("io.opentelemetry.javaagent.apache-httpclient-5.0", 3);
        put("io.opentelemetry.javaagent.applicationinsights-web-2.3", 4);
        put("io.opentelemetry.javaagent.azure-functions", 5);
        put("io.opentelemetry.javaagent.azure-core-1.14", 6);
        put("io.opentelemetry.javaagent.cassandra-3.0", 7);
        put("io.opentelemetry.javaagent.cassandra-4.0", 8);
        put("io.opentelemetry.javaagent.classloaders", 9);
        put("io.opentelemetry.javaagent.eclipse-osgi-3.6", 10);
        put("io.opentelemetry.javaagent.executors", 11);
        put("io.opentelemetry.javaagent.grpc-1.5", 12);
        put("io.opentelemetry.javaagent.http-url-connection", 13);
        put("io.opentelemetry.javaagent.java-util-logging", 14);
        put("io.opentelemetry.javaagent.java-util-logging-spans", 15);
        put("io.opentelemetry.javaagent.jaxrs-1.0", 16);
        put("io.opentelemetry.javaagent.jaxrs-2.0-common", 17);
        put("io.opentelemetry.javaagent.jaxrs-2.0-jersey-2.0", 18);
        put("io.opentelemetry.javaagent.jaxrs-2.0-resteasy-3.0", 19);
        put("io.opentelemetry.javaagent.jaxrs-2.0-resteasy-3.1", 20);
        put("io.opentelemetry.javaagent.jdbc", 21);
        put("io.opentelemetry.javaagent.jedis-1.4", 22);
        put("io.opentelemetry.javaagent.jedis-3.0", 23);
        put("io.opentelemetry.javaagent.jetty-8.0", 24);
        put("io.opentelemetry.javaagent.jms-1.1", 25);
        put("io.opentelemetry.javaagent.kafka-clients-0.11", 26);
        put("io.opentelemetry.javaagent.kafka-streams-0.11", 27);
        put("io.opentelemetry.javaagent.kotlinx-coroutines", 28);
        put("io.opentelemetry.javaagent.lettuce-4.0", 29);
        put("io.opentelemetry.javaagent.lettuce-5.0", 30);
        put("io.opentelemetry.javaagent.lettuce-5.1", 31);
        put("io.opentelemetry.javaagent.lettuce-common", 32);
        put("io.opentelemetry.javaagent.log4j-2.0", 33);
        put("io.opentelemetry.javaagent.log4j-spans-1.2", 34);
        put("io.opentelemetry.javaagent.log4j-spans-2.0", 35);
        put("io.opentelemetry.javaagent.logback-1.0", 36);
        put("io.opentelemetry.javaagent.logback-spans-1.0", 37);
        put("io.opentelemetry.javaagent.micrometer-1.0", 38);
        put("io.opentelemetry.javaagent.mongo-3.1", 39);
        put("io.opentelemetry.javaagent.mongo-3.7", 40);
        put("io.opentelemetry.javaagent.mongo-async-3.3", 41);
        put("io.opentelemetry.javaagent.mongo-common", 42);
        put("io.opentelemetry.javaagent.netty-4.0", 43);
        put("io.opentelemetry.javaagent.netty-4.1", 44);
        put("io.opentelemetry.javaagent.okhttp-3.0", 45);
        put("io.opentelemetry.javaagent.opentelemetry-annotations-1.0", 46);
        put("io.opentelemetry.javaagent.opentelemetry-api-1.0", 47);
        put("io.opentelemetry.javaagent.reactor-3.1", 48);
        put("io.opentelemetry.javaagent.reactor-netty-0.9", 49);
        put("io.opentelemetry.javaagent.reactor-netty-1.0", 50);
        put("io.opentelemetry.javaagent.servlet-2.2", 51);
        put("io.opentelemetry.javaagent.servlet-3.0", 52);
        put("io.opentelemetry.javaagent.servlet-common", 53);
        put("io.opentelemetry.javaagent.spring-scheduling-3.1", 54);
        put("io.opentelemetry.javaagent.spring-webflux-5.0", 55);
        put("io.opentelemetry.javaagent.spring-webmvc-3.1", 56);
        put("io.opentelemetry.javaagent.tomcat-7.0", 57);
    }};

    public static Map<Integer, String> INSTRUMENTATION_MAP_DECODING = new HashMap<Integer, String>() {{
        put(0, "io.opentelemetry.javaagent.apache-httpasyncclient-4.1");
        put(1, "io.opentelemetry.javaagent.apache-httpclient-2.0");
        put(2, "io.opentelemetry.javaagent.apache-httpclient-4.0");
        put(3, "io.opentelemetry.javaagent.apache-httpclient-5.0");
        put(4, "io.opentelemetry.javaagent.applicationinsights-web-2.3");
        put(5, "io.opentelemetry.javaagent.azure-functions");
        put(6, "io.opentelemetry.javaagent.azure-core-1.14");
        put(7, "io.opentelemetry.javaagent.cassandra-3.0");
        put(8, "io.opentelemetry.javaagent.cassandra-4.0");
        put(9, "io.opentelemetry.javaagent.classloaders");
        put(10, "io.opentelemetry.javaagent.eclipse-osgi-3.6");
        put(11, "io.opentelemetry.javaagent.executors");
        put(12, "io.opentelemetry.javaagent.grpc-1.5");
        put(13, "io.opentelemetry.javaagent.http-url-connection");
        put(14, "io.opentelemetry.javaagent.java-util-logging");
        put(15, "io.opentelemetry.javaagent.java-util-logging-spans");
        put(16, "io.opentelemetry.javaagent.jaxrs-1.0");
        put(17, "io.opentelemetry.javaagent.jaxrs-2.0-common");
        put(18, "io.opentelemetry.javaagent.jaxrs-2.0-jersey-2.0");
        put(19, "io.opentelemetry.javaagent.jaxrs-2.0-resteasy-3.0");
        put(20, "io.opentelemetry.javaagent.jaxrs-2.0-resteasy-3.1");
        put(21, "io.opentelemetry.javaagent.jdbc");
        put(22, "io.opentelemetry.javaagent.jedis-1.4");
        put(23, "io.opentelemetry.javaagent.jedis-3.0");
        put(24, "io.opentelemetry.javaagent.jetty-8.0");
        put(25, "io.opentelemetry.javaagent.jms-1.1");
        put(26, "io.opentelemetry.javaagent.kafka-clients-0.11");
        put(27, "io.opentelemetry.javaagent.kafka-streams-0.11");
        put(28, "io.opentelemetry.javaagent.kotlinx-coroutines");
        put(29, "io.opentelemetry.javaagent.lettuce-4.0");
        put(30, "io.opentelemetry.javaagent.lettuce-5.0");
        put(31, "io.opentelemetry.javaagent.lettuce-5.1");
        put(32, "io.opentelemetry.javaagent.lettuce-common");
        put(33, "io.opentelemetry.javaagent.log4j-2.0");
        put(34, "io.opentelemetry.javaagent.log4j-spans-1.2");
        put(35, "io.opentelemetry.javaagent.log4j-spans-2.0");
        put(36, "io.opentelemetry.javaagent.logback-1.0");
        put(37, "io.opentelemetry.javaagent.logback-spans-1.0");
        put(38, "io.opentelemetry.javaagent.micrometer-1.0");
        put(39, "io.opentelemetry.javaagent.mongo-3.1");
        put(40, "io.opentelemetry.javaagent.mongo-3.7");
        put(41, "io.opentelemetry.javaagent.mongo-async-3.3");
        put(42, "io.opentelemetry.javaagent.mongo-common");
        put(43, "io.opentelemetry.javaagent.netty-4.0");
        put(44, "io.opentelemetry.javaagent.netty-4.1");
        put(45, "io.opentelemetry.javaagent.okhttp-3.0");
        put(46, "io.opentelemetry.javaagent.opentelemetry-annotations-1.0");
        put(47, "io.opentelemetry.javaagent.opentelemetry-api-1.0");
        put(48, "io.opentelemetry.javaagent.reactor-3.1");
        put(49, "io.opentelemetry.javaagent.reactor-netty-0.9");
        put(50, "io.opentelemetry.javaagent.reactor-netty-1.0");
        put(51, "io.opentelemetry.javaagent.servlet-2.2");
        put(52, "io.opentelemetry.javaagent.servlet-3.0");
        put(53, "io.opentelemetry.javaagent.servlet-common");
        put(54, "io.opentelemetry.javaagent.spring-scheduling-3.1");
        put(55, "io.opentelemetry.javaagent.spring-webflux-5.0");
        put(56, "io.opentelemetry.javaagent.spring-webmvc-3.1");
        put(57, "io.opentelemetry.javaagent.tomcat-7.0");
    }};

    public static final Map<String, Integer> FEATURE_MAP = new HashMap<String, Integer>() {{
        put(JAVA_VENDOR_ORACLE, 0);
        put(JAVA_VENDOR_ZULU, 1);
        put(JAVA_VENDOR_MICROSOFT, 2);
        put(JAVA_VENDOR_ADOPT_OPENJDK, 3);
        put(JAVA_VENDOR_REDHAT, 4);
        put(JAVA_VENDOR_OTHER, 5);
    }};

    public static final Map<Integer, String> FEATURE_MAP_DECODING = new HashMap<Integer, String>() {{
        put(0, JAVA_VENDOR_ORACLE);
        put(1, JAVA_VENDOR_ZULU);
        put(2, JAVA_VENDOR_MICROSOFT);
        put(3, JAVA_VENDOR_ADOPT_OPENJDK);
        put(4, JAVA_VENDOR_REDHAT);
        put(5, JAVA_VENDOR_OTHER);
    }};

    public static long encodeInstrumentations(Set<String> instrumentations) {
        return encode(instrumentations, INSTRUMENTATION_MAP);
    }

    public static long encodeFeature(Set<String> features) {
        return encode(features, FEATURE_MAP);
    }

    public static Set<String> decodeInstrumentations(long num) {
        return decode(num, INSTRUMENTATION_MAP_DECODING);
    }

    public static Set<String> decodeFeature(long num) {
        return decode(num, FEATURE_MAP_DECODING);
    }

    private static long encode(Set<String> list, Map<String, Integer> map) {
        Long number = 0L;
        for (String item : list) {
            int index = map.get(item);
            number |= getPowerOf2(index);
        }

        return number;
    }

    private static Set<String> decode(long num, Map<Integer, String> decodedMap) {
        Set<String> result = new HashSet<>();
        Set<Integer> keySet = decodedMap.keySet();
        for(int key : keySet) {
            Long powerVal = getPowerOf2(key);
            if ((powerVal & num) == powerVal) {
                result.add(decodedMap.get(key));
            }
        }

        return result;
    }

    private static Long getPowerOf2(int exponent) {
        long result = 1L;
        while (exponent != 0) {
            result *= 2;
            exponent--;
        }
        return result;
    }

    private StatsbeatHelper() {
    }
}
