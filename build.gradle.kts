import org.sonarsource.text.registerRuleApiTasks

plugins {
    alias(libs.plugins.spotless)
    id("org.sonarsource.text.artifactory-configuration")
    id("org.sonarsource.text.rule-api")
    id("org.sonarsource.text.sonarqube")
    id("com.diffplug.blowdryer")
}

tasks.artifactoryPublish { skip = true }

artifactoryConfiguration {
    artifactsToPublish = "org.sonarsource.text:sonar-text-plugin:jar"
    artifactsToDownload = ""
    repoKeyEnv = "ARTIFACTORY_DEPLOY_REPO"
    usernameEnv = "ARTIFACTORY_DEPLOY_USERNAME"
    passwordEnv = "ARTIFACTORY_DEPLOY_PASSWORD"
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
