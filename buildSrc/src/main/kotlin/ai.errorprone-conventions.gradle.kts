import net.ltgt.gradle.errorprone.errorprone

plugins {
  id("net.ltgt.errorprone")
}

dependencies {
  errorprone("com.google.errorprone:error_prone_core")
}

val disableErrorProne = gradle.startParameter.projectProperties.get("disableErrorProne")?.toBoolean()
  ?: false

tasks {
  withType<JavaCompile>().configureEach {
    options.errorprone {
      isEnabled.set(!disableErrorProne)

      disableWarningsInGeneratedCode.set(true)
      allDisabledChecksAsWarnings.set(true)

      excludedPaths.set(".*/build/generated/.*")

      if (System.getenv("CI") == null) {
        disable("SystemOut")
      }

      // Still Java 8
      disable("Varifier")

      // Intellij does a nice job of displaying parameter names
      disable("BooleanParameter")

      // Needed for legacy 2.x bridge
      disable("JavaUtilDate")

      // Doesn't work well with Java 8
      disable("FutureReturnValueIgnored")

      // Require Guava
      disable("AutoValueImmutableFields")
      disable("StringSplitter")
      disable("ImmutableMemberCollection")

      // Don't currently use this (to indicate a local variable that's mutated) but could
      // consider for future.
      disable("Var")

      // Don't support Android without desugar
      disable("AndroidJdkLibsChecker")
      disable("Java7ApiChecker")
      disable("StaticOrDefaultInterfaceMethod")

      // needed temporarily while hosting azure-monitor-opentelemetry-exporter in this repo
      disable("MissingSummary")
      disable("UnnecessaryDefaultInEnumSwitch")
      disable("InconsistentOverloads")

      if (name.contains("Jmh")) {
        disable("MemberName")
      }
    }
  }
}
