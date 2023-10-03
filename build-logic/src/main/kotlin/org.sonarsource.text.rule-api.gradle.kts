val rulApiVersion = "2.7.0.2612"

repositories {
  maven {
    url = project.uri("https://repox.jfrog.io/repox/sonarsource-private-releases")
    authentication {
      credentials {
        val artifactoryUsername: String? by project
        val artifactoryPassword: String? by project
        username = artifactoryUsername
        password = artifactoryPassword
      }
    }
  }
}

val ruleApi = configurations.create("ruleApi")

dependencies {
  ruleApi("com.sonarsource.rule-api:rule-api:${rulApiVersion}")
}

val ruleApiUpdateSecrets = tasks.register<Exec>("ruleApiUpdateSecrets") {
  description = "Update secret rules description"
  group = "Rule API"
  workingDir = file("${projectDir}/sonarpedia-secrets")

  commandLine("java",
    "-classpath",
    ruleApi.resolve().first(),
    "com.sonarsource.ruleapi.Main",
    "update")
}

val ruleApiUpdateText = tasks.register<Exec>("ruleApiUpdateText") {
  description = "Update text rules description"
  group = "Rule API"
  workingDir = file("${projectDir}/sonarpedia-text")

  commandLine("java",
    "-classpath",
    ruleApi.resolve().first(),
    "com.sonarsource.ruleapi.Main",
    "update")
}

tasks.register("ruleApiUpdate") {
  description = "Update ALL rules description"
  group = "Rule API"
  dependsOn(ruleApiUpdateSecrets, ruleApiUpdateText)
}

val rule: String by project

tasks.register<Exec>("ruleApiUpdateRuleSecrets") {
  description = "Update rule description for secret"
  group = "Rule API"
  workingDir = file("${projectDir}/sonarpedia-secrets")

  commandLine("java",
    "-classpath",
    ruleApi.resolve().first(),
    "com.sonarsource.ruleapi.Main",
    "generate",
    "-rule",
    rule
  )
}

tasks.register<Exec>("ruleApiUpdateRuleText") {
  description = "Update rule description for text"
  group = "Rule API"
  workingDir = file("${projectDir}/sonarpedia-text")

  commandLine("java",
    "-classpath",
    ruleApi.resolve().first(),
    "com.sonarsource.ruleapi.Main",
    "generate",
    "-rule",
    rule
  )
}
