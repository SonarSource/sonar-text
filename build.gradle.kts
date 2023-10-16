plugins {
  id("org.sonarsource.text.artifactory-configuration")
  id("org.sonarsource.text.rule-api")
  id("org.sonarsource.text.sonarqube")
  id("com.diffplug.blowdryer")
}

tasks.artifactoryPublish { skip = true }
