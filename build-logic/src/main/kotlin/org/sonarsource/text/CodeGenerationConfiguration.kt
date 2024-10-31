package org.sonarsource.text

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

interface CodeGenerationConfiguration {
    val packagePrefix: Property<String>
    val checkListClassName: Property<String>
    val specFileListClassName: Property<String>
    val licenseHeaderFile: RegularFileProperty
}
