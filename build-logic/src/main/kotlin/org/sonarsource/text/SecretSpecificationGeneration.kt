package org.sonarsource.text

import org.gradle.api.Project

const val updateCheckClassesTaskName = "updateCheckClasses"

fun Project.registerUpdateCheckClassesTask(packagePrefix: String, excludedKeys: Collection<String>) = tasks.register(updateCheckClassesTaskName) {
    group = "build"
    description =
        "Synchronize specification files with check classes and corresponding tests, generating code for new " +
        "specifications and removing code for removed specifications"
    outputs.file(layout.buildDirectory.file("generated/rspecKeysToUpdate.txt"))
    outputs.upToDateWhen {
        // To be on a safe side, always rerun the generator
        false
    }

    doLast {
        UpdatingSpecificationFilesGenerator("$projectDir", packagePrefix, excludedKeys).performGeneration()
    }
}
