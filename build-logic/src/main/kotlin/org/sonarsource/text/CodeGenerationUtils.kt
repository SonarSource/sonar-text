/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
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
import java.nio.file.Path
import java.time.LocalDate
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.FileFilterUtils
import org.gradle.api.Project

const val CHECK_LIST_GENERATION_TASK_NAME = "generateSecretsCheckList"
const val SPEC_LIST_GENERATION_TASK_NAME = "generateSecretsSpecFilesList"
const val GENERATED_SOURCES_DIR = "generated/sources/secrets/java/main"
const val LINE_SEPARATOR = "\n"

fun Project.loadLicenseHeader(file: File) =
    file.readText(Charsets.UTF_8)
        .replace("\$YEAR", LocalDate.now().year.toString())
        .trimEnd()

fun Project.writeToFile(
    content: String,
    relativePath: String,
) {
    val directory = relativePath.substringBeforeLast('/')
    val filename = relativePath.substringAfterLast('/')
    val dir = layout.buildDirectory.dir("$GENERATED_SOURCES_DIR/$directory").get().asFile
    dir.mkdirs()
    file(dir.resolve(filename)).writeText(content + LINE_SEPARATOR)
}

fun listSecretSpecificationFiles(specFilesLocation: Path) =
    FileUtils.listFiles(
        specFilesLocation.toFile(),
        FileFilterUtils.suffixFileFilter(".yaml"),
        // don't recurse into subdirectories
        null
    ).sorted()

fun listCheckClasses(checkClassesLocation: Path) =
    FileUtils.listFiles(
        checkClassesLocation.toFile(),
        FileFilterUtils.suffixFileFilter("Check.java"),
        FileFilterUtils.trueFileFilter()
    ).sorted()
