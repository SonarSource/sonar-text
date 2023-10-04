plugins {
  id("org.sonarqube")
}

sonarqube {
  properties {
    property("sonar.projectKey", "org.sonarsource.text:text")
    property("sonar.exclusions", "**/build/**/*")
    property("sonar.links.ci", "https://cirrus-ci.com/github/SonarSource/sonar-text")
    property("sonar.links.scm", "https://github.com/SonarSource/sonar-text")
    property("sonar.links.issue", "https://jira.sonarsource.com/browse/SONARTEXT")
  }
}
