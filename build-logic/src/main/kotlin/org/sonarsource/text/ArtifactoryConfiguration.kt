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

import org.gradle.api.provider.Property

interface ArtifactoryConfiguration {
    val artifactsToPublish: Property<String>
    val artifactsToDownload: Property<String>
    val repoKeyEnv: Property<String>
    val usernameEnv: Property<String>
    val passwordEnv: Property<String>

    // Following fields duplicate properties of MavenPomLicense
    val licenseName: Property<String>
    val licenseUrl: Property<String>
    val licenseDistribution: Property<String>
    val licenseComments: Property<String>
}
