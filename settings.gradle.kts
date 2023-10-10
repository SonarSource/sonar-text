pluginManagement {
  repositories {
    maven {
      name = "artifactory"
      url = uri("https://repox.jfrog.io/repox/sonarsource")
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins {
  id("com.diffplug.blowdryerSetup") version "1.7.0"
}

rootProject.name = "text"
includeBuild("build-logic")
include(":sonar-text-plugin")
include(":its:plugin")
include(":its:ruling")

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    maven {
      url = uri("https://repox.jfrog.io/repox/sonarsource-private-releases")

      val artifactoryUsername = System.getenv("ARTIFACTORY_PRIVATE_USERNAME")
          ?: providers.gradleProperty("artifactoryUsername").getOrElse("")
      val artifactoryPassword = System.getenv("ARTIFACTORY_PRIVATE_PASSWORD")
          ?: providers.gradleProperty("artifactoryPassword").getOrElse("")

      if (artifactoryUsername != "" && artifactoryPassword != "") {
        authentication {
          credentials {
            username = artifactoryUsername
            password = artifactoryPassword
          }
        }
      }
    }
  }
}

// "extraSettings.gradle" should not be renamed "settings.gradle" to not create a wrong project rootDir
var extraSettings = File(rootDir, "private/extraSettings.gradle.kts")
if (extraSettings.exists()) {
  apply(extraSettings)
}
