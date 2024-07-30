import org.sonarsource.text.registerRuleApiTasks

plugins {
    id("com.diffplug.spotless") version libs.versions.spotless.gradle.get()
    id("org.sonarsource.text.artifactory-configuration")
    id("org.sonarsource.text.rule-api")
    id("org.sonarsource.text.sonarqube")
    id("com.diffplug.blowdryer")
}

spotless {
    encoding(Charsets.UTF_8)
    kotlinGradle {
        ktlint().setEditorConfigPath("$rootDir/.editorconfig")
        target("*.gradle.kts", "/build-logic/src/**/*.gradle.kts")
    }
}

registerRuleApiTasks("Secrets", file("$projectDir/sonarpedia-secrets"))
registerRuleApiTasks("Text", file("$projectDir/sonarpedia-text"))

tasks.register("ruleApiUpdate") {
    description = "Update ALL rules description"
    group = "Rule API"
    dependsOn("ruleApiUpdateSecrets", "ruleApiUpdateText")
}
