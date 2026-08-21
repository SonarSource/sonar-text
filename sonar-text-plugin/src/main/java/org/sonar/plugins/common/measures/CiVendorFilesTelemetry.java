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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.plugins.common.predicates.TextAndSecretsPredicates;

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
    Map.entry("dockercompose", Set.of("docker-compose.yml", "docker-compose.yaml", "compose.yml", "compose.yaml")),
    Map.entry("dockerfile", Set.of("Dockerfile")),
    Map.entry("containerfile", Set.of("Containerfile")));

  private CiVendorFilesTelemetry() {
    // only static methods
  }

  public static void measureProjectsCIFilesInclusion(SensorContext sensorContext, TelemetryReporter telemetryReporter) {
    if (!TextAndSecretsPredicates.isHiddenFilesAnalysisSupported(sensorContext.runtime())) {
      // Guarantees that telemetry is not calculated in SQ-IDE context since telemetry won't be saved there.
      // Since most ci vendor files are dotfiles/dot-directories, we only calculate this when hidden file analysis is supported
      return;
    }

    var fileSystem = sensorContext.fileSystem();
    var predicates = fileSystem.predicates();

    var detectedVendors = new HashSet<String>();
    var recordingPredicates = CI_VENDOR_TO_REL_FILE_PATHS.entrySet().stream()
      .flatMap(entry -> entry.getValue().stream()
        // We only look for path's relative to the root
        .<FilePredicate>map(path -> new RecordingPredicate(
          predicates.hasRelativePath(path),
          file -> detectedVendors.add(entry.getKey()))))
      .toList();
    var combinedPredicate = predicates.or(recordingPredicates);

    // predicates.or(...) short-circuits on the first predicate match.
    // Iterating triggers RecordingPredicate's recording side effects
    fileSystem.inputFiles(combinedPredicate).forEach(inputFile -> {
      // no-op: recording already happened inside RecordingPredicate.apply()
    });

    for (String vendor : CI_VENDOR_TO_REL_FILE_PATHS.keySet()) {
      telemetryReporter.addNumericMeasure(TELEMETRY_INFIX + vendor, detectedVendors.contains(vendor) ? 1 : 0);
    }
  }
}
