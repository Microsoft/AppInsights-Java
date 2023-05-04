plugins {
  `java`
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}


dependencies {
  compileOnly("org.gradle.jfr.polyfill:jfr-polyfill:1.0.0")
}
