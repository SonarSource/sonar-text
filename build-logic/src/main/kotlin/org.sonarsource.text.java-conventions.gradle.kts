plugins {
  `java-library`
  id("org.sonarqube")
}

java {
  withSourcesJar()
  withJavadocJar()
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<JavaCompile> {
  options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
  options.encoding = "UTF-8"
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
