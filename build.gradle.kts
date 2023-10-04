plugins {
  id("org.sonarsource.text.artifactory-configuration")
  id("org.sonarsource.text.rule-api")
  id("com.diffplug.blowdryer")
}

tasks.artifactoryPublish { skip = true }

tasks.register("fastBuild") {
  group = "Build"
  description = "Runs fast build without integration tests"

  dependsOn(tasks.build)
  gradle.taskGraph.whenReady {
    (gradle.taskGraph as org.gradle.execution.taskgraph.TaskExecutionGraphInternal).findTask("integrationTest")?.enabled = false
  }
}
