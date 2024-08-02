package org.sonarsource.text

import java.io.File
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

fun Project.registerRuleApiTasks(suffix: String, sonarpediaLocation: File) {
    registerRuleApiTask("ruleApiUpdate$suffix") {
        description = "Update $suffix rules description"

        workingDir = sonarpediaLocation
        args("com.sonarsource.ruleapi.Main", "update")
    }

    val rule = providers.gradleProperty("rule")
    val branch = providers.gradleProperty("branch")
    registerRuleApiTask("ruleApiGenerateRule$suffix") {
        description = "Update rule description for $suffix"

        workingDir = sonarpediaLocation
        args(
            buildList {
                add("com.sonarsource.ruleapi.Main")
                add("generate")
                add("-rule")
                add(rule.getOrElse(""))
                if (branch.isPresent) {
                    add("-branch")
                    add(branch.get())
                }
            }
        )
    }
}

fun Project.registerRuleApiTask(name: String, configure: JavaExec.() -> Unit): TaskProvider<JavaExec> =
    tasks.register<JavaExec>(name) {
        group = "Rule API"
        classpath = configurations.getByName("ruleApi")
        configure(this)
    }
