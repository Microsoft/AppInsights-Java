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

package io.opentelemetry.instrumentation.api.instrumenter;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;

import io.opentelemetry.api.common.AttributeKey;

public final class BootstrapSemanticAttributes {

  // replaced by ai.preview.connection_string
  @Deprecated
  public static final AttributeKey<String> INSTRUMENTATION_KEY =
      AttributeKey.stringKey("ai.preview.instrumentation_key");

  public static final AttributeKey<String> CONNECTION_STRING =
      AttributeKey.stringKey("ai.preview.connection_string");

  public static final AttributeKey<String> ROLE_NAME =
      AttributeKey.stringKey("ai.preview.service_name");

  public static final AttributeKey<Boolean> IS_SYNTHETIC =
      booleanKey("applicationinsights.internal.is_synthetic");
  public static final AttributeKey<Boolean> IS_PRE_AGGREGATED =
      booleanKey("applicationinsights.internal.is_pre_aggregated");

  private BootstrapSemanticAttributes() {}
}
