/*
 * SonarQube Text Plugin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.common.measures;

import java.util.Map;
import java.util.Set;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.plugins.common.TextAndSecretsSensor;

public class CiVendorFilesTelemetry {

  private static final String TELEMETRY_INFIX = "civendor_";

  public static final Map<String, Set<String>> CI_VENDOR_TO_REL_FILE_PATHS = Map.ofEntries(
    Map.entry("travisci", Set.of(".travis.yml")),
    Map.entry("jenkins", Set.of("Jenkinsfile")),
    Map.entry("circleci", Set.of(".circleci/config.yml")),
    Map.entry("gitlab", Set.of(".gitlab-ci.yml")),
    Map.entry("appveyor", Set.of("appveyor.yml")),
    Map.entry("azurepipelines", Set.of("azure-pipelines.yml")),
    Map.entry("bamboo", Set.of("bamboo.yml")),
    Map.entry("buildkite", Set.of(".buildkite/pipeline.yml")),
    Map.entry("bitbucketpipelines", Set.of("bitbucket-pipelines.yml")),
    Map.entry("semaphore", Set.of(".semaphore/semaphore.yml")),
    Map.entry("dockercompose", Set.of("docker-compose.yml", "docker-compose.yaml", "compose.yml", "compose.yaml")));

  private CiVendorFilesTelemetry() {
    // only static methods
  }

  public static void measureProjectsCIFilesInclusion(SensorContext sensorContext, TelemetryReporter telemetryReporter) {
    if (TextAndSecretsSensor.isSonarLintContext(sensorContext.runtime())) {
      // Telemetry is not raised in SQ-IDE context, we don't need to compute it
      return;
    }

    var fileSystem = sensorContext.fileSystem();

    for (Map.Entry<String, Set<String>> entry : CI_VENDOR_TO_REL_FILE_PATHS.entrySet()) {
      var vendor = entry.getKey();
      var relativePaths = entry.getValue();
      // We only look for path's relative to the root
      var hasFilesForVendor = relativePaths.stream()
        .anyMatch(path -> fileSystem.hasFiles(fileSystem.predicates().hasRelativePath(path)));
      telemetryReporter.addNumericMeasure(TELEMETRY_INFIX + vendor, hasFilesForVendor ? 1 : 0);
    }
  }
}
