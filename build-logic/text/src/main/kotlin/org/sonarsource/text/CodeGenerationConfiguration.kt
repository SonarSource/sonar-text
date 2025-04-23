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

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty

open class CodeGenerationConfiguration(objects: ObjectFactory) {
    val packagePrefix: Property<String> = objects.property<String>()
    val baseTestClass: Property<String> = objects.property<String>()
    val excludedKeys: SetProperty<String> = objects.setProperty<String>()
    val checkListClassName: Property<String> = objects.property<String>()
    val checkListClassesToEmbed: SetProperty<String> = objects.setProperty<String>()
    val specFileListClassName: Property<String> = objects.property<String>()
    val licenseHeaderFile: RegularFileProperty = objects.fileProperty()
}
