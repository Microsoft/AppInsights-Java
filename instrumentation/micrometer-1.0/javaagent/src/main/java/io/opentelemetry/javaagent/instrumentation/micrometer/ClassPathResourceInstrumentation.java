/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.micrometer;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.javaagent.bootstrap.AgentInitializer;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.io.InputStream;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.core.io.ClassPathResource;

// TODO consider applying this instrumentation more generally on ClassLoaders
public final class ClassPathResourceInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("org.springframework.core.io.ClassPathResource");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        named("getInputStream").and(takesArguments(0)).and(returns(InputStream.class)),
        ClassPathResourceInstrumentation.class.getName() + "$GetInputStreamAdvice");
  }

  public static class GetInputStreamAdvice {
    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static InputStream onEnter(@Advice.This ClassPathResource resource) {
      if ("io/opentelemetry/javaagent/instrumentation/micrometer/AzureMonitorAutoConfiguration.class"
          .equals(resource.getPath())) {
        ClassLoader agentClassLoader = AgentInitializer.getExtensionsClassLoader();
        if (agentClassLoader != null) {
          return agentClassLoader.getResourceAsStream(
              "io/opentelemetry/javaagent/instrumentation/micrometer/AzureMonitorAutoConfiguration.class");
        }
      }
      return null;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(
        @Advice.Return(readOnly = false) InputStream result,
        @Advice.Enter InputStream resultFromAgentLoader) {

      if (resultFromAgentLoader != null) {
        result = resultFromAgentLoader;
      }
    }
  }
}
