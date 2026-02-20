/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
plugins {
    id("org.sonarsource.cloud-native.code-style-conventions")
    id("org.sonarsource.cloud-native.artifactory-configuration")
    id("org.sonarsource.cloud-native.rule-api")
    id("org.sonarsource.text.sonarqube")
}

tasks.artifactoryPublish { skip = true }

val cliArtifacts = setOf("linux-x86-64", "linux-arm64", "macos-arm64", "windows-x64-64")
    .joinToString(",") { it -> "com.sonarsource.text:sonar-secrets:exe:$it" }

artifactoryConfiguration {
    buildName = providers.environmentVariable("PROJECT").orElse("sonar-text")
    artifactsToPublish = "org.sonarsource.text:sonar-text-plugin:jar," + cliArtifacts
    artifactsToDownload = ""
    repoKeyEnv = "ARTIFACTORY_DEPLOY_REPO"
    usernameEnv = "ARTIFACTORY_DEPLOY_USERNAME"
    passwordEnv = "ARTIFACTORY_DEPLOY_ACCESS_TOKEN"
}

spotless {
    // Mainly used to define spotless configuration for the build-logic
    encoding(Charsets.UTF_8)
    java {
        target("/build-logic/text/src/**/*.java")
        targetExclude("**/templates/*.java")
    }
    kotlinGradle {
        target("*.gradle.kts", "build-logic/text/*.gradle.kts", "/build-logic/text/src/**/*.gradle.kts")
    }
    kotlin {
        ktlint().setEditorConfigPath("$rootDir/build-logic/common/.editorconfig")
        target("/build-logic/text/src/**/*.kt")
        licenseHeaderFile(rootProject.file("LICENSE_HEADER"))
    }
}

ruleApi {
    languageToSonarpediaDirectory = mapOf(
        "Secrets" to "sonar-text-plugin/sonarpedia-secrets",
        "Text" to "sonar-text-plugin/sonarpedia-text"
    )
}

sonar {
    properties {
        property("sonar.projectName", "SonarText Enterprise")
        property("sonar.projectKey", System.getenv("SONAR_PROJECT_KEY"))
        property("sonar.organization", "sonarsource")
        property("sonar.exclusions", "**/build/**/*")
        property("sonar.links.ci", "https://cirrus-ci.com/github/SonarSource/sonar-text-enterprise")
        property("sonar.links.scm", "https://github.com/SonarSource/sonar-text-enterprise")
        property("sonar.links.issue", "https://jira.sonarsource.com/browse/SECRETS")

        properties["sonar.sources"] as MutableCollection<String> +=
            gradle.includedBuild("build-logic-text").projectDir.resolve("src/main/java").toString()
        property("sonar.sca.exclusions", "private/its/benchmark/src/integrationTest/resources/sources/**")

        val binaries = properties["sonar.java.binaries"] as? MutableCollection<String> ?: mutableSetOf()
        properties["sonar.java.binaries"] = binaries +
            gradle.includedBuild("build-logic-text").projectDir.resolve("build/classes/java/main").toString()
        val libraries = properties["sonar.java.libraries"] as? MutableCollection<String> ?: mutableSetOf()
        properties["sonar.java.libraries"] = libraries + buildscript.configurations.getByName("classpath")
        properties["sonar.coverage.jacoco.xmlReportPaths"] =
            gradle.includedBuild("build-logic-text").projectDir.resolve("build/reports/jacoco/test/jacocoTestReport.xml").toString()
    }
}
