plugins {
  id("org.sonarsource.text.java-conventions")
  id("org.sonarsource.text.code-style-convention")
  id("org.sonarsource.text.integration-test")
}

dependencies {
  "integrationTestImplementation"(project(":sonar-text-plugin", configuration = "shadow"))
  "integrationTestImplementation"(libs.sonar.analyzer.commons)
  "integrationTestImplementation"(libs.sonar.orchestrator)
  "integrationTestImplementation"(libs.assertj.core)
}

