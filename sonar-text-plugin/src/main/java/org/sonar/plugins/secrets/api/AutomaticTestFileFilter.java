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
package org.sonar.plugins.secrets.api;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.common.InputFileContext;

/**
 * Class for producing a scope based file filter.
 */
public final class AutomaticTestFileFilter {
  private static final Logger LOG = LoggerFactory.getLogger(AutomaticTestFileFilter.class);

  private static final Set<String> FILENAME_PREFIXES = Set.of("test");
  private static final Set<String> FILENAME_CONTAINS = Set.of("test.", "tests.");
  private static final Set<String> FILENAME_SUFFIXES = Set.of(
    ".spec.js", ".spec.jsx", ".spec.ts", ".spec.tsx",
    "_spec.rb", "_test.rb");

  private AutomaticTestFileFilter() {
  }

  /**
   * Creates a predicate based on the rule scopes and status of automatic test file detection.
   */
  public static Predicate<InputFileContext> isNotAutomaticallyDetectedTestFile() {
    return ctx -> !isFilenameTest(ctx) && !isFileInDocOrTestDirectory(ctx);
  }

  private static boolean isFilenameTest(InputFileContext inputFileContext) {
    var filename = inputFileContext.getInputFile().filename().toLowerCase(Locale.ROOT);
    return FILENAME_PREFIXES.stream().anyMatch(filename::startsWith) ||
      FILENAME_CONTAINS.stream().anyMatch(filename::contains) ||
      FILENAME_SUFFIXES.stream().anyMatch(filename::endsWith);
  }

  private static boolean isFileInDocOrTestDirectory(InputFileContext inputFileContext) {
    // the path of the baseDir is not analyzed, only the relative path from file to baseDir
    var path = Path.of(inputFileContext.getInputFile().uri());
    String relativeUnixPath;
    try {
      var baseDir = inputFileContext.getFileSystem().baseDir().toPath();
      var relativePath = baseDir.relativize(path).normalize();
      relativeUnixPath = normalizeToUnixPathSeparator(relativePath.toString().toLowerCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      LOG.debug("Couldn't calculate the projects relative path of {}", inputFileContext.getInputFile());
      // Default to not detect it as test file
      return false;
    }
    var pathElements = Arrays.asList(relativeUnixPath.split("/"));
    var pathElementsWithoutFilename = pathElements.subList(0, pathElements.size() - 1);
    return isDocDirectory(pathElementsWithoutFilename) || isTestDirectory(pathElementsWithoutFilename)
      || hasEnding(pathElementsWithoutFilename, "test") || hasEnding(pathElementsWithoutFilename, "tests");
  }

  private static String normalizeToUnixPathSeparator(String filename) {
    return filename.replace('\\', '/');
  }

  private static boolean isDocDirectory(List<String> pathElements) {
    return pathElements.contains("doc") || pathElements.contains("docs");
  }

  private static boolean isTestDirectory(List<String> pathElements) {
    return pathElements.contains("test") || pathElements.contains("tests") || pathElements.contains("mock") || pathElements.contains("mocks");
  }

  private static boolean hasEnding(List<String> pathElements, String text) {
    return pathElements.stream().anyMatch(pathElement -> pathElement.endsWith(text));
  }
}
