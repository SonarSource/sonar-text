import org.sonarsource.text.ArtifactoryConfiguration
import org.sonarsource.text.signingCondition

plugins {
    id("com.jfrog.artifactory")
    signing
    `maven-publish`
}

// this value is present on CI
val buildNumber: String? = System.getProperty("buildNumber")
if (project.version.toString().endsWith("-SNAPSHOT") && buildNumber != null) {
    val versionSuffix = if (project.version.toString().count { it == '.' } == 1) ".0.$buildNumber" else ".$buildNumber"
    project.version = project.version.toString().replace("-SNAPSHOT", versionSuffix)
    logger.lifecycle("Project version set to $version")
}

val artifactoryConfiguration = extensions.create<ArtifactoryConfiguration>("artifactoryConfiguration")

publishing {
    publications.create<MavenPublication>("mavenJava") {
        pom {
            name.set("SonarSource Text Analyzer")
            description.set(project.description)
            url.set("http://www.sonarqube.org/")
            organization {
                name.set("SonarSource")
                url.set("http://www.sonarsource.com/")
            }
            licenses {
                license {
                    artifactoryConfiguration.license?.invoke(this)
                }
            }
            scm {
                url.set("https://github.com/SonarSource/sonar-text-enterprise")
            }
            developers {
                developer {
                    id.set("sonarsource-team")
                    name.set("SonarSource Team")
                }
            }
        }
    }
}

signing {
    val signingKeyId: String? by project
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    setRequired {
        project.signingCondition()
    }
    sign(publishing.publications)
}

tasks.withType<Sign> {
    onlyIf {
        val artifactorySkip: Boolean = tasks.artifactoryPublish.get().skip
        !artifactorySkip && project.signingCondition()
    }
}

// `afterEvaluate` is required to inject configurable properties; see https://github.com/jfrog/artifactory-gradle-plugin/issues/71#issuecomment-1734977528
project.afterEvaluate {
    artifactory {
        if (artifactoryConfiguration.artifactsToPublish.isPresent) {
            clientConfig.info.addEnvironmentProperty(
                "ARTIFACTS_TO_PUBLISH",
                artifactoryConfiguration.artifactsToPublish.get()
            )
            clientConfig.info.addEnvironmentProperty(
                "ARTIFACTS_TO_DOWNLOAD",
                artifactoryConfiguration.artifactsToDownload.getOrElse("")
            )
        }

        setContextUrl(System.getenv("ARTIFACTORY_URL"))
        // Note: `publish` should only be called once: https://github.com/jfrog/artifactory-gradle-plugin/issues/111
        publish {
            if (artifactoryConfiguration.repoKeyEnv.isPresent) {
                repository {
                    setRepoKey(System.getenv(artifactoryConfiguration.repoKeyEnv.get()))
                    setUsername(System.getenv(artifactoryConfiguration.usernameEnv.get()))
                    setPassword(System.getenv(artifactoryConfiguration.passwordEnv.get()))
                }
            }
            defaults {
                publications("mavenJava")
                setProperties(
                    mapOf(
                        "build.name" to "sonar-text-enterprise",
                        "version" to project.version.toString(),
                        "build.number" to buildNumber,
                        "pr.branch.target" to System.getenv("PULL_REQUEST_BRANCH_TARGET"),
                        "pr.number" to System.getenv("PULL_REQUEST_NUMBER"),
                        "vcs.branch" to System.getenv("GIT_BRANCH"),
                        "vcs.revision" to System.getenv("GIT_COMMIT")
                    )
                )
                setPublishArtifacts(true)
                setPublishPom(true)
                setPublishIvy(false)
            }
        }

        clientConfig.info.addEnvironmentProperty("PROJECT_VERSION", project.version.toString())
        clientConfig.info.buildName = "sonar-text-enterprise"
        clientConfig.info.buildNumber = buildNumber
        clientConfig.isIncludeEnvVars = true
        clientConfig.envVarsExcludePatterns =
            "*password*,*PASSWORD*,*secret*,*MAVEN_CMD_LINE_ARGS*,sun.java.command," +
            "*token*,*TOKEN*,*LOGIN*,*login*,*key*,*KEY*,*PASSPHRASE*,*signing*"
    }
}
