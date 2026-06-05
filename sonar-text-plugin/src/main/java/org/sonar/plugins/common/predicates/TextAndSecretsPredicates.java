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
package org.sonar.plugins.common.predicates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.SonarProduct;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.IndexedFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.plugins.common.analyzer.Analyzer;
import org.sonar.plugins.common.git.GitService;
import org.sonar.plugins.common.git.GitTrackedFilePredicate;
import org.sonar.plugins.common.measures.DurationStatistics;
import org.sonar.plugins.common.measures.TelemetryReporter;
import org.sonar.plugins.common.warnings.AnalysisWarningsWrapper;

import static org.sonar.plugins.common.measures.TelemetryReporter.ALL_TRACKED_TEXT_FILES_MEASURE_KEY;

public class TextAndSecretsPredicates {

  private static final Logger LOG = LoggerFactory.getLogger(TextAndSecretsPredicates.class);
  public static final String EXCLUDED_FILE_SUFFIXES_KEY = "sonar.secrets.excluded.file.suffixes";
  public static final String EXCLUDED_FILE_SUFFIXES_DEFAULT_VALUE = ".adoc,.md,.mdx,.dist,.html,.example,.sample,.template";

  public static final String TEXT_INCLUSIONS_KEY = "sonar.text.inclusions";
  public static final String TEXT_INCLUSIONS_DEFAULT_VALUE = "**/*.sh,**/*.bash,**/*.zsh,**/*.ksh,**/*.ps1,**/*.properties," +
    "**/*.conf,**/*.pem,**/*.config,**/*.env,.aws/config,**/*.key";
  public static final String INCLUSIONS_ACTIVATION_KEY = "sonar.text.inclusions.activate";
  public static final boolean INCLUSIONS_ACTIVATION_DEFAULT_VALUE = true;

  private static final String ANALYZE_ALL_FILES_KEY = "sonar.text.analyzeAllFiles";
  public static final FilePredicate LANGUAGE_FILE_PREDICATE = inputFile -> inputFile.language() != null;

  // Deprecated: Replaced by EXCLUDED_FILE_SUFFIXES_KEY. Will be removed in a future release.
  public static final String DEPRECATED_EXCLUDED_BINARY_FILE_SUFFIXES_KEY = "sonar.text.excluded.file.suffixes";

  private final SensorContext sensorContext;
  protected final GitService gitService;
  private GitTrackedFilePredicate gitTrackedFilePredicate;
  private final DurationStatistics durationStatistics;
  private final TelemetryReporter telemetryReporter;
  private final NotBinaryFilePredicate notBinaryFilePredicate;
  private final AnalysisWarningsWrapper analysisWarningsWrapper;

  public TextAndSecretsPredicates(
    SensorContext sensorContext,
    DurationStatistics durationStatistics,
    TelemetryReporter telemetryReporter,
    GitService gitService,
    AnalysisWarningsWrapper analysisWarningsWrapper) {
    this.sensorContext = sensorContext;
    this.durationStatistics = durationStatistics;
    this.telemetryReporter = telemetryReporter;
    this.notBinaryFilePredicate = notBinaryFilePredicate();
    this.gitService = gitService;
    this.analysisWarningsWrapper = analysisWarningsWrapper;
  }

  /**
   * Blacklist approach: provide a predicate that excludes files that are considered as binary file.
   * Example: For 'exe' it will return false. For 'txt' and 'unknown' it will return true
   */
  private static NotBinaryFilePredicate notBinaryFilePredicate() {
    return new NotBinaryFilePredicate();
  }

  /**
   * General file predicate that is used to match files suitable for the text and secrets analysis.
   * File suffix exclusions are handled in {@link org.sonar.plugins.secrets.utils.CheckContainer}, as they should only apply to secret checks.
   */
  public FilePredicate textAndSecretsPredicate() {
    var predicates = sensorContext.fileSystem().predicates();

    LOG.info("Start fetching files for the text and secrets analysis");
    var analyzeAllFiles = shouldAnalyzeAllFiles(sensorContext);
    if (analyzeAllFiles) {
      LOG.info("Retrieving all except binary files");
      // (not binary file)
      // user exclusions are not taken into account
      return notBinaryFilePredicate;
    }

    // (associated to a language) AND (non-hidden)
    var visibleLanguageFilesPredicate = visibleLanguageFiles();

    // if the inclusions property is inactive, we prevent jgit from being initialized
    if (!isInclusionsActivationEnabled(sensorContext)) {
      LOG.info("Retrieving only language associated files, \"{}\" property is deactivated", INCLUSIONS_ACTIVATION_KEY);
      return visibleLanguageFilesPredicate;
    }

    initializeGitPredicate(sensorContext);
    if (!gitTrackedFilePredicate.isGitStatusSuccessful()) {
      LOG.warn("Retrieving only language associated files, " +
        "make sure to run the analysis inside a git repository to make use of inclusions specified via \"{}\"",
        TEXT_INCLUSIONS_KEY);
      return visibleLanguageFilesPredicate;
    }

    // (included via path patterns)
    var additionalFilesPredicate = includedPathPatternsFilePredicate(sensorContext);

    if (isHiddenFilesAnalysisSupported(sensorContext.runtime())) {
      // (included via path patterns OR hidden file)
      additionalFilesPredicate = predicates.or(IndexedFile::isHidden, additionalFilesPredicate);
    }

    LOG.info("Retrieving language associated files and files included via \"{}\" that are tracked by git", TEXT_INCLUSIONS_KEY);
    return predicates.or(
      visibleLanguageFilesPredicate,
      // (included via path patterns OR hidden file) AND (not excluded file suffix) AND (tracked by git)
      predicates.and(additionalFilesPredicate, gitTrackedFilePredicate));
  }

  /**
   * Whitelist approach: provide a predicate that include files that are considered as text file.
   * Example: For 'exe', 'txt' and 'unknown', it will return true for 'txt'
   * List of path patterns to include are provided by configuration key {@link TextAndSecretsPredicates#TEXT_INCLUSIONS_KEY}
   */
  private static FilePredicate includedPathPatternsFilePredicate(SensorContext sensorContext) {
    String[] includedPathPatterns = sensorContext.config().getStringArray(TEXT_INCLUSIONS_KEY);
    if (includedPathPatterns.length == 0) {
      return sensorContext.fileSystem().predicates().none();
    }

    List<FilePredicate> pathPatternsPredicates = new ArrayList<>();
    for (String pathPattern : includedPathPatterns) {
      var filePredicate = sensorContext.fileSystem().predicates().matchesPathPattern(pathPattern);
      pathPatternsPredicates.add(filePredicate);
    }
    return sensorContext.fileSystem().predicates().or(pathPatternsPredicates);
  }

  private FilePredicate visibleLanguageFiles() {
    if (isHiddenFilesAnalysisSupported(sensorContext.runtime())) {
      var predicates = sensorContext.fileSystem().predicates();
      FilePredicate onlyNonHiddenFiles = inputFile -> !inputFile.isHidden();
      return predicates.and(onlyNonHiddenFiles, LANGUAGE_FILE_PREDICATE);
    }
    // In SonarLint context, we do not support hidden files analysis
    return LANGUAGE_FILE_PREDICATE;
  }

  private static boolean isHiddenFilesAnalysisSupported(SonarRuntime sonarRuntime) {
    // Temporarily exclude SonarLint context, as it's breaking integration tests, where sonar-plugin-api is retrieved from the classpath, and
    // not from the SQ-IDE library
    return !isSonarLintContext(sonarRuntime) && sonarRuntime.getApiVersion().isGreaterThanOrEqual(Analyzer.HIDDEN_FILES_SUPPORTED_API_VERSION);
  }

  /**
   * List of file suffixes to exclude are provided by configuration key {@link TextAndSecretsPredicates#EXCLUDED_FILE_SUFFIXES_KEY} and the deprecated {@link TextAndSecretsPredicates#DEPRECATED_EXCLUDED_BINARY_FILE_SUFFIXES_KEY}.
   * Applied at the check level in {@link org.sonar.plugins.common.analyzer.TextAndSecretsAnalyzer} / {@link org.sonar.plugins.secrets.utils.CheckContainer}, as it only should affect secret rules.
   * Text rules should not be affected by this predicate file filtering.
   */
  public FilePredicate excludedFileSuffixesPredicate(SensorContext sensorContext) {
    String[] excludedFileSuffixes = sensorContext.config().getStringArray(TextAndSecretsPredicates.EXCLUDED_FILE_SUFFIXES_KEY);
    List<String> cleanedSuffixes = cleanExcludedFileSuffixes(excludedFileSuffixes);
    reportExcludedFileSuffixes(cleanedSuffixes);

    // Handling deprecated exclusions
    String[] excludedFileSuffixesFromDeprecatedProperty = sensorContext.config().getStringArray(TextAndSecretsPredicates.DEPRECATED_EXCLUDED_BINARY_FILE_SUFFIXES_KEY);
    List<String> cleanedSuffixesDeprecatedProperty = cleanExcludedFileSuffixes(excludedFileSuffixesFromDeprecatedProperty);
    reportExcludedFileSuffixFromDeprecatedKey(cleanedSuffixesDeprecatedProperty);

    Set<String> suffixesToExclude = new HashSet<>(cleanedSuffixes);
    suffixesToExclude.addAll(cleanedSuffixesDeprecatedProperty);

    var predicates = sensorContext.fileSystem().predicates();
    if (suffixesToExclude.isEmpty()) {
      return predicates.all();
    }

    List<FilePredicate> rejectedSuffixesPredicate = new ArrayList<>();
    for (String suffix : suffixesToExclude) {
      var isExtension = suffix.length() > 1 && suffix.startsWith(".") && suffix.indexOf('.', 1) == -1;
      if (isExtension) {
        rejectedSuffixesPredicate.add(predicates.hasExtension(suffix.substring(1)));
      } else {
        rejectedSuffixesPredicate.add(new FilenameSuffixPredicate(suffix));
      }
    }
    return predicates.not(predicates.or(rejectedSuffixesPredicate));
  }

  /**
   * Report the number of non-binary files tracked by git.
   * For performance reasons, it does not take into account binary files excluded based on content, so the measure can be inflated.
   */
  public void reportAllTrackedTextFilesMeasure() {
    if (isSonarLintContext(sensorContext.runtime())) {
      // In SQ IDE the file predicates are handled differently, and there is no telemetry anyway
      return;
    }

    int allTrackedTextFilesCount = durationStatistics.timed(
      "countAllTrackedTextFiles" + DurationStatistics.SUFFIX_GENERAL,
      () -> countAllTrackedTextFiles(sensorContext, notBinaryFilePredicate));
    telemetryReporter.addNumericMeasure(ALL_TRACKED_TEXT_FILES_MEASURE_KEY, allTrackedTextFilesCount);
  }

  private int countAllTrackedTextFiles(SensorContext sensorContext, NotBinaryFilePredicate notBinaryFilePredicate) {
    if (gitTrackedFilePredicate == null) {
      // If the tracked files have not been computed before, we do not want to compute it here as it can be expensive
      return 0;
    }

    var baseDir = sensorContext.fileSystem().baseDir().toPath();
    // Initializing a new one to not interfere with the logged "ignoredFileNames" of the main one
    var trackedFilesPredicate = new GitTrackedFilePredicate(baseDir, gitService, LANGUAGE_FILE_PREDICATE);

    if (!trackedFilesPredicate.isGitStatusSuccessful()) {
      return 0;
    }

    var predicates = sensorContext.fileSystem().predicates();
    var allTrackedTextFilesPredicate = predicates.and(trackedFilesPredicate, notBinaryFilePredicate);
    // First check if any language is associated with the file to improve performances
    allTrackedTextFilesPredicate = predicates.or(LANGUAGE_FILE_PREDICATE, allTrackedTextFilesPredicate);

    return (int) StreamSupport
      .stream(sensorContext.fileSystem().inputFiles(allTrackedTextFilesPredicate).spliterator(), false)
      .count();
  }

  private void initializeGitPredicate(SensorContext sensorContext) {
    if (gitTrackedFilePredicate == null) {
      var baseDir = sensorContext.fileSystem().baseDir().toPath();
      gitTrackedFilePredicate = durationStatistics.timed(
        "trackedByGitPredicate" + DurationStatistics.SUFFIX_GENERAL,
        () -> new GitTrackedFilePredicate(baseDir, gitService, visibleLanguageFiles()));
    }
  }

  public void logGitTrackedPredicateSummary() {
    if (gitTrackedFilePredicate != null) {
      gitTrackedFilePredicate.logSummary();
    }
  }

  private static boolean isInclusionsActivationEnabled(SensorContext sensorContext) {
    return sensorContext.config().getBoolean(INCLUSIONS_ACTIVATION_KEY).orElse(INCLUSIONS_ACTIVATION_DEFAULT_VALUE);
  }

  public static boolean isSonarLintContext(SonarRuntime runtime) {
    return runtime.getProduct() == SonarProduct.SONARLINT;
  }

  private static boolean shouldAnalyzeAllFiles(SensorContext sensorContext) {
    return isSonarLintContext(sensorContext.runtime()) || sensorContext.config().getBoolean(ANALYZE_ALL_FILES_KEY).orElse(false);
  }

  private static List<String> cleanExcludedFileSuffixes(String[] excludedFileSuffixes) {
    return Arrays.stream(excludedFileSuffixes)
      .map(String::trim)
      .map(value -> value.replaceAll("[^a-zA-Z0-9-_.]", ""))
      .filter(value -> !value.isEmpty())
      .distinct()
      .toList();
  }

  public void reportExcludedFileSuffixes(List<String> cleanedSuffixes) {
    List<String> defaultSuffixes = cleanExcludedFileSuffixes(
      EXCLUDED_FILE_SUFFIXES_DEFAULT_VALUE.split(","));
    if (!cleanedSuffixes.equals(defaultSuffixes)) {
      telemetryReporter.addListAsStringMeasure("secrets.excluded.user.suffix", cleanedSuffixes);
    }
  }

  public void reportExcludedFileSuffixFromDeprecatedKey(List<String> cleanedSuffixesDeprecatedProperty) {
    if (!cleanedSuffixesDeprecatedProperty.isEmpty()) {
      var deprecationNotice = String.format("""

        The property "%s" is deprecated and will be removed in a future release.
        Please migrate all excluded file suffixes to "%s".

        """, TextAndSecretsPredicates.DEPRECATED_EXCLUDED_BINARY_FILE_SUFFIXES_KEY, TextAndSecretsPredicates.EXCLUDED_FILE_SUFFIXES_KEY);
      analysisWarningsWrapper.addWarning(deprecationNotice);
      LOG.warn(deprecationNotice);
      telemetryReporter.addListAsStringMeasure("excluded.user.suffix", cleanedSuffixesDeprecatedProperty);
    }
  }
}
