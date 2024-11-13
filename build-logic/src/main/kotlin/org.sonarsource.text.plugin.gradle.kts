import java.time.Duration
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    id("org.sonarsource.text.java-conventions")
    id("org.sonarsource.text.code-style-convention")
    jacoco
    id("com.gradleup.shadow")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        // log the full stack trace (default is the 1st line of the stack trace)
        exceptionFormat = TestExceptionFormat.FULL
        // verbose log for failed and skipped tests (by default the name of the tests are not logged)
        events("skipped", "failed")
    }
    providers.systemProperty("filename").map { systemProperty("filename", it) }.orNull
    timeout = Duration.ofMinutes(5)
}

jacoco {
    toolVersion = "0.8.12"
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

val cleanupTask = tasks.register<Delete>("cleanupOldVersion") {
    group = "build"
    description = "Clean up jars of old plugin version"

    delete(
        fileTree(project.layout.buildDirectory.dir("libs")).matching {
            include("${project.name}-*.jar")
            exclude("${project.name}-${project.version}-*.jar")
            exclude("${project.name}-${project.version}.jar")
        }
    )
}

tasks.shadowJar {
    dependsOn(cleanupTask)
}

artifacts {
    archives(tasks.shadowJar)
}
