plugins {
  id("ai.java-conventions")
}

dependencies {
  implementation("com.google.guava:guava:30.1.1-jre")
  compileOnly("javax.servlet:javax.servlet-api:3.0.1")
  implementation("com.google.code.gson:gson")
  implementation(project(":test:smoke:framework:utils"))
}
