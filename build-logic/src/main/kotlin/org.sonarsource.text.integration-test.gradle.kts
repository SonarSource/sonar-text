import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

// Inspiration: https://docs.gradle.org/current/samples/sample_jvm_multi_project_with_additional_test_types.html

plugins {
    java
}

val integrationTest by sourceSets.creating

configurations[integrationTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[integrationTest.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())

val integrationTestTask =
    tasks.register<Test>("integrationTest") {
        description = "Runs integration tests."
        group = "verification"
        inputs.property("SQ version", System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE"))
        inputs.property("keep SQ running", System.getProperty("keepSonarqubeRunning", "false"))
        useJUnitPlatform()

        testClassesDirs = integrationTest.output.classesDirs
        classpath = configurations[integrationTest.runtimeClasspathConfigurationName] + integrationTest.output

        if (System.getProperty("sonar.runtimeVersion") != null) {
            systemProperty("sonar.runtimeVersion", System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE"))
        }

        if (System.getProperty("keepSonarqubeRunning") != null) {
            systemProperty("keepSonarqubeRunning", System.getProperty("keepSonarqubeRunning"))
        }

        val enableParallelExecution = DefaultNativePlatform.getCurrentOperatingSystem().isWindows.not()
        systemProperty("junit.jupiter.execution.parallel.enabled", enableParallelExecution.toString())

        testLogging {
            // log the full stack trace (default is the 1st line of the stack trace)
            exceptionFormat = TestExceptionFormat.FULL
            events("started", "passed", "skipped", "failed")
        }
    }
