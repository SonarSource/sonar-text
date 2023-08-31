plugins {
  id("org.sonarsource.text.java-conventions")
  id("org.sonarsource.text.artifactory-configuration")
  id("org.sonarsource.text.code-style-convention")
  id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
  api(libs.sonar.analyzer.commons)
  api(libs.jackson.dataformat.yaml)
  api(libs.com.networknt.jsonSchemaValidator) {
    exclude("org.slf4j", "slf4j-api")
    because("bundles slf4j 2.x")
  }
  compileOnly(libs.sonar.plugin.api)
  compileOnly(libs.slf4j.api)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.assertj.core)
  testImplementation(libs.sonar.plugin.api.test.fixtures)
  testImplementation(libs.sonar.plugin.api.impl)
  testImplementation(libs.mockito.core)
  testImplementation(libs.sonar.java.checks)
}

description = "SonarSource Text Analyzer :: Plugin"

tasks.test {
  useJUnitPlatform()
}

// used to be done by sonar-packaging maven plugin
tasks.jar {
  manifest {
    attributes(
      mapOf(
        "Plugin-ChildFirstClassLoader" to "false",
        "Plugin-Class" to "org.sonar.plugins.common.TextAndSecretsPlugin",
        "Plugin-Description" to "Analyzer for Text Files",
        "Plugin-Developers" to "SonarSource Team",
        "Plugin-Display-Version" to version,
        "Plugin-Homepage" to "https://sonarsource.atlassian.net/browse/SONARTEXT",
        "Plugin-IssueTrackerUrl" to "https://sonarsource.atlassian.net/browse/SONARTEXT",
        "Plugin-Key" to "text",
        "Plugin-License" to "GNU LGPL 3",
        "Plugin-Name" to "Text Code Quality and Security",
        "Plugin-Organization" to "SonarSource",
        "Plugin-OrganizationUrl" to "https://www.sonarsource.com",
        "Plugin-SourcesUrl" to "https://github.com/SonarSource/sonar-text",
        "Plugin-Version" to project.version,
        "Sonar-Version" to "9.8",
        "SonarLint-Supported" to "false",
        "Version" to project.version.toString(),
        "Jre-Min-Version" to java.sourceCompatibility.majorVersion
      )
    )
  }
}

tasks.shadowJar {
  minimize {}
  exclude("META-INF/LICENSE*")
  exclude("META-INF/NOTICE*")
  exclude("META-INF/*.RSA")
  exclude("META-INF/*.SF")
  exclude("LICENSE*")
  exclude("NOTICE*")
//  doLast {
//    enforceJarSizeAndCheckContent(shadowJar.get().archiveFile.get().asFile, 39_500_000L, 41_000_000L)
//  }
}

artifacts {
  archives(tasks.shadowJar)
}

publishing {
  publications.withType<MavenPublication> {
    artifact(tasks.shadowJar) {
      // remove `-all` suffix from the fat jar
      classifier = null
    }
    artifact(tasks.sourcesJar)
    artifact(tasks.javadocJar)
  }
}
