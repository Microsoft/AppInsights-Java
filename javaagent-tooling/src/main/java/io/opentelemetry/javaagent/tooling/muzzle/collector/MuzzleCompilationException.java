/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.tooling.muzzle.collector;

final class MuzzleCompilationException extends RuntimeException {
  MuzzleCompilationException(String message) {
    super(message);
  }
}
