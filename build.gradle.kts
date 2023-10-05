plugins {
  id("org.sonarsource.text.artifactory-configuration")
  id("org.sonarsource.text.rule-api")
  id("com.diffplug.blowdryer")
}

tasks.artifactoryPublish { skip = true }

