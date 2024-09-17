package org.sonarsource.text

import java.io.File
import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.registerIfAbsent

/**
 * An empty build service to serve as a synchronization point for rule-api tasks.
 * Because rule-api requires exclusive access to `$HOME/.sonar/rule-api/rspec`, we force tasks to never run in parallel
 * by configuring this service.
 */
abstract class RuleApiService : BuildService<BuildServiceParameters.None>

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
        usesService(gradle.sharedServices.registerIfAbsent("ruleApiRepoProvider", RuleApiService::class) {
            // because rule-api requires exclusive access to `$HOME/.sonar/rule-api/rspec`, we force tasks to never run in parallel
            maxParallelUsages = 1
        })
        classpath = configurations.getByName("ruleApi")
        configure(this)
    }
