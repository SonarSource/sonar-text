/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
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
package org.sonarsource.text

import java.io.File
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
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

fun Project.registerRuleApiTasks(
    suffix: String,
    sonarpediaLocation: File,
) {
    registerRuleApiTask("ruleApiUpdate$suffix") {
        description = "Update $suffix rules description"

        workingDir = sonarpediaLocation
        args("update")
    }

    val rule = providers.gradleProperty("rule")
    val branch = providers.gradleProperty("branch")
    registerRuleApiGenerateTask(suffix, sonarpediaLocation, rule, branch)

    if (suffix == "Secrets" || suffix == "DeveloperSecrets") {
        val pluginSubproject = when (suffix) {
            "Secrets" -> project(":sonar-text-plugin")
            "DeveloperSecrets" -> project(":private:sonar-text-developer-plugin")
            else -> error("Unsupported suffix: $suffix")
        }
        registerRuleApiGenerateFromFileTask(
            "Generation",
            sonarpediaLocation,
            pluginSubproject.layout.buildDirectory.file("generated/rspecKeysToUpdate.txt"),
            branch
        ).configure {
            doLast {
                delete(pluginSubproject.layout.buildDirectory.file("generated/rspecKeysToUpdate.txt"))
            }
        }
    }
}

fun Project.registerRuleApiGenerateFromFileTask(
    suffix: String,
    sonarpediaLocation: File,
    rulesFile: Provider<RegularFile>,
    branch: Provider<String>,
): TaskProvider<JavaExec> {
    val ruleKeysProvider = rulesFile.map {
        it.asFile.readLines().joinToString(" ")
    }
    return registerRuleApiGenerateTask(suffix, sonarpediaLocation, ruleKeysProvider, branch)
}

fun Project.registerRuleApiGenerateTask(
    suffix: String,
    sonarpediaLocation: File,
    rule: Provider<String>,
    branch: Provider<String>,
) = registerRuleApiTask(
    "ruleApiGenerateRule$suffix"
) {
    description = "Update rule description for $suffix"
    onlyIf { rule.isPresent && rule.get().isNotBlank() }
    outputs.upToDateWhen {
        // To be on a safe side, don't try to cache results of rule-api.
        false
    }

    workingDir = sonarpediaLocation
    argumentProviders.add {
        buildList {
            add("generate")
            add("-rule")
            rule.getOrElse("").split(" ").forEach { add(it) }
            if (branch.isPresent) {
                add("-branch")
                add(branch.get())
            }
        }
    }
}

private fun Project.registerRuleApiTask(
    name: String,
    configure: JavaExec.() -> Unit,
): TaskProvider<JavaExec> =
    tasks.register<JavaExec>(name) {
        group = "Rule API"
        usesService(
            gradle.sharedServices.registerIfAbsent("ruleApiRepoProvider", RuleApiService::class) {
                // because rule-api requires exclusive access to `$HOME/.sonar/rule-api/rspec`, we force tasks to never run in parallel
                maxParallelUsages = 1
            }
        )
        classpath = configurations.getByName("ruleApi")
        mainClass = "com.sonarsource.ruleapi.Main"
        configure(this)
    }
