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

import org.gradle.api.GradleException
import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

fun String.toCamelCase() = replace("-[a-z]".toRegex()) { it.value.last().uppercase() }.capitalized()

fun String.toSnakeCase() = replace("[a-z][A-Z]".toRegex()) { it.value.first() + "_" + it.value.last().lowercase() }

fun getNativeClassifier(platform: DefaultNativePlatform): String {
    val os = when {
        platform.operatingSystem.isLinux -> "linux"
        platform.operatingSystem.isMacOsX -> "macos"
        platform.operatingSystem.isWindows -> "windows"
        else -> throw GradleException("Unsupported OS: ${platform.operatingSystem.displayName}")
    }
    val arch = when {
        platform.architecture.isArm64 -> "arm64"
        platform.architecture.isAmd64 -> "x86-64"
        else -> throw GradleException("Unsupported architecture: ${platform.architecture}")
    }
    return "$os-$arch"
}
