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

fun String.expandCommonBlock(commonSections: Map<String, String>): String {
    val line = this
    return if (line.contains("\${common/")) {
        val indent = line.takeWhile(Char::isWhitespace).count()
        val commonSectionName = line.substringAfter("\${common/").substringBefore("}")
        val commonSection = commonSections[commonSectionName] ?: error(
            "Missing file [$commonSectionName] in configuration/common"
        )
        commonSection.lines().filterNot { it.isBlank() }.joinToString("\n") { " ".repeat(indent) + it }
    } else {
        line
    }
}
