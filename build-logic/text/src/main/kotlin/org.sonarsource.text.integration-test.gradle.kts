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
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

// Inspiration: https://docs.gradle.org/current/samples/sample_jvm_multi_project_with_additional_test_types.html

plugins {
    java
}

val integrationTest by sourceSets.creating

configurations[integrationTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[integrationTest.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())

val integrationTestTask =
    tasks.register<Test>("integrationTest") {
        description = "Runs integration tests."
        group = "verification"
        inputs.property("SQ version", System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE"))
        inputs.property("keep SQ running", System.getProperty("keepSonarqubeRunning", "false"))
        useJUnitPlatform()

        testClassesDirs = integrationTest.output.classesDirs
        classpath = configurations[integrationTest.runtimeClasspathConfigurationName] + integrationTest.output

        if (System.getProperty("sonar.runtimeVersion") != null) {
            systemProperty("sonar.runtimeVersion", System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE"))
        }

        if (System.getProperty("keepSonarqubeRunning") != null) {
            systemProperty("keepSonarqubeRunning", System.getProperty("keepSonarqubeRunning"))
        }

        val enableParallelExecution = DefaultNativePlatform.getCurrentOperatingSystem().isWindows.not()
        systemProperty("junit.jupiter.execution.parallel.enabled", enableParallelExecution.toString())

        testLogging {
            // log the full stack trace (default is the 1st line of the stack trace)
            exceptionFormat = TestExceptionFormat.FULL
            events("started", "passed", "skipped", "failed")
        }

        outputs.upToDateWhen {
            // As the exact SQ version is not known at configuration time, we cannot know if the task is up-to-date
            false
        }
    }
