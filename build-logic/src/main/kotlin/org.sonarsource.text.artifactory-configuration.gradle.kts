plugins {
    id("com.jfrog.artifactory")
    signing
    `maven-publish`
}

// this value is present on CI
val buildNumber: String? = System.getProperty("buildNumber")
project.ext["buildNumber"] = buildNumber
if (project.version.toString().endsWith("-SNAPSHOT") && buildNumber != null) {
    val versionSuffix = if (project.version.toString().count { it == '.' } == 1) ".0.$buildNumber" else ".$buildNumber"
    project.version = project.version.toString().replace("-SNAPSHOT", versionSuffix)
    logger.lifecycle("Project version set to $version")
}

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
                    name.set("GNU LPGL 3")
                    url.set("http://www.gnu.org/licenses/lgpl.txt")
                    distribution.set("repo")
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
        val branch = System.getenv()["CIRRUS_BRANCH"] ?: ""
        (branch == "master" || branch.matches("branch-[\\d.]+".toRegex())) &&
            gradle.taskGraph.hasTask(":artifactoryPublish")
    }
    sign(publishing.publications)
}

tasks.withType<Sign> {
    onlyIf {
        val branch = System.getenv()["CIRRUS_BRANCH"] ?: ""
        val artifactorySkip: Boolean = tasks.artifactoryPublish.get().skip
        !artifactorySkip && (branch == "master" || branch.matches("branch-[\\d.]+".toRegex())) &&
            gradle.taskGraph.hasTask(":artifactoryPublish")
    }
}

artifactory {
    val artifactsToPublish = "org.sonarsource.text:sonar-text-plugin:jar"

    clientConfig.info.addEnvironmentProperty("ARTIFACTS_TO_PUBLISH", artifactsToPublish)
    clientConfig.info.addEnvironmentProperty("ARTIFACTS_TO_DOWNLOAD", "")

    setContextUrl(System.getenv("ARTIFACTORY_URL"))
    publish {
        repository {
            setRepoKey(System.getenv("ARTIFACTORY_DEPLOY_REPO"))
            setUsername(System.getenv("ARTIFACTORY_DEPLOY_USERNAME"))
            setPassword(System.getenv("ARTIFACTORY_DEPLOY_PASSWORD"))
        }
        defaults {
            publications("mavenJava")
            setProperties(
                mapOf(
                    "build.name" to "sonar-text-enterprise",
                    "version" to project.version.toString(),
                    "build.number" to project.ext["buildNumber"].toString(),
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
    clientConfig.info.buildNumber = project.ext["buildNumber"].toString()
    clientConfig.isIncludeEnvVars = true
    clientConfig.envVarsExcludePatterns =
        "*password*,*PASSWORD*,*secret*,*MAVEN_CMD_LINE_ARGS*,sun.java.command," +
        "*token*,*TOKEN*,*LOGIN*,*login*,*key*,*KEY*,*PASSPHRASE*,*signing*"
}
