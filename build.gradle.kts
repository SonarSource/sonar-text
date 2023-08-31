plugins {
  id("org.sonarsource.text.artifactory-configuration")
  id("com.diffplug.blowdryer")
}

tasks.artifactoryPublish { skip = true }
