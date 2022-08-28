import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.InventoryMarkdownReportRenderer

plugins {
  id("com.github.jk1.dependency-license-report")

  id("ai.java-conventions")
  id("ai.shadow-conventions")
  id("ai.publish-conventions")
}

base.archivesName.set("applicationinsights-agent")

// this configuration collects libs that will be placed in the bootstrap classloader
val bootstrapLibs by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}
// this configuration collects libs that will be placed in the agent classloader, isolated from the instrumented application code
val javaagentLibs by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}
// this configuration stores the upstream agent dep that's extended by this project
val upstreamAgent: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}

val otelInstrumentationVersion: String by project
val otelInstrumentationAlphaVersion: String by project

val licenseReportDependencies by configurations.creating {
  extendsFrom(bootstrapLibs)
}

dependencies {

  // required to access OpenTelemetryAgent
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:$otelInstrumentationAlphaVersion")

  bootstrapLibs(project(":agent:agent-bootstrap"))

  javaagentLibs(project(":agent:agent-tooling"))

  upstreamAgent("io.opentelemetry.javaagent:opentelemetry-javaagent:$otelInstrumentationVersion")

  licenseReportDependencies(project(":agent:agent-tooling"))
}

val javaagentDependencies = dependencies

// collect all instrumentation sub projects
project(":agent:instrumentation").subprojects {
  val subProj = this
  plugins.withId("ai.javaagent-instrumentation") {
    javaagentDependencies.run {
      add(javaagentLibs.name, project(subProj.path))
    }
  }
}

tasks {
  processResources {
    from(rootProject.file("licenses")) {
      into("META-INF/licenses")
    }
  }

  // building the final javaagent jar is done in 3 steps:

  // 1. all distro-specific javaagent libs are relocated (by the ai.shadow-conventions plugin)
  val relocateJavaagentLibs by registering(ShadowJar::class) {
    configurations = listOf(javaagentLibs)

    duplicatesStrategy = DuplicatesStrategy.FAIL

    archiveFileName.set("javaagentLibs-relocated.jar")

    dependencies {
      // exclude known bootstrap dependencies - they can't appear in the inst/ directory
      exclude(dependency("org.slf4j:slf4j-api"))
      exclude(dependency("io.opentelemetry:opentelemetry-api"))
      exclude(dependency("io.opentelemetry:opentelemetry-api-metrics"))
      exclude(dependency("io.opentelemetry:opentelemetry-context"))
      exclude(dependency("io.opentelemetry:opentelemetry-semconv"))

      // TODO (trask) Azure SDK: why is this included in azure-core?
      exclude(dependency("io.netty:netty-tcnative-boringssl-static"))
    }
  }

  // 2. the distro javaagent libs are then isolated - moved to the inst/ directory
  // having a separate task for isolating javaagent libs is required to avoid duplicates with the upstream javaagent
  // duplicatesStrategy in shadowJar won't be applied when adding files with with(CopySpec) because each CopySpec has
  // its own duplicatesStrategy
  val isolateJavaagentLibs by registering(Copy::class) {
    dependsOn(relocateJavaagentLibs)
    isolateClasses(relocateJavaagentLibs.get().outputs.files)

    into("$buildDir/isolated/javaagentLibs")
  }

  // 3. the relocated and isolated javaagent libs are merged together with the bootstrap libs (which undergo relocation
  // in this task) and the upstream javaagent jar; duplicates are removed
  val shadowJarWithDuplicates by registering(ShadowJar::class) {
    configurations = listOf(bootstrapLibs, upstreamAgent)

    // using logback in this distro
    // this excludes slf4j-simple from the upstream agent
    // but it doesn't exclude logback's files in this package since they haven't been shaded
    // into this package yet at the time exclusion takes place
    exclude("io/opentelemetry/javaagent/slf4j/impl/**")

    exclude("io/opentelemetry/javaagent/shaded/instrumentation/api/instrumenter/http/HttpClientMetrics.class")
    exclude("io/opentelemetry/javaagent/shaded/instrumentation/api/instrumenter/http/HttpServerMetrics.class")
    exclude("io/opentelemetry/javaagent/shaded/instrumentation/api/instrumenter/http/TemporaryMetricsView.class")

    exclude("io/opentelemetry/javaagent/shaded/instrumentation/api/instrumenter/rpc/RpcClientMetrics.class")
    exclude("io/opentelemetry/javaagent/shaded/instrumentation/api/instrumenter/rpc/RpcServerMetrics.class")
    exclude("io/opentelemetry/javaagent/shaded/instrumentation/api/instrumenter/rpc/TemporaryMetricsView.class")

    dependsOn(isolateJavaagentLibs)
    from(isolateJavaagentLibs.get().outputs)

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    archiveClassifier.set("dontuse")

    manifest {
      attributes(jar.get().manifest.attributes)
      attributes(
        "Agent-Class" to "com.microsoft.applicationinsights.agent.Agent",
        "Premain-Class" to "com.microsoft.applicationinsights.agent.Agent",
        "Can-Redefine-Classes" to true,
        "Can-Retransform-Classes" to true
      )
    }
  }

  // a separate task is needed to get rid of duplicates
  shadowJar {
    archiveClassifier.set("")

    dependsOn(shadowJarWithDuplicates)

    from(zipTree(shadowJarWithDuplicates.get().archiveFile))

    manifest {
      attributes(shadowJarWithDuplicates.get().manifest.attributes)
    }
  }

  jar {
    enabled = false
  }

  assemble {
    dependsOn(shadowJar)
  }

  val cleanLicenses by registering(Delete::class) {
    delete(rootProject.file("licenses"))
  }

  named("generateLicenseReport").configure {
    dependsOn(cleanLicenses)
  }

  // Because we reconfigure publishing to only include the shadow jar, the Gradle metadata is not correct.
  // Since we are fully bundled and have no dependencies, Gradle metadata wouldn't provide any advantage over
  // the POM anyways so in practice we shouldn't be losing anything.
  withType<GenerateModuleMetadata>().configureEach {
    enabled = false
  }
}

// Don't publish non-shadowed jar (shadowJar is in shadowRuntimeElements)
with(components["java"] as AdhocComponentWithVariants) {
  configurations.forEach {
    withVariantsFromConfiguration(configurations["apiElements"]) {
      skip()
    }
    withVariantsFromConfiguration(configurations["runtimeElements"]) {
      skip()
    }
  }
}

licenseReport {
  outputDir = rootProject.file("licenses").absolutePath

  renderers = arrayOf(InventoryMarkdownReportRenderer("more-licenses.md"))

  configurations = arrayOf(licenseReportDependencies.name)

  excludeBoms = true

  excludeGroups = arrayOf(
    "applicationinsights-java.*"
  )

  excludes = arrayOf(
    "io.opentelemetry:opentelemetry-bom-alpha",
    "io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha"
  )

  filters = arrayOf(LicenseBundleNormalizer("$projectDir/license-normalizer-bundle.json", true))
}

fun CopySpec.isolateClasses(jars: Iterable<File>) {
  jars.forEach {
    from(zipTree(it)) {
      into("inst")
      rename("^(.*)\\.class\$", "\$1.classdata")
      // Rename LICENSE file since it clashes with license dir on non-case sensitive FSs (i.e. Mac)
      rename("""^LICENSE$""", "LICENSE.renamed")
    }
  }
  from("${rootProject.projectDir}/LICENSE") {
    into("META-INF")
  }
}
