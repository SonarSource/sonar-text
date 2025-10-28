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
import kotlin.io.path.div
import org.sonarsource.text.CodeGenerationConfiguration
import org.sonarsource.text.Constants
import org.sonarsource.text.GENERATED_SOURCES_DIR
import org.sonarsource.text.SPEC_LIST_GENERATION_TASK_NAME
import org.sonarsource.text.listSecretSpecificationFiles
import org.sonarsource.text.loadLicenseHeader
import org.sonarsource.text.writeToFile

val codeGenerationConfiguration = extensions.findByType<CodeGenerationConfiguration>()
    ?: extensions.create<CodeGenerationConfiguration>("codeGeneration")

tasks.register(SPEC_LIST_GENERATION_TASK_NAME) {
    description = "Generates spec files list class based on all specification files"
    group = "build"

    val constants = with(codeGenerationConfiguration) {
        packagePrefix.zip(specFileListClassName) { packagePrefix, generatedClassName ->
            Constants(packagePrefix, generatedClassName)
        }
    }

    inputs.dir(constants.map { "$projectDir/src/main/resources/${it.specFilesLocation}/" })
    inputs.file(codeGenerationConfiguration.licenseHeaderFile)
    outputs.file(
        constants.flatMap {
            layout.buildDirectory.file("$GENERATED_SOURCES_DIR/${it.packagePrefix}/sonar/plugins/secrets/${it.generatedClassName}.java")
        }
    )

    doLast {
        val constants = constants.get()

        val files = listSecretSpecificationFiles(projectDir.toPath() / "src/main/resources/${constants.specFilesLocation}")

        val result = constants.template
            .replace("//<LICENSE_HEADER>", loadLicenseHeader(codeGenerationConfiguration.licenseHeaderFile.asFile.get()))
            .replace("//<REPLACE-WITH-LIST-OF-FILES>", discoverSpecFiles(files))

        writeToFile(result, "${constants.packagePrefix}/sonar/plugins/secrets/${constants.generatedClassName}.java")
    }
}

fun discoverSpecFiles(files: List<File>): String =
    files.joinToString(",\n", postfix = ");") { file ->
        "      \"${file.name.replace(".yaml", ".sml")}\""
    }
