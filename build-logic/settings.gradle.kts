dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://repox.jfrog.io/repox/sonarsource")

            val artifactoryUsername =
                System.getenv("ARTIFACTORY_PRIVATE_USERNAME")
                    ?: providers.gradleProperty("artifactoryUsername").getOrElse("")
            val artifactoryPassword =
                System.getenv("ARTIFACTORY_PRIVATE_PASSWORD")
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
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

pluginManagement {
    repositories {
        maven {
            url = uri("https://repox.jfrog.io/repox/sonarsource")

            val artifactoryUsername =
                System.getenv("ARTIFACTORY_PRIVATE_USERNAME")
                    ?: providers.gradleProperty("artifactoryUsername").getOrElse("")
            val artifactoryPassword =
                System.getenv("ARTIFACTORY_PRIVATE_PASSWORD")
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
        mavenCentral()
        gradlePluginPortal()
    }
}
