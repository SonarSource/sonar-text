/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.plugins.common.TextAndSecretsSensor;

public class CiVendorFilesTelemetry {

  private static final String TELEMETRY_INFIX = "civendor_";

  public static final Map<String, String> CI_VENDOR_TO_REL_FILE_PATH = Map.of(
    "travisci", ".travis.yml",
    "jenkins", "Jenkinsfile",
    "circleci", ".circleci/config.yml",
    "gitlab", ".gitlab-ci.yml",
    "appveyor", "appveyor.yml",
    "azurepipelines", "azure-pipelines.yml",
    "bamboo", "bamboo.yml",
    "buildkite", ".buildkite/pipeline.yml",
    "bitbucketpipelines", "bitbucket-pipelines.yml",
    "semaphore", ".semaphore/semaphore.yml");

  private CiVendorFilesTelemetry() {
    // only static methods
  }

  public static void measureProjectsCIFilesInclusion(SensorContext sensorContext, TelemetryReporter telemetryReporter) {
    if (TextAndSecretsSensor.isSonarLintContext(sensorContext.runtime())) {
      // Telemetry is not raised in SQ-IDE context, we don't need to compute it
      return;
    }

    var fileSystem = sensorContext.fileSystem();

    for (Map.Entry<String, String> stringStringEntry : CI_VENDOR_TO_REL_FILE_PATH.entrySet()) {
      var vendor = stringStringEntry.getKey();
      var relativePath = stringStringEntry.getValue();
      // We only look for path's relative to the root
      var filePredicate = fileSystem.predicates().hasRelativePath(relativePath);
      var hasFilesForVendor = fileSystem.hasFiles(filePredicate);
      telemetryReporter.addNumericMeasure(TELEMETRY_INFIX + vendor, hasFilesForVendor ? 1 : 0);
    }
  }
}
