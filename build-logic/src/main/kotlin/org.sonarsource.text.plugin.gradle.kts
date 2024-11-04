import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.sonarsource.text.CHECK_LIST_GENERATION_TASK_NAME
import org.sonarsource.text.CodeGenerationConfiguration
import org.sonarsource.text.GENERATED_SOURCES_DIR
import org.sonarsource.text.SPEC_LIST_GENERATION_TASK_NAME
import org.sonarsource.text.UpdatingSpecificationFilesGenerator

plugins {
    id("org.sonarsource.text.java-conventions")
    id("org.sonarsource.text.code-style-convention")
    id("org.sonarsource.text.check-list-generator")
    id("org.sonarsource.text.specification-files-list-generator")
    jacoco
    id("com.github.johnrengelman.shadow")
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

tasks.test {
    useJUnitPlatform()
    testLogging {
        // log the full stack trace (default is the 1st line of the stack trace)
        exceptionFormat = TestExceptionFormat.FULL
        // verbose log for failed and skipped tests (by default the name of the tests are not logged)
        events("skipped", "failed")
    }
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(false)
    }
}

// when subproject has Jacoco plugin applied we want to generate XML report for coverage
plugins.withType<JacocoPlugin> {
    tasks["test"].finalizedBy("jacocoTestReport")
}

val cleanupTask = tasks.register<Delete>("cleanupOldVersion") {
    group = "build"
    description = "Clean up jars of old plugin version"

    delete(
        fileTree(project.layout.buildDirectory.dir("libs")).matching {
            include("${project.name}-*.jar")
            exclude("${project.name}-${project.version}-*.jar")
            exclude("${project.name}-${project.version}.jar")
        }
    )
}

tasks.shadowJar {
    dependsOn(cleanupTask)
}

artifacts {
    archives(tasks.shadowJar)
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

    doLast {
        val config = project.extensions.getByName<CodeGenerationConfiguration>("codeGeneration")
        UpdatingSpecificationFilesGenerator("$projectDir", config.packagePrefix.get(), config.excludedKeys.get()).performGeneration()
    }
}
