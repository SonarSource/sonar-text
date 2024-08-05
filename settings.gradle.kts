pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven {
            name = "artifactory"
            url = uri("https://repox.jfrog.io/repox/sonarsource")
            val artifactoryUsername =
                providers.environmentVariable("ARTIFACTORY_PRIVATE_USERNAME")
                    .orElse(providers.gradleProperty("artifactoryUsername"))
            val artifactoryPassword =
                providers.environmentVariable("ARTIFACTORY_PRIVATE_PASSWORD")
                    .orElse(providers.gradleProperty("artifactoryPassword"))

            if (artifactoryUsername.isPresent && artifactoryPassword.isPresent) {
                authentication {
                    credentials {
                        username = artifactoryUsername.get()
                        password = artifactoryPassword.get()
                    }
                }
            }
        }
    }
}

plugins {
    id("com.diffplug.blowdryerSetup") version "1.7.1"
}

rootProject.name = "text"
includeBuild("build-logic")
include(":sonar-text-plugin")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://repox.jfrog.io/repox/sonarsource-private-releases")
            val artifactoryUsername =
                providers.environmentVariable("ARTIFACTORY_PRIVATE_USERNAME")
                    .orElse(providers.gradleProperty("artifactoryUsername"))
            val artifactoryPassword =
                providers.environmentVariable("ARTIFACTORY_PRIVATE_PASSWORD")
                    .orElse(providers.gradleProperty("artifactoryPassword"))

            if (artifactoryUsername.isPresent && artifactoryPassword.isPresent) {
                authentication {
                    credentials {
                        username = artifactoryUsername.get()
                        password = artifactoryPassword.get()
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
