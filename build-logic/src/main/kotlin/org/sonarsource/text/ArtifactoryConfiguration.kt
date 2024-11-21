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
