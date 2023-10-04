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
  }
}

