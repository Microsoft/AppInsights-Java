plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web") {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }
  implementation("io.opentelemetry:opentelemetry-api:1.12.0")
}

configurations.all {
  resolutionStrategy.force("io.opentelemetry:opentelemetry-api:1.12.0")
}
