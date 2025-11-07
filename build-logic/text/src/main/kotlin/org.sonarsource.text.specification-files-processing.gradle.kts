/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
import org.sonarsource.text.CodeGenerationConfiguration
import org.sonarsource.text.convertYamlToSmile
import org.sonarsource.text.expandCommonBlock

val codeGenerationConfiguration = extensions.findByType<CodeGenerationConfiguration>()
    ?: extensions.create<CodeGenerationConfiguration>("codeGeneration")

tasks.named<ProcessResources>("processResources") {
    val packagePrefix = codeGenerationConfiguration.packagePrefix.get()
    val configurationPath = "$packagePrefix/sonar/plugins/secrets"
    val commonSections = file("src/main/resources/$configurationPath/common").listFiles()?.associate { file ->
        file.name to file.readText()
    } ?: emptyMap()

    filteringCharset = Charsets.UTF_8.name()
    filesMatching("$configurationPath/configuration/*.yaml") {
        filter { line -> line.expandCommonBlock(commonSections) }
    }
    filesMatching("$configurationPath/common/**") {
        exclude()
    }
    includeEmptyDirs = false

    doLast {
        val outputDir = destinationDir
        val yamlFiles = fileTree(outputDir).matching {
            include("$configurationPath/configuration/*.yaml")
        }

        yamlFiles.forEach { yamlFile ->
            val smileFile = File(yamlFile.parentFile, yamlFile.nameWithoutExtension + ".sml")
            convertYamlToSmile(yamlFile, smileFile)
            yamlFile.delete()
        }
    }
}

tasks.named<ProcessResources>("processTestResources") {
    doLast {
        val outputDir = destinationDir
        val yamlFiles = fileTree(outputDir).matching {
            include("secretsConfiguration/**/*.yaml")
            include("regex/**/*.yaml")
        }

        yamlFiles.forEach { yamlFile ->
            val smileFile = File(yamlFile.parentFile, yamlFile.nameWithoutExtension + ".sml")
            convertYamlToSmile(yamlFile, smileFile)
            // YAML files are not deleted since some tests rely on them (e.g. schema validation)
        }
    }
}
