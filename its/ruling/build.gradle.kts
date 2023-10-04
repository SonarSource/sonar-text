plugins {
  id("org.sonarsource.text.java-conventions")
  id("org.sonarsource.text.code-style-convention")
}

dependencies {
  testImplementation(project(":sonar-text-plugin", configuration = "shadow"))
  testImplementation(libs.sonar.analyzer.commons)
  testImplementation(libs.sonar.orchestrator)
  testImplementation(libs.assertj.core)
}

tasks.test {
  useJUnit()
}
