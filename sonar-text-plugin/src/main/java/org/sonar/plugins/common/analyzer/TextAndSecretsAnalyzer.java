/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.common.analyzer;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.common.DurationStatistics;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.common.NotBinaryFilePredicate;
import org.sonar.plugins.common.TextAndSecretsSensor;
import org.sonar.plugins.common.thread.ParallelizationManager;
import org.sonar.plugins.secrets.BinaryFileCheck;

public final class TextAndSecretsAnalyzer extends AbstractAnalyzer {
  private static final Logger LOG = LoggerFactory.getLogger(TextAndSecretsAnalyzer.class);
  private static final String ANALYSIS_NAME = "text and secrets analysis";
  private final NotBinaryFilePredicate notBinaryFilePredicate;
  private final boolean analyzeAllFilesMode;
  private boolean displayHelpAboutExcludingBinaryFile = true;

  public TextAndSecretsAnalyzer(
    SensorContext sensorContext,
    ParallelizationManager parallelizationManager,
    DurationStatistics durationStatistics,
    List<Check> activeChecks,
    NotBinaryFilePredicate notBinaryFilePredicate,
    boolean analyzeAllFilesMode) {
    super(sensorContext, parallelizationManager, durationStatistics, activeChecks, ANALYSIS_NAME);
    this.notBinaryFilePredicate = notBinaryFilePredicate;
    this.analyzeAllFilesMode = analyzeAllFilesMode;
  }

  @Override
  protected boolean shouldRunCheck(Check check) {
    return !(check instanceof BinaryFileCheck);
  }

  @Override
  protected boolean shouldAnalyzeFile(InputFileContext inputFileContext) {
    if (analyzeAllFilesMode) {
      return shouldBeAnalyzedBlacklistMode(inputFileContext);
    } else {
      return shouldBeAnalyzedWhitelistMode(inputFileContext);
    }
  }

  /**
   * We suppose here that the list of provided files may contain some binary files that we couldn't exclude beforehand.
   * If that happen, we add its extension dynamically in the blacklist to avoid all files with the same extension.
   */
  private boolean shouldBeAnalyzedBlacklistMode(InputFileContext inputFileContext) {
    if (notBinaryFilePredicate.apply(inputFileContext.getInputFile())) {
      boolean hasNonTextCharacters = inputFileContext.hasNonTextCharacters();
      if (hasNonTextCharacters) {
        excludeBinaryFileExtension(inputFileContext.getInputFile());
      }
      return !hasNonTextCharacters;
    }
    return false;
  }

  /**
   * We suppose here that all provided files have been whitelisted, so we don't expected binary files.
   * In case it still happen, we don't add the extension to the blacklist as we consider it to be an exception.
   */
  private static boolean shouldBeAnalyzedWhitelistMode(InputFileContext inputFileContext) {
    boolean hasNonTextCharacters = inputFileContext.hasNonTextCharacters();
    if (hasNonTextCharacters) {
      LOG.warn("The file '{}' contains binary data and will not be analyzed.", inputFileContext.getInputFile());
      LOG.warn("Please check this file and/or remove the extension from the '{}' property.", TextAndSecretsSensor.TEXT_INCLUSIONS_KEY);
    }
    return !hasNonTextCharacters;
  }

  private void excludeBinaryFileExtension(InputFile inputFile) {
    String extension = NotBinaryFilePredicate.extension(inputFile.filename());
    if (extension != null) {
      notBinaryFilePredicate.addBinaryFileExtension(extension);
      LOG.warn("'{}' was added to the binary file filter because the file '{}' is a binary file.", extension, inputFile);
      if (displayHelpAboutExcludingBinaryFile) {
        displayHelpAboutExcludingBinaryFile = false;
        LOG.info("To remove the previous warning you can add the '.{}' extension to the '{}' property.", extension,
          TextAndSecretsSensor.EXCLUDED_FILE_SUFFIXES_KEY);
      }
    }
  }
}
