plugins {
  id("ai.java-conventions")
}

dependencies {
  implementation(project(":agent:agent-profiler:agent-alerting-api"))
  implementation("org.slf4j:slf4j-api")

  testImplementation(platform("org.junit:junit-bom"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.assertj:assertj-core")
}
