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
import org.sonarsource.text.KOTLIN_GRADLE_DELIMITER
import org.sonarsource.text.registerRuleApiTasks

plugins {
    alias(libs.plugins.spotless)
    id("org.sonarsource.text.artifactory-configuration")
    id("org.sonarsource.text.rule-api")
    id("org.sonarsource.text.sonarqube")
    id("com.diffplug.blowdryer")
}

tasks.artifactoryPublish { skip = true }

artifactoryConfiguration {
    artifactsToPublish = "org.sonarsource.text:sonar-text-plugin:jar"
    artifactsToDownload = ""
    repoKeyEnv = "ARTIFACTORY_DEPLOY_REPO"
    usernameEnv = "ARTIFACTORY_DEPLOY_USERNAME"
    passwordEnv = "ARTIFACTORY_DEPLOY_PASSWORD"
}

spotless {
    // Mainly used to define spotless configuration for the build-logic
    encoding(Charsets.UTF_8)
    java {
        target("/build-logic/src/**/*.java")
        licenseHeaderFile(rootProject.file("LICENSE_HEADER")).updateYearWithLatest(true)
    }
    kotlinGradle {
        ktlint().setEditorConfigPath("$rootDir/.editorconfig")
        target("*.gradle.kts", "build-logic/*.gradle.kts", "/build-logic/src/**/*.gradle.kts")
        licenseHeaderFile(
            rootProject.file("LICENSE_HEADER"),
            KOTLIN_GRADLE_DELIMITER
        ).updateYearWithLatest(true)
    }
    kotlin {
        ktlint().setEditorConfigPath("$rootDir/.editorconfig")
        target("/build-logic/src/**/*.kt")
        licenseHeaderFile(rootProject.file("LICENSE_HEADER")).updateYearWithLatest(true)
    }
    format("javaMisc") {
        target("/build-logic/src/**/package-info.java")
        licenseHeaderFile(rootProject.file("LICENSE_HEADER"), "@javax.annotation").updateYearWithLatest(true)
    }
}

registerRuleApiTasks("Secrets", file("$projectDir/sonar-text-plugin/sonarpedia-secrets"))
registerRuleApiTasks("Text", file("$projectDir/sonar-text-plugin/sonarpedia-text"))

tasks.register("ruleApiUpdate") {
    description = "Update ALL rules description"
    group = "Rule API"
    dependsOn("ruleApiUpdateSecrets", "ruleApiUpdateText")
}

sonar {
    properties {
        properties["sonar.sources"] as MutableCollection<String> +=
            gradle.includedBuild("build-logic").projectDir.resolve("src/main/java").toString()

        val binaries = properties["sonar.java.binaries"] as? MutableCollection<String> ?: mutableSetOf()
        properties["sonar.java.binaries"] = binaries +
            gradle.includedBuild("build-logic").projectDir.resolve("build/classes/java/main").toString()
        val libraries = properties["sonar.java.libraries"] as? MutableCollection<String> ?: mutableSetOf()
        properties["sonar.java.libraries"] = libraries + buildscript.configurations.getByName("classpath")
        properties["sonar.coverage.jacoco.xmlReportPaths"] =
            gradle.includedBuild("build-logic").projectDir.resolve("build/reports/jacoco/test/jacocoTestReport.xml").toString()
    }
}
