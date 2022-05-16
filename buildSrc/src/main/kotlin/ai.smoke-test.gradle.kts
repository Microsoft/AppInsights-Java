import com.microsoft.applicationinsights.gradle.AiSmokeTestExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  `java-library`
}

val aiSmokeTest = extensions.create<AiSmokeTestExtension>("aiSmokeTest")

sourceSets {
  create("smokeTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
  }
}

val smokeTestImplementation by configurations.getting {
  extendsFrom(configurations.implementation.get())
}

configurations["smokeTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

// FIXME (trask) copy-pasted from ai.java-conventions.gradle
java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }

  // See https://docs.gradle.org/current/userguide/upgrading_version_5.html, Automatic target JVM version
  disableAutoTargetJvm()
  withJavadocJar()
  withSourcesJar()
}

// FIXME (trask) copy-pasted from ai.java-conventions.gradle
tasks.withType<JavaCompile>().configureEach {
  with(options) {
    release.set(8)
    compilerArgs.add("-Werror")
  }
}

// FIXME (trask) copy-pasted from ai.java-conventions.gradle
val dependencyManagement by configurations.creating {
  isCanBeConsumed = false
  isCanBeResolved = false
  isVisible = false
}

dependencies {
  // FIXME (trask) copy-pasted from ai.java-conventions.gradle
  dependencyManagement(platform(project(":dependencyManagement")))
  afterEvaluate {
    configurations.configureEach {
      if (isCanBeResolved && !isCanBeConsumed) {
        extendsFrom(dependencyManagement)
      }
    }
  }

  smokeTestImplementation(project(":smoke-tests:framework"))

  // NOTE not updating smoke tests to JUnit 5, because AiSmokeTest has deep dependency on JUnit 4 infra,
  // and so would take a good amount of work, and eventually want to migrate to otel smoke tests anyways
  smokeTestImplementation("org.hamcrest:hamcrest-core:1.3")
  smokeTestImplementation("org.hamcrest:hamcrest-library:1.3")
  smokeTestImplementation("junit:junit:4.13.2")
}

tasks {
  // This task addresses the issue of dependency containers not existing when the app runs and the test fails due to timeout.
  val pullDependencyContainers by registering {
    doLast {
      aiSmokeTest.dependencyContainers.get().forEach { dc ->
        logger.info("Pulling $dc...")
        exec {
          executable = "docker"
          args = listOf("pull", dc)
        }
      }
    }
  }

  task<Test>("smokeTest") {
    // this is just to force building the agent first
    dependsOn(":agent:agent:shadowJar")

    dependsOn(":smoke-tests:servers:buildDockerImage")
    dependsOn(assemble)
    dependsOn(pullDependencyContainers)

    testClassesDirs = sourceSets["smokeTest"].output.classesDirs
    classpath = sourceSets["smokeTest"].runtimeClasspath

    doFirst {

      val warFile = aiSmokeTest.testAppArtifactDir.file(aiSmokeTest.testAppArtifactFilename.get()).get()

      // need to delay for project to configure the extension
      jvmArgs("-Dai.smoketest.testAppWarFile=$warFile")

      // There's no real harm in setting this for all tests even if any happen to not be using
      // context propagation.
      jvmArgs("-Dio.opentelemetry.context.enableStrictContext=true")
      // TODO (trask): Have agent map unshaded to shaded.
      jvmArgs("-Dio.opentelemetry.javaagent.shaded.io.opentelemetry.context.enableStrictContext=true")
    }

    testLogging {
      showStandardStreams = true
      exceptionFormat = TestExceptionFormat.FULL
    }

    // TODO (trask) this is still a problem
    //  e.g. changes in agent-tooling do not cause smoke tests to re-run
    outputs.upToDateWhen { false }
  }
}
