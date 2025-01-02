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
package org.sonarsource.text

import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.internal.extensions.stdlib.capitalized

fun enforceJarSize(
    file: File,
    minSize: Long,
    maxSize: Long,
) {
    val size = file.length()
    if (size < minSize) {
        throw GradleException("${file.path} size ($size) too small. Min is $minSize")
    } else if (size > maxSize) {
        throw GradleException("${file.path} size ($size) too large. Max is $maxSize")
    }
}

fun Project.signingCondition(): Boolean {
    val branch = System.getenv()["CIRRUS_BRANCH"] ?: ""
    return (branch == "master" || branch.matches("branch-[\\d.]+".toRegex())) &&
        gradle.taskGraph.hasTask(":artifactoryPublish")
}

fun String.toCamelCase() = replace("-[a-z]".toRegex()) { it.value.last().uppercase() }.capitalized()

fun String.toSnakeCase() = replace("[a-z][A-Z]".toRegex()) { it.value.first() + "_" + it.value.last().lowercase() }
