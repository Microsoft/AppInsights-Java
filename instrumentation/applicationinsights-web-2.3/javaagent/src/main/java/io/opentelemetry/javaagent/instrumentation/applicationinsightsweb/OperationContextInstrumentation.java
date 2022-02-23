/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.applicationinsightsweb;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesNoArguments;

import com.microsoft.applicationinsights.extensibility.context.OperationContext;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.api.field.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class OperationContextInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("com.microsoft.applicationinsights.extensibility.context.OperationContext");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod().and(isPublic()).and(not(isStatic())).and(named("getId")).and(takesNoArguments()),
        OperationContextInstrumentation.class.getName() + "$GetIdAdvice");
    transformer.applyAdviceToMethod(
        isMethod().and(isPublic()).and(not(isStatic())).and(not(named("getId"))),
        OperationContextInstrumentation.class.getName() + "$OtherMethodsAdvice");
  }

  public static class GetIdAdvice {
    @Advice.OnMethodExit
    public static void methodExit(
        @Advice.This OperationContext operationContext,
        @Advice.Return(readOnly = false) String id) {
      Span span = VirtualField.find(OperationContext.class, Span.class).get(operationContext);
      if (span != null) {
        id = span.getSpanContext().getTraceId();
      }
    }
  }

  public static class OtherMethodsAdvice {
    @Advice.OnMethodEnter
    public static void methodEnter(
        @Advice.This OperationContext operationContext, @Advice.Origin("#m") String methodName) {
      Span span = VirtualField.find(OperationContext.class, Span.class).get(operationContext);
      if (span != null) {
        LogOnce.logOnce(
            "ThreadContext.getRequestTelemetryContext().getRequestTelemetry().getContext().getOperation()."
                + methodName
                + "() is not supported by the Application Insights for Java 3.x agent");
      }
    }
  }
}
