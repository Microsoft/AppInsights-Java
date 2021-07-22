plugins {
  id("ai.java-conventions")
}

dependencies {
  implementation(project(":agent:agent-profiler:agent-alerting-api"))
  implementation("com.azure:azure-core")
  implementation("com.squareup.moshi:moshi")
  implementation("org.slf4j:slf4j-api")
}
