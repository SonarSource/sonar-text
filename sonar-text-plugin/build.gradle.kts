plugins {
  id("org.sonarsource.text.java-conventions")
  id("org.sonarsource.text.artifactory-configuration")
  id("org.sonarsource.text.code-style-convention")
  jacoco
  id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
  api(libs.sonar.analyzer.commons)
  api(libs.jackson.dataformat.yaml)
  api(libs.com.networknt.jsonSchemaValidator) {
    exclude("org.slf4j", "slf4j-api")
    because("bundles slf4j 2.x")
  }
  api(libs.eclipse.jgit)
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
  // pass the filename property to SecretsRegexTest
  systemProperty("filename", System.getProperty("filename"))
  testLogging {
    exceptionFormat =
      org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL // log the full stack trace (default is the 1st line of the stack trace)
    events("skipped", "failed") // verbose log for failed and skipped tests (by default the name of the tests are not logged)
  }
}

jacoco {
  toolVersion = "0.8.10"
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

val jGitExclusions = listOf(
  "about.html",
  "org/eclipse/jgit/blame/**",
  "org/eclipse/jgit/hooks/**",
  "org/eclipse/jgit/merge/**",
  "org/eclipse/jgit/patch/**",
  "org/eclipse/jgit/transport/AmazonS3.class",
  "org/eclipse/jgit/transport/BasePackFetchConnection.class",
  "org/eclipse/jgit/transport/FetchProcess.class",
  "org/eclipse/jgit/transport/ReceivePack.class",
  "org/eclipse/jgit/transport/TransportHttp.class",
  "org/eclipse/jgit/transport/UploadPack.class",
  "org/eclipse/jgit/transport/WalkFetchConnection.class",
  "org/eclipse/jgit/transport/http/**",
  "org/eclipse/jgit/internal/transport/**",
  "org/eclipse/jgit/internal/diffmergetool/**",
  "org/eclipse/jgit/internal/storage/dfs/**")

tasks.shadowJar {
  minimize()
  exclude("META-INF/LICENSE*")
  exclude("META-INF/NOTICE*")
  exclude("META-INF/*.RSA")
  exclude("META-INF/*.SF")
  exclude("LICENSE*")
  exclude("NOTICE*")

  // jGit Exclusions
  exclude(jGitExclusions)
  exclude {
    (it.name.endsWith("Command.class") && it.name != "StatusCommand.class" && it.name != "GitCommand.class") ||
      it.name.endsWith("Result.class")
  }

  doLast {
    enforceJarSize(tasks.shadowJar.get().archiveFile.get().asFile, 5_500_000L, 6_000_000L)
  }
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

fun enforceJarSize(file: File, minSize: Long, maxSize: Long) {
  val size = file.length()
  if (size < minSize) {
    throw GradleException("${file.path} size ($size) too small. Min is $minSize")
  } else if (size > maxSize) {
    throw GradleException("${file.path} size ($size) too large. Max is $maxSize")
  }
}
