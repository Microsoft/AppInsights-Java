plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("com.microsoft.azure:applicationinsights-web") {
    // applicationinsights-core is embedded in applicationinsights-web
    // and duplicate class files produces lots of warning messages on jetty
    exclude("com.microsoft.azure", "applicationinsights-core")
  }
  implementation("redis.clients:jedis:2+")
}
