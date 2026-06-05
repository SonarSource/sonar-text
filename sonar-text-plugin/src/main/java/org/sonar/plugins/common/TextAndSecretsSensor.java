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
package org.sonar.plugins.common;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarProduct;
import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.fs.FilePredicate;
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
import org.sonar.plugins.common.git.LazyGitService;
import org.sonar.plugins.common.measures.CiVendorFilesTelemetry;
import org.sonar.plugins.common.measures.DurationStatistics;
import org.sonar.plugins.common.measures.MemoryMonitor;
import org.sonar.plugins.common.measures.TelemetryReporter;
import org.sonar.plugins.common.predicates.TextAndSecretsPredicates;
import org.sonar.plugins.common.thread.ParallelizationManager;
import org.sonar.plugins.common.warnings.AnalysisWarningsWrapper;
import org.sonar.plugins.common.warnings.DefaultAnalysisWarningsWrapper;
import org.sonar.plugins.secrets.SecretsCheckList;
import org.sonar.plugins.secrets.SecretsRulesDefinition;
import org.sonar.plugins.secrets.api.MessageFormatter;
import org.sonar.plugins.secrets.api.SecretsSpecificationLoader;
import org.sonar.plugins.secrets.api.SpecificationBasedCheck;
import org.sonar.plugins.secrets.api.SpecificationConfiguration;
import org.sonar.plugins.secrets.api.filters.RejectionLogger;
import org.sonar.plugins.secrets.api.filters.SkippedFilter;
import org.sonar.plugins.secrets.api.task.RegexMatchingManager;
import org.sonar.plugins.secrets.configuration.SecretsSpecificationContainer;
import org.sonar.plugins.secrets.utils.CheckContainer;
import org.sonar.plugins.text.TextCheckList;
import org.sonar.plugins.text.TextRuleDefinition;
import org.sonar.plugins.text.api.AbstractUnicodeSequenceCheck;
import org.sonar.plugins.text.checks.BIDICharacterCheck;

import static org.sonar.plugins.common.measures.TelemetryReporter.SENSOR_DISABLED_MEASURE_KEY;

public class TextAndSecretsSensor implements Sensor {

  private static final Logger LOG = LoggerFactory.getLogger(TextAndSecretsSensor.class);
  public static final String REGEX_MATCH_TIMEOUT_KEY = "sonar.text.regex.timeout.match";
  public static final String REGEX_EXECUTION_TIMEOUT_KEY = "sonar.text.regex.timeout.execution";
  public static final String ANALYZER_ACTIVATION_KEY = "sonar.text.activate";
  public static final boolean ANALYZER_ACTIVATION_DEFAULT_VALUE = true;
  public static final String THREAD_NUMBER_KEY = "sonar.text.threads";
  public static final String TEXT_CATEGORY = "Secrets";
  public static final String SONAR_TESTS_KEY = "sonar.tests";
  public static final String DISABLE_ENTROPY_FILTER_KEY = "sonar.secrets.disableEntropyFilter";
  public static final boolean DISABLE_ENTROPY_FILTER_DEFAULT_VALUE = false;
  public static final String DISABLE_TEST_FILE_DETECTION_KEY = "sonar.secrets.disableTestFileDetection";
  public static final boolean DISABLE_TEST_FILE_DETECTION_DEFAULT_VALUE = false;
  /**
   * Internal debug switch (off by default) that enables per-candidate debug logging when a post-filter rejects a
   * match.
   */
  public static final String DEBUG_LOG_REJECTED_CANDIDATES_KEY = "sonar.text.debug.logRejectedCandidates";
  public static final boolean DEBUG_LOG_REJECTED_CANDIDATES_DEFAULT_VALUE = false;
  public static final String DEBUG_LOG_REJECTED_CANDIDATES_LIMIT_KEY = "sonar.text.debug.logRejectedCandidates.maxPerRulePerFile";
  public static final int DEBUG_LOG_REJECTED_CANDIDATES_LIMIT_DEFAULT_VALUE = 20;

  protected final CheckFactory checkFactory;
  protected final SonarRuntime sonarRuntime;
  protected final AnalysisWarningsWrapper analysisWarnings;
  private final SecretsSpecificationContainer secretsSpecificationContainer;
  private final CheckContainer checkContainer;
  protected DurationStatistics durationStatistics;
  protected TelemetryReporter telemetryReporter;
  protected MemoryMonitor memoryMonitor;
  protected ParallelizationManager parallelizationManager;
  protected TextAndSecretsPredicates textAndSecretsPredicates;
  protected GitService gitService;

  public TextAndSecretsSensor(SonarRuntime sonarRuntime, CheckFactory checkFactory, SecretsSpecificationContainer secretsSpecificationContainer,
    CheckContainer checkContainer) {
    this(sonarRuntime, checkFactory, DefaultAnalysisWarningsWrapper.NOOP_ANALYSIS_WARNINGS, secretsSpecificationContainer, checkContainer);
  }

  public TextAndSecretsSensor(
    SonarRuntime sonarRuntime,
    CheckFactory checkFactory,
    AnalysisWarningsWrapper analysisWarnings,
    SecretsSpecificationContainer secretsSpecificationContainer,
    CheckContainer checkContainer) {
    this.sonarRuntime = sonarRuntime;
    this.checkFactory = checkFactory;
    this.analysisWarnings = analysisWarnings;
    this.secretsSpecificationContainer = secretsSpecificationContainer;
    this.checkContainer = checkContainer;
  }

  @Override
  public void describe(SensorDescriptor descriptor) {
    descriptor
      .name("TextAndSecretsSensor")
      .createIssuesForRuleRepositories(TextRuleDefinition.REPOSITORY_KEY, SecretsRulesDefinition.REPOSITORY_KEY)
      .processesFilesIndependently()
      // Using global because implemented ProjectSensor doesn't work with the LITS Plugin we use in IntegrationTest
      .global();
    activateHiddenFilesProcessing(descriptor);
  }

  protected void activateHiddenFilesProcessing(SensorDescriptor descriptor) {
    if (isHiddenFilesAnalysisSupported(sonarRuntime)) {
      descriptor.processesHiddenFiles();
    }
  }

  @Override
  public void execute(SensorContext sensorContext) {
    if (!isActive(sensorContext)) {
      LOG.info("The text and secrets analysis was deactivated using the property \"{}\"", ANALYZER_ACTIVATION_KEY);
      LOG.info("Experiencing any issues with the text and secrets analysis? " +
        "Please report them at https://community.sonarsource.com/tag/secrets - your feedback helps us improve the product!");
      new TelemetryReporter(sensorContext)
        .addNumericMeasure(SENSOR_DISABLED_MEASURE_KEY, 1)
        .report();
      return;
    }
    initialize(sensorContext);

    var activeChecks = getActiveChecks();
    if (activeChecks.isEmpty()) {
      return;
    }

    initializeChecks(activeChecks, sensorContext, createSpecificationConfiguration(sensorContext));

    runAnalysis(sensorContext, activeChecks);

    afterAnalysis(activeChecks);
    processMetrics();
    cleanUp();
  }

  protected SpecificationConfiguration createSpecificationConfiguration(SensorContext sensorContext) {
    var skippedFilters = resolveSkippedFilters(sensorContext);
    var automaticTestFileDetection = resolveAutomaticTestFileDetection(sensorContext);
    var rejectionLogger = resolveRejectionLogger(sensorContext);
    return new SpecificationConfiguration(automaticTestFileDetection, skippedFilters, MessageFormatter.RULE_MESSAGE, rejectionLogger);
  }

  protected static RejectionLogger resolveRejectionLogger(SensorContext sensorContext) {
    boolean enabled = sensorContext.config().getBoolean(DEBUG_LOG_REJECTED_CANDIDATES_KEY)
      .orElse(DEBUG_LOG_REJECTED_CANDIDATES_DEFAULT_VALUE);
    if (!enabled) {
      return RejectionLogger.DISABLED;
    }
    int limit = sensorContext.config().getInt(DEBUG_LOG_REJECTED_CANDIDATES_LIMIT_KEY)
      .orElse(DEBUG_LOG_REJECTED_CANDIDATES_LIMIT_DEFAULT_VALUE);
    return RejectionLogger.create(limit);
  }

  private static boolean resolveAutomaticTestFileDetection(SensorContext sensorContext) {
    var sonarTestsValue = sensorContext.config().get(SONAR_TESTS_KEY).orElse("");
    if (sonarTestsValue.isBlank() && !isSonarLintContext(sensorContext.runtime())) {
      var message = """
        The property "%s" is not set. To improve the analysis accuracy, we categorize a file as a test file if any of the following is true:
          * The filename starts with "test"
          * The filename contains "test." or "tests."
          * Any directory in the file path is named: "doc", "docs", "test", "tests", "mock" or "mocks"
          * Any directory in the file path has a name ending in "test" or "tests"
        """.formatted(SONAR_TESTS_KEY);
      LOG.info(message);
      return true;
    }
    return false;
  }

  private static Set<SkippedFilter> resolveSkippedFilters(SensorContext sensorContext) {
    EnumSet<SkippedFilter> skippedFilters = EnumSet.noneOf(SkippedFilter.class);
    if (sensorContext.config().getBoolean(DISABLE_ENTROPY_FILTER_KEY).orElse(DISABLE_ENTROPY_FILTER_DEFAULT_VALUE)) {
      skippedFilters.add(SkippedFilter.ENTROPY_FILTER);
    }
    if (sensorContext.config().getBoolean(DISABLE_TEST_FILE_DETECTION_KEY).orElse(DISABLE_TEST_FILE_DETECTION_DEFAULT_VALUE)) {
      skippedFilters.add(SkippedFilter.TEST_FILES_FILTER);
    }

    if (!skippedFilters.isEmpty()) {
      var filterNames = skippedFilters.stream().map(SkippedFilter::filterName).collect(Collectors.joining(", "));
      LOG.info("The secret analysis will skip the following filters per user configuration: {}", filterNames);
    }
    return Set.copyOf(skippedFilters);
  }

  protected void runAnalysis(SensorContext sensorContext, List<Check> activeChecks) {
    runTextAndSecretsAnalysis(sensorContext, activeChecks);
  }

  private void runTextAndSecretsAnalysis(SensorContext sensorContext, List<Check> activeChecks) {
    var suitableChecks = TextAndSecretsAnalyzer.filterSuitableChecks(activeChecks);
    if (suitableChecks.isEmpty()) {
      return;
    }

    var filePredicate = textAndSecretsPredicates.textAndSecretsPredicate();

    List<InputFile> inputFiles = durationStatistics.timed(
      "applyFilePredicate" + DurationStatistics.SUFFIX_GENERAL,
      () -> getInputFiles(sensorContext, filePredicate));

    var analyzer = new TextAndSecretsAnalyzer(sensorContext, parallelizationManager, durationStatistics, suitableChecks, telemetryReporter, memoryMonitor,
      checkContainer);
    durationStatistics.timed("analyzerTotal" + DurationStatistics.SUFFIX_GENERAL, () -> analyzer.analyzeFiles(inputFiles));
    logCheckBasedStatistics(suitableChecks);
    textAndSecretsPredicates.reportAllTrackedTextFilesMeasure();
  }

  private static boolean isHiddenFilesAnalysisSupported(SonarRuntime sonarRuntime) {
    // Temporarily exclude SonarLint context, as it's breaking integration tests, where sonar-plugin-api is retrieved from the classpath, and
    // not from the SQ-IDE library
    return !isSonarLintContext(sonarRuntime) && sonarRuntime.getApiVersion().isGreaterThanOrEqual(Analyzer.HIDDEN_FILES_SUPPORTED_API_VERSION);
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
    gitService = initializeGitService(sensorContext);
    textAndSecretsPredicates = new TextAndSecretsPredicates(sensorContext, durationStatistics, telemetryReporter, gitService, analysisWarnings);
    CiVendorFilesTelemetry.measureProjectsCIFilesInclusion(sensorContext, telemetryReporter);
    reportDisabledSecretFilters(sensorContext);
    initializeParallelizationManager(sensorContext);
    initializeOptionalConfigValue(sensorContext, REGEX_MATCH_TIMEOUT_KEY, RegexMatchingManager::setTimeoutMs);
    initializeOptionalConfigValue(sensorContext, REGEX_EXECUTION_TIMEOUT_KEY, RegexMatchingManager::setUninterruptibleTimeoutMs);

    secretsSpecificationContainer.initialize(this::constructSpecificationLoader, durationStatistics);
  }

  private GitService initializeGitService(SensorContext sensorContext) {
    var baseDir = sensorContext.fileSystem().baseDir().toPath();
    return createGitService(baseDir);
  }

  // For testing purposes
  public GitService createGitService(Path baseDir) {
    return new LazyGitService(() -> new CachingGitService(new GitCliAndJGitService(baseDir)));
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

  protected void initializeChecks(List<Check> checks, SensorContext sensorContext, SpecificationConfiguration specificationConfiguration) {
    durationStatistics.timed("initializingSecretMatchers" + DurationStatistics.SUFFIX_GENERAL, () -> {
      for (Check activeCheck : checks) {
        if (activeCheck instanceof SpecificationBasedCheck specificationBasedCheck) {
          specificationBasedCheck.initialize(secretsSpecificationContainer.getSpecificationLoader(), durationStatistics, specificationConfiguration);
        } else if (activeCheck instanceof BIDICharacterCheck bidiCharacterCheck) {
          bidiCharacterCheck.initialize(durationStatistics);
        } else if (activeCheck instanceof AbstractUnicodeSequenceCheck unicodeSequenceCheck) {
          unicodeSequenceCheck.initialize(durationStatistics);
        }
      }
    });
    var secretSuffixExclusionPredicate = textAndSecretsPredicates.excludedFileSuffixesPredicate(sensorContext);
    checkContainer.initialize(checks, secretsSpecificationContainer.getSpecificationLoader(), durationStatistics, secretSuffixExclusionPredicate);
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

  private void logWarningConsoleUI(String message) {
    LOG.warn(message);
    analysisWarnings.addWarning(message);
  }

  protected SecretsSpecificationLoader constructSpecificationLoader() {
    return new SecretsSpecificationLoader();
  }

  protected void logCheckBasedStatistics(List<Check> activeChecks) {
    // nothing to do here for this class
  }

  protected void afterAnalysis(List<Check> activeChecks) {
    // nothing to do here for this class
  }

  private void processMetrics() {
    telemetryReporter.endRecordingSensorTime(getEditionName());
    telemetryReporter.report();
    durationStatistics.log();
    textAndSecretsPredicates.logGitTrackedPredicateSummary();
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
    parallelizationManager.shutdown();
    RegexMatchingManager.shutdown();
    textAndSecretsPredicates = null;
    try {
      gitService.close();
    } catch (Exception e) {
      LOG.debug("Error closing GitService", e);
    }
  }

  private static boolean isActive(SensorContext sensorContext) {
    return sensorContext.config().getBoolean(ANALYZER_ACTIVATION_KEY).orElse(ANALYZER_ACTIVATION_DEFAULT_VALUE);
  }

  public static boolean isSonarLintContext(SonarRuntime runtime) {
    return runtime.getProduct() == SonarProduct.SONARLINT;
  }

  private static boolean isSonarCloudContext(SensorContext sensorContext) {
    return !isSonarLintContext(sensorContext.runtime()) && sensorContext.runtime().getEdition() == SonarEdition.SONARCLOUD;
  }

  private void reportDisabledSecretFilters(SensorContext sensorContext) {
    boolean entropyFilterDisabled = sensorContext.config().getBoolean(DISABLE_ENTROPY_FILTER_KEY).orElse(DISABLE_ENTROPY_FILTER_DEFAULT_VALUE);
    telemetryReporter.addNumericMeasure("secrets.disable_entropy_filter", entropyFilterDisabled ? 1 : 0);

    boolean testFileDetectionDisabled = sensorContext.config().getBoolean(DISABLE_TEST_FILE_DETECTION_KEY).orElse(DISABLE_TEST_FILE_DETECTION_DEFAULT_VALUE);
    telemetryReporter.addNumericMeasure("secrets.disable_test_file_detection", testFileDetectionDisabled ? 1 : 0);
  }
}
