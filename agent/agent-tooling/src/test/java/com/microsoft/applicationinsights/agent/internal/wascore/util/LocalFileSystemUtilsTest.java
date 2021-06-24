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

package com.microsoft.applicationinsights.agent.internal.wascore.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.junit.jupiter.api.Test;

class LocalFileSystemUtilsTest {
  /*
   * NOTE: it doesn't matter that Windows paths are converted to *nix paths and vice-versa.
   */

  @Test
  void getTempDir_WindowsForUserWithSystemUserUnknown() {
    final String input = "C:\\Users\\olivida\\AppData\\Local\\Temp";

    File actual = LocalFileSystemUtils.getTempDir(input, "unknown");

    File expected = new File(input);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void getTempDir_WindowsForUserWithSystemUserDefined() {
    final String input = "C:\\Users\\olivida\\AppData\\Local\\Temp";

    File actual = LocalFileSystemUtils.getTempDir(input, "olivida");

    File expected = new File(input);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void getTempDir_WindowsForUser() {
    final String input = "C:\\Users\\olivida\\AppData\\Local\\Temp";

    File actual = LocalFileSystemUtils.getTempDir(input, null);

    File expected = new File(input);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void getTempDir_Linux() {
    final String input = "/tmp";

    File actual = LocalFileSystemUtils.getTempDir(input, "olivida");

    File expected = new File(input, "olivida").getAbsoluteFile();
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void getTempDir_PerUser() {
    final String input = "/tmp/olivida";

    File actual = LocalFileSystemUtils.getTempDir(input, "olivida");

    File expected = new File(input);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void getTempDir_InsideUserHome() {
    final String input = "/home/olivida/tmp";

    File actual = LocalFileSystemUtils.getTempDir(input, "olivida");

    File expected = new File(input);
    assertThat(actual).isEqualTo(expected);
  }
}
