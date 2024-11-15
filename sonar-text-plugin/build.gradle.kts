import org.sonarsource.text.enforceJarSize

plugins {
    id("org.sonarsource.text.plugin")
    id("org.sonarsource.text.code-generation")
    id("org.sonarsource.text.artifactory-configuration")
    `java-library`
    `java-test-fixtures`
}

description = "SonarSource Text Analyzer :: Plugin"

dependencies {
    api(libs.sonar.analyzer.commons)
    api(libs.jackson.dataformat.yaml)
    api(libs.com.networknt.jsonSchemaValidator)
    api(libs.eclipse.jgit)
    compileOnly(libs.sonar.plugin.api)
    compileOnly(libs.slf4j.api)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.sonar.plugin.api.test.fixtures)
    testImplementation(libs.sonar.plugin.api.impl)
    testImplementation(libs.mockito.core)
    testImplementation(libs.logback.classic)

    testFixturesImplementation(libs.junit.jupiter)
    testFixturesImplementation(libs.assertj.core)
    testFixturesImplementation(libs.sonar.plugin.api.test.fixtures)
    testFixturesImplementation(libs.sonar.plugin.api.impl)
    testFixturesImplementation(libs.mockito.core)
    testFixturesImplementation(libs.sonar.java.checks)
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
                "SonarLint-Supported" to "true",
                "Version" to project.version.toString(),
                "Jre-Min-Version" to java.sourceCompatibility.majorVersion
            )
        )
    }
}

tasks.shadowJar {
    minimize()
    exclude("META-INF/LICENSE*")
    exclude("META-INF/NOTICE*")
    exclude("META-INF/*.RSA")
    exclude("META-INF/*.SF")
    exclude("LICENSE*")
    exclude("NOTICE*")

    doLast {
        enforceJarSize(tasks.shadowJar.get().archiveFile.get().asFile, 6_500_000L, 7_500_000L)
    }
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

artifactoryConfiguration {
    license {
        name.set("GNU LPGL 3")
        url.set("http://www.gnu.org/licenses/lgpl.txt")
        distribution.set("repo")
    }
    repoKeyEnv = "ARTIFACTORY_DEPLOY_REPO"
    usernameEnv = "ARTIFACTORY_DEPLOY_USERNAME"
    passwordEnv = "ARTIFACTORY_DEPLOY_PASSWORD"
}

codeStyleConvention {
    licenseHeaderFile.set(rootProject.file("LICENSE_HEADER"))
}

codeGeneration {
    packagePrefix = "org"
    baseTestClass = "org.sonar.plugins.secrets.utils.AbstractRuleExampleTest"
    excludedKeys = emptySet()
    checkListClassName = "SecretsCheckList"
    checkListClassesToEmbed = emptySet()
    specFileListClassName = "SecretsSpecificationFilesDefinition"
    licenseHeaderFile = rootProject.file("LICENSE_HEADER")
}
