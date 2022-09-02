// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.serviceprofilerapi.client.uploader;

import com.microsoft.applicationinsights.serviceprofilerapi.client.contract.OsPlatforms;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OsPlatformProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(OsPlatformProvider.class.getName());

  @Nullable
  public static String getOsPlatformDescription() {
    if (isWindows()) {
      return OsPlatforms.WINDOWS;
    } else if (isLinux()) {
      return OsPlatforms.LINUX;
    } else if (isMac()) {
      return OsPlatforms.OSX;
    }

    LOGGER.warn("Type of operating system could not be determined");
    return null;
  }

  private static boolean isWindows() {
    return getOsName().startsWith("Windows");
  }

  private static boolean isLinux() {
    return getOsName().startsWith("Linux") || getOsName().startsWith("LINUX");
  }

  private static boolean isMac() {
    return getOsName().startsWith("Mac");
  }

  private static String getOsName() {
    try {
      return System.getProperty("os.name");
    } catch (SecurityException ex) {
      return "";
    }
  }

  private OsPlatformProvider() {}
}
