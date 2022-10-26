plugins {
  id("ai.smoke-test-jar")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter")

  // needs to be same version used in fakeIngestion server
  implementation("org.eclipse.jetty:jetty-server:9.4.7.v20170914")
}
