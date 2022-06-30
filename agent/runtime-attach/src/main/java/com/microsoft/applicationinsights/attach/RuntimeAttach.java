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

package com.microsoft.applicationinsights.attach;

import java.io.File;
import java.lang.management.ManagementFactory;
import net.bytebuddy.agent.ByteBuddyAgent;

/** To remove after next OTel java contrib release */
final class RuntimeAttach {

  private static final String AGENT_ENABLED_PROPERTY = "otel.javaagent.enabled";
  private static final String AGENT_ENABLED_ENV_VAR = "OTEL_JAVAAGENT_ENABLED";
  static final String MAIN_METHOD_CHECK_PROP =
      "otel.javaagent.testing.runtime-attach.main-method-check";

  /**
   * Attach the OpenTelemetry Java agent to the current JVM. The attachment must be requested at the
   * beginning of the main method.
   *
   * @param otelAgentFile OpenTelemetry agent file
   */
  public static void attachJavaagentToCurrentJvm(File otelAgentFile) {
    if (!shouldAttach()) {
      return;
    }
    ByteBuddyAgent.attach(otelAgentFile, getPid());

    if (!agentIsAttached()) {
      printError("Agent was not attached. An unexpected issue has happened.");
    }
  }

  @SuppressWarnings("SystemOut")
  private static void printError(String message) {
    System.err.println(message);
  }

  private static boolean shouldAttach() {
    if (agentIsDisabledWithProp()) {
      return false;
    }
    if (agentIsDisabledWithEnvVar()) {
      return false;
    }
    if (agentIsAttached()) {
      return false;
    }
    if (mainMethodCheckIsEnabled() && !isMainThread()) {
      printError(
          "Agent is not attached because runtime attachment was not requested from main thread.");
      return false;
    }
    if (mainMethodCheckIsEnabled() && !isMainMethod()) {
      printError(
          "Agent is not attached because runtime attachment was not requested from main method.");
      return false;
    }
    return true;
  }

  private static boolean agentIsDisabledWithProp() {
    String agentEnabledPropValue = System.getProperty(AGENT_ENABLED_PROPERTY);
    return "false".equalsIgnoreCase(agentEnabledPropValue);
  }

  private static boolean agentIsDisabledWithEnvVar() {
    String agentEnabledEnvVarValue = System.getenv(AGENT_ENABLED_ENV_VAR);
    return "false".equals(agentEnabledEnvVarValue);
  }

  private static boolean agentIsAttached() {
    try {
      Class.forName("io.opentelemetry.javaagent.OpenTelemetryAgent", false, null);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private static boolean mainMethodCheckIsEnabled() {
    String mainThreadCheck = System.getProperty(MAIN_METHOD_CHECK_PROP);
    return !"false".equals(mainThreadCheck);
  }

  private static boolean isMainThread() {
    Thread currentThread = Thread.currentThread();
    return "main".equals(currentThread.getName());
  }

  static boolean isMainMethod() {
    StackTraceElement bottomOfStack = findBottomOfStack(Thread.currentThread());
    String methodName = bottomOfStack.getMethodName();
    return "main".equals(methodName);
  }

  private static StackTraceElement findBottomOfStack(Thread thread) {
    StackTraceElement[] stackTrace = thread.getStackTrace();
    return stackTrace[stackTrace.length - 1];
  }

  private static String getPid() {
    return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
  }

  private RuntimeAttach() {}
}
