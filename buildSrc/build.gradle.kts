plugins {
  `java-gradle-plugin`
  `kotlin-dsl`
  // When updating, update below in dependencies too
  id("com.diffplug.spotless") version "6.25.0"
}

spotless {
  java {
    googleJavaFormat()
    licenseHeaderFile(rootProject.file("../buildscripts/spotless.license.java"), "(package|import|public)")
    target("src/**/*.java")
  }
}

repositories {
  mavenCentral()
  mavenLocal()
  gradlePluginPortal()
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

dependencies {
  implementation(gradleApi())

  // When updating, update above in plugins too
  implementation("com.diffplug.spotless:spotless-plugin-gradle:6.25.0")
  implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.0.25")
  implementation("com.github.johnrengelman:shadow:8.1.1")
  implementation("com.gradle.enterprise:com.gradle.enterprise.gradle.plugin:3.18.1")

  implementation("org.owasp:dependency-check-gradle:10.0.4")

  implementation("io.opentelemetry.instrumentation:gradle-plugins:2.8.0-alpha")

  implementation("net.ltgt.gradle:gradle-errorprone-plugin:4.0.1")
  implementation("net.ltgt.gradle:gradle-nullaway-plugin:2.0.0")

  implementation("gradle.plugin.io.morethan.jmhreport:gradle-jmh-report:0.9.6")
  implementation("me.champeau.jmh:jmh-gradle-plugin:0.7.2")

  // earlier versions aren't compatible with Gradle 8.1.1
  implementation("org.springframework.boot:spring-boot-gradle-plugin:2.5.12")
}
