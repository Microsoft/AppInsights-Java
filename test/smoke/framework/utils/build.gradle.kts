plugins {
  id("ai.java-conventions")
}

dependencies {
  implementation("com.google.code.gson:gson")
  implementation("org.apache.httpcomponents:httpclient:4.5.13")
  implementation("org.apache.commons:commons-lang3:3.7")
  implementation("org.hamcrest:hamcrest-library:1.3")

  testImplementation("org.assertj:assertj-core")
}
