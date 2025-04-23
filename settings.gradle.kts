/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
pluginManagement {
    includeBuild("build-logic/text") {
        name = "build-logic-text"
    }
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
