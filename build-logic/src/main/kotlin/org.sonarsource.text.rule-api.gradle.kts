val rulApiVersion = "2.7.0.2612"

repositories {
    maven {
        url = project.uri("https://repox.jfrog.io/repox/sonarsource-private-releases")
        authentication {
            credentials {
                val artifactoryUsername: String? by project
                val artifactoryPassword: String? by project
                username = System.getenv("ARTIFACTORY_PRIVATE_USERNAME") ?: artifactoryUsername
                password = System.getenv("ARTIFACTORY_PRIVATE_PASSWORD") ?: artifactoryPassword
            }
        }
    }
    mavenCentral()
}

val ruleApi = configurations.create("ruleApi")

dependencies {
    ruleApi("com.sonarsource.rule-api:rule-api:$rulApiVersion")
}

val ruleApiUpdateSecrets =
    tasks.register<JavaExec>("ruleApiUpdateSecrets") {
        description = "Update secret rules description"
        group = "Rule API"
        workingDir = file("$projectDir/sonarpedia-secrets")
        classpath = ruleApi

        args(
            "com.sonarsource.ruleapi.Main",
            "update"
        )
    }

val ruleApiUpdateText =
    tasks.register<JavaExec>("ruleApiUpdateText") {
        description = "Update text rules description"
        group = "Rule API"
        workingDir = file("$projectDir/sonarpedia-text")
        classpath = ruleApi

        args(
            "com.sonarsource.ruleapi.Main",
            "update"
        )
    }

tasks.register("ruleApiUpdate") {
    description = "Update ALL rules description"
    group = "Rule API"
    dependsOn(ruleApiUpdateSecrets, ruleApiUpdateText)
}

val rule: String? by project
val branch: String? by project

tasks.register<JavaExec>("ruleApiGenerateRuleSecrets") {
    description = "Update rule description for secret"
    group = "Rule API"
    workingDir = file("$projectDir/sonarpedia-secrets")
    classpath = ruleApi

    args(
        buildList {
            add("com.sonarsource.ruleapi.Main")
            add("generate")
            add("-rule")
            add(rule.orEmpty())
            if (!branch.isNullOrEmpty()) {
                add("-branch")
                add(branch)
            }
        }
    )
}

tasks.register<JavaExec>("ruleApiGenerateRuleText") {
    description = "Update rule description for text"
    group = "Rule API"
    workingDir = file("$projectDir/sonarpedia-text")
    classpath = ruleApi

    args(
        "com.sonarsource.ruleapi.Main",
        "generate",
        "-rule",
        rule.orEmpty()
    )
}
