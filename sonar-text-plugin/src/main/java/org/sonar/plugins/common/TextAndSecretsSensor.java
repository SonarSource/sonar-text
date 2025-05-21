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
package org.sonar.plugins.common;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarProduct;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.IndexedFile;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.plugins.common.analyzer.Analyzer;
import org.sonar.plugins.common.analyzer.TextAndSecretsAnalyzer;
import org.sonar.plugins.common.git.CachingGitService;
import org.sonar.plugins.common.git.GitCliAndJGitService;
import org.sonar.plugins.common.git.GitService;
import org.sonar.plugins.common.git.GitTrackedFilePredicate;
import org.sonar.plugins.common.git.LazyGitService;
import org.sonar.plugins.common.measures.DurationStatistics;
import org.sonar.plugins.common.measures.MemoryMonitor;
import org.sonar.plugins.common.measures.TelemetryReporter;
import org.sonar.plugins.common.thread.ParallelizationManager;
import org.sonar.plugins.common.warnings.AnalysisWarningsWrapper;
import org.sonar.plugins.common.warnings.DefaultAnalysisWarningsWrapper;
import org.sonar.plugins.secrets.SecretsCheckList;
import org.sonar.plugins.secrets.SecretsRulesDefinition;
import org.sonar.plugins.secrets.api.SecretsSpecificationLoader;
import org.sonar.plugins.secrets.api.SpecificationBasedCheck;
import org.sonar.plugins.secrets.api.SpecificationConfiguration;
import org.sonar.plugins.secrets.api.task.RegexMatchingManager;
import org.sonar.plugins.text.TextCheckList;
import org.sonar.plugins.text.TextRuleDefinition;

public class TextAndSecretsSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(TextAndSecretsSensor.class);
  public static final String EXCLUDED_FILE_SUFFIXES_KEY = "sonar.text.excluded.file.suffixes";
  public static final String TEXT_INCLUSIONS_KEY = "sonar.text.inclusions";

  // On UNIX systems, files or directories starting with a dot are not being retrieved by the scanner
  public static final String TEXT_INCLUSIONS_DEFAULT_VALUE = "**/*.sh,**/*.bash,**/*.zsh,**/*.ksh,**/*.ps1,**/*.properties," +
    "**/*.conf,**/*.pem,**/*.config,.env,.aws/config";
  private static final String ANALYZE_ALL_FILES_KEY = "sonar.text.analyzeAllFiles";
  public static final String REGEX_MATCH_TIMEOUT_KEY = "sonar.text.regex.timeout.match";
  public static final String REGEX_EXECUTION_TIMEOUT_KEY = "sonar.text.regex.timeout.execution";
  public static final String ANALYZER_ACTIVATION_KEY = "sonar.text.activate";
  public static final boolean ANALYZER_ACTIVATION_DEFAULT_VALUE = true;
  public static final String INCLUSIONS_ACTIVATION_KEY = "sonar.text.inclusions.activate";
  public static final boolean INCLUSIONS_ACTIVATION_DEFAULT_VALUE = true;
  public static final String THREAD_NUMBER_KEY = "sonar.text.threads";
  public static final String TEXT_CATEGORY = "Secrets";
  public static final String SONAR_TESTS_KEY = "sonar.tests";
  public static final FilePredicate LANGUAGE_FILE_PREDICATE = inputFile -> inputFile.language() != null;

  protected final CheckFactory checkFactory;

  protected final AnalysisWarningsWrapper analysisWarnings;
  protected DurationStatistics durationStatistics;
  protected TelemetryReporter telemetryReporter;
  protected MemoryMonitor memoryMonitor;
  protected ParallelizationManager parallelizationManager;
  protected GitService gitService;
  private GitTrackedFilePredicate gitTrackedFilePredicate;

  public TextAndSecretsSensor(CheckFactory checkFactory) {
    this(checkFactory, DefaultAnalysisWarningsWrapper.NOOP_ANALYSIS_WARNINGS);
  }

  public TextAndSecretsSensor(CheckFactory checkFactory, AnalysisWarningsWrapper analysisWarnings) {
    this.checkFactory = checkFactory;
    this.analysisWarnings = analysisWarnings;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("TextAndSecretsSensor")
      .createIssuesForRuleRepositories(TextRuleDefinition.REPOSITORY_KEY, SecretsRulesDefinition.REPOSITORY_KEY)
      .processesFilesIndependently()
      // Using global because implemented ProjectSensor doesn't work with the LITS Plugin we use in IntegrationTest
      .global();
  }

  @Override
  public void execute(SensorContext sensorContext) {
    if (!isActive(sensorContext)) {
      return;
    }
    initialize(sensorContext);

    List<Check> activeChecks = getActiveChecks();
    if (activeChecks.isEmpty()) {
      return;
    }

    initializeChecks(activeChecks, new SpecificationConfiguration(enableAutomaticTestFileDetection(sensorContext)));

    runAnalysis(sensorContext, activeChecks);

    processMetrics();
    cleanUp();
  }

  protected void runAnalysis(SensorContext sensorContext, List<Check> activeChecks) {
    runTextAndSecretsAnalysis(sensorContext, activeChecks);
  }

  private void runTextAndSecretsAnalysis(SensorContext sensorContext, List<Check> activeChecks) {
    var suitableChecks = TextAndSecretsAnalyzer.filterSuitableChecks(activeChecks);
    if (suitableChecks.isEmpty()) {
      return;
    }

    // Retrieve list of files to analyse using the right FilePredicate
    boolean shouldAnalyzeAllFiles = shouldAnalyzeAllFiles(sensorContext);
    var notBinaryFilePredicate = notBinaryFilePredicate(sensorContext);
    var filePredicate = constructGeneralFilePredicate(sensorContext, notBinaryFilePredicate, shouldAnalyzeAllFiles);

    List<InputFile> inputFiles = getInputFiles(sensorContext, filePredicate);

    var analyzer = new TextAndSecretsAnalyzer(sensorContext, parallelizationManager, durationStatistics, suitableChecks, telemetryReporter, memoryMonitor, notBinaryFilePredicate,
      shouldAnalyzeAllFiles);
    durationStatistics.timed("analyzerTotal" + DurationStatistics.SUFFIX_GENERAL, () -> analyzer.analyzeFiles(inputFiles));
    logCheckBasedStatistics(suitableChecks);
  }

  private FilePredicate constructGeneralFilePredicate(SensorContext sensorContext, FilePredicate notBinaryFilePredicate, boolean analyzeAllFiles) {
    LOG.info("Start fetching files for the text and secrets analysis");
    if (analyzeAllFiles) {
      // if we're in a sonarlint context, we return this predicate as well
      LOG.info("Retrieving all except non binary files");
      return notBinaryFilePredicate;
    }

    // if the property is inactive, we prevent jgit from being initialized
    if (!isGitAndInclusionsActive(sensorContext)) {
      LOG.info("Retrieving only language associated files, \"{}\" property is deactivated", INCLUSIONS_ACTIVATION_KEY);
      return LANGUAGE_FILE_PREDICATE;
    }

    initializeGitPredicate(sensorContext);
    if (!gitTrackedFilePredicate.isGitStatusSuccessful()) {
      LOG.warn("Retrieving only language associated files, " +
        "make sure to run the analysis inside a git repository to make use of inclusions specified via \"{}\"",
        TEXT_INCLUSIONS_KEY);
      return LANGUAGE_FILE_PREDICATE;
    }

    // Retrieve list of files to analyse using the right FilePredicate
    var includedFilesPredicate = includedPathPatternsFilePredicate(sensorContext);

    var predicates = sensorContext.fileSystem().predicates();
    if (shouldAnalyzeAllTrackedHiddenFiles(sensorContext)) {
      includedFilesPredicate = predicates.or(IndexedFile::isHidden, includedFilesPredicate);
    }

    LOG.info("Retrieving language associated files and files included via \"{}\" that are tracked by git", TEXT_INCLUSIONS_KEY);
    return predicates.or(
      LANGUAGE_FILE_PREDICATE,
      predicates.and(includedFilesPredicate, gitTrackedFilePredicate));
  }

  /**
   * Blacklist approach: provide a predicate that exclude file that are considered as not-binary file.
   * Example: for 'exe', 'txt' and 'unknown', it will return true for 'txt' and 'unknown'
   * List of binary extension to exclude are provided by configuration key {@link TextAndSecretsSensor#EXCLUDED_FILE_SUFFIXES_KEY}
   */
  private static NotBinaryFilePredicate notBinaryFilePredicate(SensorContext sensorContext) {
    return new NotBinaryFilePredicate(sensorContext.config().getStringArray(TextAndSecretsSensor.EXCLUDED_FILE_SUFFIXES_KEY));
  }

  /**
   * Whitelist approach: provide a predicate that include file that are considered as text file.
   * Example: for 'exe', 'txt' and 'unknown', it will return true for 'txt'
   * List of path patterns to include are provided by configuration key {@link TextAndSecretsSensor#TEXT_INCLUSIONS_KEY}
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

  private static boolean shouldAnalyzeAllTrackedHiddenFiles(SensorContext sensorContext) {
    return sensorContext.runtime().getApiVersion().isGreaterThanOrEqual(Analyzer.HIDDEN_FILES_SUPPORTED_API_VERSION);
  }

  protected static boolean enableAutomaticTestFileDetection(SensorContext sensorContext) {
    var value = sensorContext.config().get(SONAR_TESTS_KEY).orElse("");
    if (value.isBlank() && !isSonarLintContext(sensorContext)) {
      var message = """
        The property "%s" is not set. To improve the analysis accuracy, we categorize a file as a test file if any of the following is true:
          * The filename starts with "test"
          * The filename contains "test." or "tests."
          * Any directory in the file path is named: "doc", "docs", "test" or "tests"
          * Any directory in the file path has a name ending in "test" or "tests"
        """.formatted(SONAR_TESTS_KEY);
      LOG.info(message);
      return true;
    }
    return false;
  }

  /**
   * In SonarLint context we want to analyze all non-binary input files, even when they are not analyzed or assigned to a language.
   * To avoid analyzing all non-binary files to reduce time and memory consumption in a non SonarLint context only files assigned to a
   * language OR file with a text extension are analyzed.
   */
  protected static List<InputFile> getInputFiles(SensorContext sensorContext, FilePredicate filePredicate) {
    List<InputFile> inputFiles = new ArrayList<>();
    var fileSystem = sensorContext.fileSystem();
    for (InputFile inputFile : fileSystem.inputFiles(filePredicate)) {
      inputFiles.add(inputFile);
    }
    return inputFiles;
  }

  protected List<Check> getActiveChecks() {
    List<Check> checks = new ArrayList<>(checkFactory.<Check>create(TextRuleDefinition.REPOSITORY_KEY)
      .addAnnotatedChecks(new TextCheckList().checks()).all());

    checks.addAll(checkFactory.<Check>create(SecretsRulesDefinition.REPOSITORY_KEY)
      .addAnnotatedChecks(new SecretsCheckList().checks()).all());
    return checks;
  }

  private void initialize(SensorContext sensorContext) {
    memoryMonitor = new MemoryMonitor(sensorContext.config());
    durationStatistics = new DurationStatistics(sensorContext.config());
    telemetryReporter = new TelemetryReporter(sensorContext);
    telemetryReporter.startRecordingSensorTime();
    initializeParallelizationManager(sensorContext);
    initializeGitService(sensorContext);
    initializeOptionalConfigValue(sensorContext, REGEX_MATCH_TIMEOUT_KEY, RegexMatchingManager::setTimeoutMs);
    initializeOptionalConfigValue(sensorContext, REGEX_EXECUTION_TIMEOUT_KEY, RegexMatchingManager::setUninterruptibleTimeoutMs);
  }

  private void initializeParallelizationManager(SensorContext sensorContext) {
    int availableProcessors = Runtime.getRuntime().availableProcessors();
    Optional<Integer> threadOption = sensorContext.config().getInt(THREAD_NUMBER_KEY);
    int threads = availableProcessors;

    var logMessageSuffix = "";
    if (threadOption.isPresent()) {
      threads = threadOption.get();
      logMessageSuffix = ", according to the value of \"" + THREAD_NUMBER_KEY + "\" property";
      if (threads > availableProcessors) {
        if (isSonarCloudContext(sensorContext)) {
          threads = availableProcessors;
          logMessageSuffix = ", \"" + THREAD_NUMBER_KEY + "\" is ignored";
        } else {
          logWarningConsoleUI("\"" + THREAD_NUMBER_KEY + "\" property was set to " + threads + ", " +
            "which is greater than the number of available processors: " + availableProcessors + ".\n" +
            "It is recommended to let the analyzer detect the number of threads automatically by not setting the property.\n" +
            "For more information, visit the documentation page.");
        }
      }
    }

    LOG.info("Available processors: {}", availableProcessors);
    LOG.info("Using {} {} for analysis{}.", threads, (threads != 1 ? "threads" : "thread"), logMessageSuffix);

    parallelizationManager = new ParallelizationManager(threads);
    RegexMatchingManager.initialize(threads);
  }

  private void initializeGitService(SensorContext sensorContext) {
    var baseDir = sensorContext.fileSystem().baseDir().toPath();
    gitService = createGitService(baseDir);
  }

  protected void initializeChecks(List<Check> checks, SpecificationConfiguration specificationConfiguration) {
    var specificationLoader = durationStatistics.timed("deserializingSpecifications" + DurationStatistics.SUFFIX_GENERAL,
      this::constructSpecificationLoader);

    durationStatistics.timed("initializingSecretMatchers" + DurationStatistics.SUFFIX_GENERAL, () -> {
      for (Check activeCheck : checks) {
        if (activeCheck instanceof SpecificationBasedCheck specificationBasedCheck) {
          (specificationBasedCheck).initialize(specificationLoader, durationStatistics, specificationConfiguration);
        }
      }
    });
  }

  private static void initializeOptionalConfigValue(SensorContext sensorContext, String key, Consumer<Integer> setConfigValue) {
    try {
      Optional<Integer> valueAsInt = sensorContext.config().getInt(key);
      valueAsInt.ifPresent(setConfigValue);
    } catch (IllegalStateException e) {
      // provided value not in the expected format - do nothing
      LOG.debug("Provided value with key \"{}\" is not parseable as an integer", key, e);
    }
  }

  private void initializeGitPredicate(SensorContext sensorContext) {
    if (gitTrackedFilePredicate == null) {
      var baseDir = sensorContext.fileSystem().baseDir().toPath();
      gitTrackedFilePredicate = durationStatistics.timed(
        "trackedByGitPredicate" + DurationStatistics.SUFFIX_GENERAL,
        () -> new GitTrackedFilePredicate(baseDir, gitService, LANGUAGE_FILE_PREDICATE));
    }
  }

  private void logWarningConsoleUI(String message) {
    LOG.warn(message);
    analysisWarnings.addWarning(message);
  }

  protected SecretsSpecificationLoader constructSpecificationLoader() {
    return new SecretsSpecificationLoader();
  }

  public GitService createGitService(Path baseDir) {
    return new LazyGitService(() -> new CachingGitService(new GitCliAndJGitService(baseDir)));
  }

  protected void logCheckBasedStatistics(List<Check> activeChecks) {
    // nothing to do here for this class
  }

  private void processMetrics() {
    telemetryReporter.endRecordingSensorTime(getEditionName());
    telemetryReporter.report();
    durationStatistics.log();
    if (gitTrackedFilePredicate != null) {
      gitTrackedFilePredicate.logSummary();
    }
    memoryMonitor.addRecord("End of the sensor");
    memoryMonitor.logMemory();
  }

  protected String getEditionName() {
    return SonarEdition.COMMUNITY.getLabel();
  }

  private void cleanUp() {
    durationStatistics = null;
    telemetryReporter = null;
    memoryMonitor = null;
    gitTrackedFilePredicate = null;
    parallelizationManager.shutdown();
    RegexMatchingManager.shutdown();
    try {
      gitService.close();
    } catch (Exception e) {
      LOG.debug("Error closing GitService", e);
    }
  }

  private static boolean isActive(SensorContext sensorContext) {
    return sensorContext.config().getBoolean(ANALYZER_ACTIVATION_KEY).orElse(ANALYZER_ACTIVATION_DEFAULT_VALUE);
  }

  private static boolean isGitAndInclusionsActive(SensorContext sensorContext) {
    return sensorContext.config().getBoolean(INCLUSIONS_ACTIVATION_KEY).orElse(INCLUSIONS_ACTIVATION_DEFAULT_VALUE);
  }

  private static boolean isSonarLintContext(SensorContext sensorContext) {
    return sensorContext.runtime().getProduct() == SonarProduct.SONARLINT;
  }

  private static boolean isSonarCloudContext(SensorContext sensorContext) {
    return !isSonarLintContext(sensorContext) && sensorContext.runtime().getEdition() == SonarEdition.SONARCLOUD;
  }

  private static boolean shouldAnalyzeAllFiles(SensorContext sensorContext) {
    return isSonarLintContext(sensorContext) || sensorContext.config().getBoolean(ANALYZE_ALL_FILES_KEY).orElse(false);
  }
}
