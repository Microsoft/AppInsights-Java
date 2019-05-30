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

package com.microsoft.applicationinsights.agent.internal.config;

import com.microsoft.applicationinsights.agent.internal.agent.ClassInstrumentationData;

import java.util.List;

/**
 * Created by gupele on 6/5/2015.
 */
public class AgentBuiltInConfiguration {
    private final boolean enabled;
    private final boolean httpEnabled;
    private final boolean w3cEnabled;
    private final boolean isW3CBackportEnabled;
    private final boolean jdbcEnabled;
    private final boolean hibernateEnabled;
    private final boolean jedisEnabled;
    private final boolean jmxEnabled;
    private final long maxSqlQueryLimitInMS;
    private final long redisThresholdInMS;
    private final DataOfConfigurationForException dataOfConfigurationForException;
    private final List<ClassInstrumentationData> simpleBuiltInClasses;

    public AgentBuiltInConfiguration(boolean enabled,
                                     List<ClassInstrumentationData> simpleBuiltInClasses,
                                     boolean httpEnabled,
                                     boolean w3cEnabled,
                                     boolean isW3CBackportEnabled,
                                     boolean jdbcEnabled,
                                     boolean hibernateEnabled,
                                     boolean jedisEnabled,
                                     boolean jmxEnabled,
                                     Long maxSqlQueryLimitInMS,
                                     long redisThresholdInMS,
                                     DataOfConfigurationForException dataOfConfigurationForException) {
        this.simpleBuiltInClasses = simpleBuiltInClasses;
        this.enabled = enabled;
        this.httpEnabled = httpEnabled;
        this.w3cEnabled = w3cEnabled;
        this.isW3CBackportEnabled = isW3CBackportEnabled;
        this.jdbcEnabled = jdbcEnabled;
        this.hibernateEnabled = hibernateEnabled;
        this.jmxEnabled = jmxEnabled;
        if (maxSqlQueryLimitInMS == null) {
            throw new IllegalArgumentException("maxSqlQueryLimit cannot be null");
        }
        this.redisThresholdInMS = redisThresholdInMS;
        this.jedisEnabled = jedisEnabled;
        this.maxSqlQueryLimitInMS = maxSqlQueryLimitInMS;
        this.dataOfConfigurationForException = dataOfConfigurationForException;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isHttpEnabled() {
        return httpEnabled;
    }

    public boolean isJdbcEnabled() {
        return jdbcEnabled;
    }

    public boolean isHibernateEnabled() {
        return hibernateEnabled;
    }

    public long getSqlMaxQueryLimitInMS() {
        return maxSqlQueryLimitInMS;
    }

    public boolean isRedisEnabled() {
        return jedisEnabled;
    }

    public long getRedisThresholdInMS() {
        return redisThresholdInMS;
    }

    public boolean isJmxEnabled() {
        return jmxEnabled;
    }

    public boolean isW3cEnabled() {
        return w3cEnabled;
    }

    public boolean isW3CBackportEnabled() {
        return isW3CBackportEnabled;
    }

    public DataOfConfigurationForException getDataOfConfigurationForException() {
        return dataOfConfigurationForException;
    }

    public List<ClassInstrumentationData> getSimpleBuiltInClasses() {
        return simpleBuiltInClasses;
    }
}
