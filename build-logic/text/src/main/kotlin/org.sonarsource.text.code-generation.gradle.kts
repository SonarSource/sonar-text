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
import org.sonarsource.text.CHECK_LIST_GENERATION_TASK_NAME
import org.sonarsource.text.CodeGenerationConfiguration
import org.sonarsource.text.GENERATED_SOURCES_DIR
import org.sonarsource.text.SPEC_LIST_GENERATION_TASK_NAME
import org.sonarsource.text.UpdatingSpecificationFilesGenerator

plugins {
    java
    id("org.sonarsource.text.check-list-generator")
    id("org.sonarsource.text.specification-files-list-generator")
}

val generateJavaCode by tasks.registering {
    inputs.files(tasks[CHECK_LIST_GENERATION_TASK_NAME], tasks[SPEC_LIST_GENERATION_TASK_NAME])
    outputs.dir("build/$GENERATED_SOURCES_DIR")
}

sourceSets {
    main {
        java {
            srcDirs(generateJavaCode)
        }
    }
}

tasks.register("updateCheckClasses") {
    group = "build"
    description =
        "Synchronize specification files with check classes and corresponding tests, generating code for new " +
        "specifications and removing code for removed specifications"
    outputs.file(layout.buildDirectory.file("generated/rspecKeysToUpdate.txt"))
    outputs.upToDateWhen {
        // To be on a safe side, always rerun the generator
        false
    }
    dependsOn("processResources")

    doLast {
        val config = project.extensions.getByName<CodeGenerationConfiguration>("codeGeneration")
        UpdatingSpecificationFilesGenerator(
            "$projectDir",
            config.packagePrefix.get(),
            config.excludedKeys.get(),
            config.baseTestClass.get()
        ).performGeneration()
    }
}
