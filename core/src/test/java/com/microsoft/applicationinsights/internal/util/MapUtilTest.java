package com.microsoft.applicationinsights.internal.util;

import org.junit.Assert;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

public class MapUtilTest {

    @Test
    public void testCopyIntoHashMap() {
        Map<String, String> source = new HashMap<>();
        Map<String, String> target = new HashMap<>();

        source.put("key1", "value1");
        source.put("key2", null);

        MapUtil.copy(source, target);
        Assert.assertEquals(2, target.size());
    }

    @Test
    public void testCopyIntoConcurrentHashMap() {
        Map<String, String> source = new HashMap<>();
        Map<String, String> target = new ConcurrentHashMap<>();

        source.put("key1", "value1");
        source.put("key2", null);

        MapUtil.copy(source, target);
        Assert.assertEquals(1, target.size());
    }
}
