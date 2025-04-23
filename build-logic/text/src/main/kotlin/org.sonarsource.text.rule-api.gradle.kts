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
            includeGroup("com.sonarsource.parent")
        }
    }
    mavenCentral()
}

val ruleApi = configurations.create("ruleApi")
val ruleApiVersion = "2.10.0.4287"

dependencies {
    ruleApi("com.sonarsource.rule-api:rule-api:$ruleApiVersion")
}
