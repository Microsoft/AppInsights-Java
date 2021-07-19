plugins {
  id("com.github.johnrengelman.shadow")

  id("otel.java-conventions")
  id("otel.publish-conventions")
}

description = "opentelemetry-extension-annotations shaded for internal javaagent usage"
group = "io.opentelemetry.javaagent"

dependencies {
  implementation("io.opentelemetry:opentelemetry-extension-annotations")
  implementation("io.opentelemetry:opentelemetry-context")
}

// OpenTelemetry API shaded so that it can be used in instrumentation of OpenTelemetry API itself,
// and then its usage can be unshaded after OpenTelemetry API is shaded
// (see more explanation in opentelemetry-api-1.0.gradle)
tasks {
  shadowJar {
    relocate("io.opentelemetry", "application.io.opentelemetry")
  }
}
