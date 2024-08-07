val rulApiVersion = "2.7.0.2612"

repositories {
    maven {
        url = project.uri("https://repox.jfrog.io/repox/sonarsource-private-releases")
        authentication {
            credentials {
                val artifactoryUsername: String? by project
                val artifactoryPassword: String? by project
                username = System.getenv("ARTIFACTORY_PRIVATE_USERNAME") ?: artifactoryUsername
                password = System.getenv("ARTIFACTORY_PRIVATE_PASSWORD") ?: artifactoryPassword
            }
        }
        content {
            includeGroup("com.sonarsource.rule-api")
        }
    }
    mavenCentral()
}

val ruleApi = configurations.create("ruleApi")

dependencies {
    ruleApi("com.sonarsource.rule-api:rule-api:$rulApiVersion")
}
