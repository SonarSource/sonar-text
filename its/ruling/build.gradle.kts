plugins {
  id("org.sonarsource.text.java-conventions")
  id("org.sonarsource.text.code-style-convention")
  id("org.sonarsource.text.integration-test")
  id("org.sonarqube")
}

dependencies {
  "integrationTestImplementation"(project(":sonar-text-plugin", configuration = "shadow"))
  "integrationTestImplementation"(libs.sonar.analyzer.commons)
  "integrationTestImplementation"(libs.sonar.orchestrator)
  "integrationTestImplementation"(libs.assertj.core)
}

sonar {
  isSkipProject = true
}
