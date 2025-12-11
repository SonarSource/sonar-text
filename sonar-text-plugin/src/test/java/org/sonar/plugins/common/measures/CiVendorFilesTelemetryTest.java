/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource Sàrl
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

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.internal.SensorContextTester;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.sonar.plugins.common.TestUtils.inputFile;
import static org.sonar.plugins.common.measures.CiVendorFilesTelemetry.CI_VENDOR_TO_REL_FILE_PATH;

class CiVendorFilesTelemetryTest {

  static Stream<Arguments> shouldReportTelemetryForTheDefaultCases() {
    return CI_VENDOR_TO_REL_FILE_PATH.entrySet().stream().map(entry -> arguments(entry.getValue(), entry.getKey()));
  }

  @MethodSource
  @ParameterizedTest
  void shouldReportTelemetryForTheDefaultCases(String filePath, String vendorToRaiseTelemetryOn) {
    SensorContextTester sensorContext = SensorContextTester.create(Path.of("."));
    InputFile inputFile = inputFile(Path.of(filePath), "");
    sensorContext.fileSystem().add(inputFile);

    verifyCIVendorTelemetryRaised(sensorContext, Set.of(vendorToRaiseTelemetryOn));
  }

  @MethodSource("shouldReportTelemetryForTheDefaultCases")
  @ParameterizedTest
  void shouldNotReportTelemetryWhenFileNotInRoot(String filePath, String vendorsToRaiseTelemetryOn) {
    SensorContextTester sensorContext = SensorContextTester.create(Path.of("."));
    InputFile inputFile = inputFile(Path.of("foo", filePath), "");
    sensorContext.fileSystem().add(inputFile);

    verifyCIVendorTelemetryRaised(sensorContext, Collections.emptySet());
  }

  void verifyCIVendorTelemetryRaised(SensorContext sensorContext, Set<String> vendorsToRaiseTelemetryOn) {
    var sensorTelemetry = spy(new TelemetryReporter(sensorContext));

    CiVendorFilesTelemetry.measureProjectsCIFilesInclusion(sensorContext, sensorTelemetry);

    for (String vendor : CI_VENDOR_TO_REL_FILE_PATH.keySet()) {
      if (vendorsToRaiseTelemetryOn.contains(vendor)) {
        verify(sensorTelemetry).addNumericMeasure("civendor_" + vendor, 1);
      } else {
        verify(sensorTelemetry).addNumericMeasure("civendor_" + vendor, 0);
      }
    }
  }

}
