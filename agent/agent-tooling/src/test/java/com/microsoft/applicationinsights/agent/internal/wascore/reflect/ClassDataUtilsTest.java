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

package com.microsoft.applicationinsights.agent.internal.wascore.reflect;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClassDataUtilsTest {
  private static final String PUBLIC_EXISTING_METHOD = "endsWith";
  private static final String PUBLIC_NOT_EXISTING_METHOD = "notexistingmethod";
  private static final String EXISTING_CLASS = "java.lang.String";
  private static final String NOT_EXISTING_CLASS = "java.lang.StringStringString";

  @Test
  void testMethodExistingPublicMethod() {
    ClassDataUtils.INSTANCE.initialize();
    boolean found =
        ClassDataUtils.INSTANCE.verifyMethodExists(
            String.class, PUBLIC_EXISTING_METHOD, String.class);
    assertThat(found).isTrue();
  }

  @Test
  void testMethodNotExistingPublicMethod() {
    ClassDataUtils.INSTANCE.initialize();
    boolean found =
        ClassDataUtils.INSTANCE.verifyMethodExists(String.class, PUBLIC_NOT_EXISTING_METHOD);
    assertThat(found).isFalse();
  }

  @Test
  void testClassExists() {
    ClassDataUtils.INSTANCE.initialize();
    boolean found = ClassDataUtils.INSTANCE.verifyClassExists(EXISTING_CLASS);
    assertThat(found).isTrue();
  }

  @Test
  void testClassDoesNotExist() {
    ClassDataUtils.INSTANCE.initialize();
    boolean found = ClassDataUtils.INSTANCE.verifyClassExists(NOT_EXISTING_CLASS);
    assertThat(found).isFalse();
  }
}
