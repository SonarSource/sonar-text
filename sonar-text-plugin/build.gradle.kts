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
import org.sonarsource.cloudnative.gradle.enforceJarSize

plugins {
    id("org.sonarsource.cloud-native.sonar-plugin")
    id("org.sonarsource.text.code-generation")
    id("org.sonarsource.text.specification-files-processing")
    `java-library`
    `java-test-fixtures`
}

description = "SonarSource Text Analyzer :: Plugin"

dependencies {
    api(libs.sonar.analyzer.commons)
    api(libs.jackson.dataformat.smile)
    api(libs.com.networknt.jsonSchemaValidator)
    api(libs.eclipse.jgit)
    api(libs.ahocorasick)
    compileOnly(libs.sonar.plugin.api)
    compileOnly(libs.slf4j.api)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.sonar.plugin.api.test.fixtures)
    testImplementation(libs.sonar.plugin.api.impl)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.logback.classic)
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesImplementation(libs.junit.jupiter)
    testFixturesImplementation(libs.assertj.core)
    testFixturesImplementation(libs.sonar.plugin.api.test.fixtures)
    testFixturesImplementation(libs.sonar.plugin.api.impl)
    testFixturesImplementation(libs.mockito.core)
    testFixturesImplementation(libs.awaitility)
    testFixturesImplementation(libs.sonar.java.checks)
    testFixturesRuntimeOnly(libs.junit.platform.launcher)
}

// used to be done by sonar-packaging maven plugin
tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Plugin-ChildFirstClassLoader" to "false",
                "Plugin-Class" to "org.sonar.plugins.common.TextAndSecretsPlugin",
                "Plugin-Description" to "Analyzer for Text Files",
                "Plugin-Developers" to "SonarSource Team",
                "Plugin-Display-Version" to version,
                "Plugin-Homepage" to "https://sonarsource.atlassian.net/browse/SONARTEXT",
                "Plugin-IssueTrackerUrl" to "https://sonarsource.atlassian.net/browse/SONARTEXT",
                "Plugin-Key" to "text",
                "Plugin-License" to "SSALv1",
                "Plugin-Name" to "Text Code Quality and Security",
                "Plugin-Organization" to "SonarSource",
                "Plugin-OrganizationUrl" to "https://www.sonarsource.com",
                "Plugin-SourcesUrl" to "https://github.com/SonarSource/sonar-text",
                "Plugin-Version" to project.version,
                "Sonar-Version" to "9.8",
                "SonarLint-Supported" to "true",
                "Version" to project.version.toString(),
                "Jre-Min-Version" to java.sourceCompatibility.majorVersion
            )
        )
    }
}

tasks.shadowJar {
    minimizeJar = true
    exclude("META-INF/LICENSE*")
    exclude("META-INF/NOTICE*")
    exclude("META-INF/*.RSA")
    exclude("META-INF/*.SF")
    exclude("LICENSE*")
    exclude("NOTICE*")

    val logger = project.logger
    doLast {
        enforceJarSize(tasks.shadowJar.get().archiveFile.get().asFile, 6_500_000L, 8_500_000L, logger)
    }
}

publishing {
    publications.withType<MavenPublication> {
        artifact(tasks.shadowJar) {
            // remove `-all` suffix from the fat jar
            classifier = null
        }
        artifact(tasks.sourcesJar)
        artifact(tasks.javadocJar)
    }
}

publishingConfiguration {
    pomName = "SonarSource Text Analyzer"
    scmUrl = "https://github.com/SonarSource/sonar-text"

    license {
        name = "SSALv1"
        url = "https://sonarsource.com/license/ssal/"
        distribution = "repo"
    }
}

codeGeneration {
    packagePrefix = "org"
    baseTestClass = "org.sonar.plugins.secrets.utils.AbstractRuleExampleTest"
    excludedKeys = emptySet()
    checkListClassName = "SecretsCheckList"
    checkListClassesToEmbed = emptySet()
    specFileListClassName = "SecretsSpecificationFilesDefinition"
    licenseHeaderFile = rootProject.file("LICENSE_HEADER")
}
