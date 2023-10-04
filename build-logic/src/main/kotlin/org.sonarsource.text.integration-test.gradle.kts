// Inspiration: https://docs.gradle.org/current/samples/sample_jvm_multi_project_with_additional_test_types.html

plugins {
  java
}

val integrationTest by sourceSets.creating

configurations[integrationTest.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[integrationTest.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())

val integrationTestTask = tasks.register<Test>("integrationTest") {
  description = "Runs integration tests."
  group = "verification"
  useJUnit()

  testClassesDirs = integrationTest.output.classesDirs
  classpath = configurations[integrationTest.runtimeClasspathConfigurationName] + integrationTest.output
}

tasks.check {
  dependsOn(integrationTestTask)
}

dependencies {
  "integrationTestImplementation"(project)
}
