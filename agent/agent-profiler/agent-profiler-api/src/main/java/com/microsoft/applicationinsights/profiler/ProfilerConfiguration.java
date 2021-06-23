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

package com.microsoft.applicationinsights.profiler;

import com.squareup.moshi.Json;
import java.util.Date;

public class ProfilerConfiguration {

  // TODO (johnoliver) can we remove unused fields here?
  //  if moshi is complaining, there's a setting to tell it to ignore missing fields
  //  (which is probably good anyways in case server adds new fields)
  @SuppressWarnings("unused")
  @Json(name = "id")
  private final String id;

  @Json(name = "lastModified")
  private final Date lastModified;

  // TODO (johnoliver) can we remove unused fields here?
  @SuppressWarnings("unused")
  @Json(name = "enabledLastModified")
  private final Date enabledLastModified;

  @Json(name = "enabled")
  private final boolean enabled;

  @Json(name = "cpuTriggerConfiguration")
  private final String cpuTriggerConfiguration;

  @Json(name = "memoryTriggerConfiguration")
  private final String memoryTriggerConfiguration;

  @Json(name = "collectionPlan")
  private final String collectionPlan;

  @Json(name = "defaultConfiguration")
  private final String defaultConfiguration;

  // TODO (johnoliver) can we remove unused fields here?
  @SuppressWarnings("unused")
  @Json(name = "geoOverride")
  private final String geoOverride;

  // TODO (johnoliver) is this used?
  public ProfilerConfiguration(
      String id,
      Date lastModified,
      Date enabledLastModified,
      boolean enabled,
      String collectionPlan,
      String cpuTriggerConfiguration,
      String memoryTriggerConfiguration,
      String defaultConfiguration,
      String geoOverride) {
    this.id = id;
    this.lastModified = new Date(lastModified.getTime());
    this.enabledLastModified = new Date(enabledLastModified.getTime());
    this.enabled = enabled;
    this.collectionPlan = collectionPlan;
    this.cpuTriggerConfiguration = cpuTriggerConfiguration;
    this.memoryTriggerConfiguration = memoryTriggerConfiguration;
    this.defaultConfiguration = defaultConfiguration;
    this.geoOverride = geoOverride;
  }

  public Date getLastModified() {
    return new Date(lastModified.getTime());
  }

  public String getCollectionPlan() {
    return collectionPlan;
  }

  public String getCpuTriggerConfiguration() {
    return cpuTriggerConfiguration;
  }

  public String getMemoryTriggerConfiguration() {
    return memoryTriggerConfiguration;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getDefaultConfiguration() {
    return defaultConfiguration;
  }
}
