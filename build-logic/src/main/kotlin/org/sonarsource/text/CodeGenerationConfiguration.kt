package org.sonarsource.text

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty

open class CodeGenerationConfiguration(objects: ObjectFactory) {
    val packagePrefix: Property<String> = objects.property<String>()
    val excludedKeys: SetProperty<String> = objects.setProperty<String>()
    val checkListClassName: Property<String> = objects.property<String>()
    val specFileListClassName: Property<String> = objects.property<String>()
    val licenseHeaderFile: RegularFileProperty = objects.fileProperty()
}
