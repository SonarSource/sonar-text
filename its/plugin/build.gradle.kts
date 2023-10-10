plugins {
  id("org.sonarsource.text.java-conventions")
  id("org.sonarsource.text.code-style-convention")
  id("org.sonarsource.text.integration-test")
  id("org.sonarqube")
}

dependencies {
  "integrationTestImplementation"(project(":sonar-text-plugin", configuration = "shadow"))
  "integrationTestImplementation"(libs.sonar.orchestrator)
  "integrationTestImplementation"(libs.sonar.plugin.api)
  "integrationTestImplementation"(libs.junit.jupiter)
  "integrationTestImplementation"(libs.assertj.core)
  "integrationTestImplementation"(libs.sonar.ws)
}

sonar {
  isSkipProject = true
}

tasks.integrationTest {
  filter {
    includeTestsMatching("org.sonarsource.text.Tests")
  }
}
