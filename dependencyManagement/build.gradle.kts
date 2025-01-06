import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
  `java-platform`

  id("com.github.ben-manes.versions")
}

data class DependencySet(val group: String, val version: String, val modules: List<String>)

val dependencyVersions = hashMapOf<String, String>()
rootProject.extra["versions"] = dependencyVersions

val otelSdkVersion = "1.44.1"
val otelInstrumentationAlphaVersion = "2.10.0-alpha"
val otelInstrumentationVersion = "2.10.0"

rootProject.extra["otelSdkVersion"] = otelSdkVersion
rootProject.extra["otelInstrumentationVersion"] = otelInstrumentationVersion
rootProject.extra["otelInstrumentationAlphaVersion"] = otelInstrumentationAlphaVersion

val DEPENDENCY_BOMS = listOf(
  "com.fasterxml.jackson:jackson-bom:2.18.2",
  "io.opentelemetry:opentelemetry-bom:${otelSdkVersion}",
  "io.opentelemetry:opentelemetry-bom-alpha:${otelSdkVersion}-alpha",
  "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:${otelInstrumentationVersion}",
  "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:${otelInstrumentationAlphaVersion}",
  "com.azure:azure-sdk-bom:1.2.29",
  "io.netty:netty-bom:4.1.116.Final",
  "org.junit:junit-bom:5.11.4",
  "org.testcontainers:testcontainers-bom:1.20.4",
)

val autoServiceVersion = "1.1.1"
val autoValueVersion = "1.11.0"
val errorProneVersion = "2.34.0"
val jmhVersion = "1.37"
val mockitoVersion = "4.11.0"
val slf4jVersion = "2.0.16"

val CORE_DEPENDENCIES = listOf(
  "io.opentelemetry:opentelemetry-semconv:1.30.1-alpha",
  "io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator:${otelInstrumentationAlphaVersion}",
  "com.google.auto.service:auto-service:${autoServiceVersion}",
  "com.google.auto.service:auto-service-annotations:${autoServiceVersion}",
  "com.google.auto.value:auto-value:${autoValueVersion}",
  "com.google.auto.value:auto-value-annotations:${autoValueVersion}",
  "com.google.errorprone:error_prone_annotations:${errorProneVersion}",
  "com.google.errorprone:error_prone_core:${errorProneVersion}",
  "org.openjdk.jmh:jmh-core:${jmhVersion}",
  "org.openjdk.jmh:jmh-generator-bytecode:${jmhVersion}",
  "org.mockito:mockito-core:${mockitoVersion}",
  "org.mockito:mockito-junit-jupiter:${mockitoVersion}",
  "org.mockito:mockito-inline:${mockitoVersion}",
  "org.slf4j:slf4j-api:${slf4jVersion}",
  "org.slf4j:log4j-over-slf4j:${slf4jVersion}",
  "org.slf4j:jcl-over-slf4j:${slf4jVersion}",
  "org.slf4j:jul-to-slf4j:${slf4jVersion}",
  "io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:${otelInstrumentationAlphaVersion}",
  "io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:${otelInstrumentationAlphaVersion}",
  "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:${otelInstrumentationAlphaVersion}",
)

val DEPENDENCIES = listOf(
  "ch.qos.logback:logback-classic:1.3.14", // logback 1.4+ requires Java 11+
  "ch.qos.logback.contrib:logback-json-classic:0.1.5",
  "com.uber.nullaway:nullaway:0.12.2",
  "commons-codec:commons-codec:1.17.1",
  "org.apache.commons:commons-text:1.13.0",
  "com.google.code.gson:gson:2.11.0",
  "com.azure:azure-core-test:1.26.2", // this is not included in azure-sdk-bom
  "org.assertj:assertj-core:3.27.2",
  "org.awaitility:awaitility:4.2.2",
  "io.github.hakky54:logcaptor:2.10.0",
  "io.opentelemetry.contrib:opentelemetry-jfr-connection:1.42.0-alpha",
  "com.google.code.findbugs:jsr305:3.0.2",
  "com.github.spotbugs:spotbugs-annotations:4.8.6"
)

javaPlatform {
  allowDependencies()
}

dependencies {
  for (bom in DEPENDENCY_BOMS) {
    api(enforcedPlatform(bom))
    val split = bom.split(':')
    dependencyVersions[split[0]] = split[2]
  }
  constraints {
    for (dependency in CORE_DEPENDENCIES) {
      api(dependency)
      val split = dependency.split(':')
      dependencyVersions[split[0]] = split[2]
    }
    for (dependency in DEPENDENCIES) {
      api(dependency)
      val split = dependency.split(':')
      dependencyVersions[split[0]] = split[2]
    }
  }
}

fun isNonStable(version: String): Boolean {
  val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
  val regex = "^[0-9,.v-]+(-r)?$".toRegex()
  val isGuava = version.endsWith("-jre")
  val isStable = stableKeyword || regex.matches(version) || isGuava
  return isStable.not()
}

tasks {
  named<DependencyUpdatesTask>("dependencyUpdates") {
    revision = "release"
    checkConstraints = true

    rejectVersionIf {
      isNonStable(candidate.version)
    }
  }
}
