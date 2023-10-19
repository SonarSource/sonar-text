plugins {
    id("org.sonarqube")
}

sonar {
    properties {
        property("sonar.projectKey", "org.sonarsource.text:text")
        property("sonar.exclusions", "**/build/**/*")
        property("sonar.links.ci", "https://cirrus-ci.com/github/SonarSource/sonar-text-enterprise")
        property("sonar.links.scm", "https://github.com/SonarSource/sonar-text-enterprise")
        property("sonar.links.issue", "https://jira.sonarsource.com/browse/SECRETS")
    }
}
