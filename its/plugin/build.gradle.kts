plugins {
  id("org.sonarsource.text.java-conventions")
  id("org.sonarsource.text.code-style-convention")
}

dependencies {
  testImplementation(project(":sonar-text-plugin", configuration = "shadow"))
  testImplementation(libs.sonar.orchestrator)
  testImplementation(libs.sonar.plugin.api)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.assertj.core)
  testImplementation(libs.sonar.ws)
}

tasks.test {
  useJUnit()
  filter {
    includeTestsMatching("org.sonarsource.text.Tests")
  }
}
