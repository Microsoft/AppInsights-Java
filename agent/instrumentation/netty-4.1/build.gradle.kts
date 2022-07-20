plugins {
  id("ai.java-conventions")
  id("ai.javaagent-instrumentation")
}

val otelInstrumentationAlphaVersion: String by project

dependencies {
  compileOnly("io.netty:netty-codec-http:4.1.0.Final")
  compileOnly("io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.1:$otelInstrumentationAlphaVersion")

  add("codegen", "io.opentelemetry.javaagent.instrumentation:opentelemetry-javaagent-netty-4.1:$otelInstrumentationAlphaVersion")

  // TODO remove when start using io.opentelemetry.instrumentation.javaagent-instrumentation plugin
  add("codegen", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationAlphaVersion")
  add("muzzleBootstrap", "io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations-support:$otelInstrumentationAlphaVersion")
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:$otelInstrumentationAlphaVersion")
  add("muzzleTooling", "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$otelInstrumentationAlphaVersion")
}
