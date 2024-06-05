/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarProduct;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.plugins.common.git.GitService;
import org.sonar.plugins.common.git.GitTrackedFilePredicate;
import org.sonar.plugins.common.thread.ParallelizationManager;
import org.sonar.plugins.common.warnings.AnalysisWarningsWrapper;
import org.sonar.plugins.common.warnings.DefaultAnalysisWarningsWrapper;
import org.sonar.plugins.secrets.SecretsCheckList;
import org.sonar.plugins.secrets.SecretsRulesDefinition;
import org.sonar.plugins.secrets.api.SpecificationBasedCheck;
import org.sonar.plugins.secrets.api.SpecificationConfiguration;
import org.sonar.plugins.secrets.api.SpecificationLoader;
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
  public static final boolean INCLUSIONS_ACTIVATION_DEFAULT_VALUE = false;
  public static final String THREAD_NUMBER_KEY = "sonar.text.threads";
  public static final String TEXT_CATEGORY = "Secrets";
  private static final String SONAR_TESTS_KEY = "sonar.tests";
  private static final FilePredicate LANGUAGE_FILE_PREDICATE = inputFile -> inputFile.language() != null;

  protected final CheckFactory checkFactory;

  protected final AnalysisWarningsWrapper analysisWarnings;
  protected DurationStatistics durationStatistics;
  private ParallelizationManager parallelizationManager;

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

    initializeParallelizationManager(sensorContext);

    // Retrieve list of checks
    List<Check> activeChecks = getActiveChecks();
    durationStatistics = new DurationStatistics(sensorContext.config());
    initializeSpecificationBasedChecks(activeChecks, sensorContext);
    if (activeChecks.isEmpty()) {
      return;
    }

    // Retrieve list of files to analyse using the right FilePredicate
    boolean analyzeAllFiles = isSonarLintContext(sensorContext) || analyzeAllFiles(sensorContext);
    var notBinaryFilePredicate = notBinaryFilePredicate(sensorContext);
    var filePredicate = constructFilePredicate(sensorContext, notBinaryFilePredicate, analyzeAllFiles);

    List<InputFile> inputFiles = getInputFiles(sensorContext, filePredicate);
    if (inputFiles.isEmpty()) {
      LOG.debug("There are no files to be analyzed");
      return;
    }

    configureRegexEngineTimeout(sensorContext, REGEX_MATCH_TIMEOUT_KEY, RegexMatchingManager::setTimeoutMs);
    configureRegexEngineTimeout(sensorContext, REGEX_EXECUTION_TIMEOUT_KEY, RegexMatchingManager::setUninterruptibleTimeoutMs);

    var analyzer = new Analyzer(sensorContext, parallelizationManager, durationStatistics, activeChecks, notBinaryFilePredicate, analyzeAllFiles);
    durationStatistics.timed("analyzerTotal" + DurationStatistics.SUFFIX_GENERAL, () -> analyzer.analyzeFiles(inputFiles));
    durationStatistics.log();
    parallelizationManager.shutdown();
    RegexMatchingManager.shutdown();
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

  private FilePredicate constructFilePredicate(SensorContext sensorContext, FilePredicate notBinaryFilePredicate, boolean analyzeAllFiles) {
    if (analyzeAllFiles) {
      // if we're in a sonarlint context, we return this predicate as well
      LOG.debug("Analyzing all except non binary files");
      return notBinaryFilePredicate;
    }

    // if the property is inactive, we prevent jgit from being initialized
    if (!isJGitAndInclusionsActive(sensorContext)) {
      LOG.debug("Analyzing only language associated files, \"{}\" property is deactivated", INCLUSIONS_ACTIVATION_KEY);
      return LANGUAGE_FILE_PREDICATE;
    }

    var trackedByGitPredicate = durationStatistics.timed("trackedByGitPredicate" + DurationStatistics.SUFFIX_GENERAL,
      () -> new GitTrackedFilePredicate(getGitService()));
    if (!trackedByGitPredicate.isGitStatusSuccessful()) {
      LOG.debug("Analyzing only language associated files, " +
        "make sure to run the analysis inside a git repository to make use of inclusions specified via \"{}\"",
        TEXT_INCLUSIONS_KEY);
      return LANGUAGE_FILE_PREDICATE;
    }
    FilePredicates predicates = sensorContext.fileSystem().predicates();
    // Retrieve list of files to analyse using the right FilePredicate
    var pathPatternPredicate = includedPathPatternsFilePredicate(sensorContext);

    LOG.debug("Analyzing language associated files and files included via \"{}\" that are tracked by git", TEXT_INCLUSIONS_KEY);
    return predicates.and(
      predicates.or(LANGUAGE_FILE_PREDICATE, pathPatternPredicate),
      trackedByGitPredicate);
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

  private static boolean isActive(SensorContext sensorContext) {
    return sensorContext.config().getBoolean(ANALYZER_ACTIVATION_KEY).orElse(ANALYZER_ACTIVATION_DEFAULT_VALUE);
  }

  private static boolean isJGitAndInclusionsActive(SensorContext sensorContext) {
    return sensorContext.config().getBoolean(INCLUSIONS_ACTIVATION_KEY).orElse(INCLUSIONS_ACTIVATION_DEFAULT_VALUE);
  }

  private static boolean isSonarLintContext(SensorContext sensorContext) {
    return sensorContext.runtime().getProduct() == SonarProduct.SONARLINT;
  }

  private static boolean isSonarCloudContext(SensorContext sensorContext) {
    return !isSonarLintContext(sensorContext) && sensorContext.runtime().getEdition() == SonarEdition.SONARCLOUD;
  }

  private static boolean analyzeAllFiles(SensorContext sensorContext) {
    return "true".equals(sensorContext.config().get(ANALYZE_ALL_FILES_KEY).orElse("false"));
  }

  private static String sonarTests(SensorContext sensorContext) {
    return sensorContext.config().get(SONAR_TESTS_KEY).orElse("");
  }

  /**
   * In SonarLint context we want to analyze all non-binary input files, even when they are not analyzed or assigned to a language.
   * To avoid analyzing all non-binary files to reduce time and memory consumption in a non SonarLint context only files assigned to a
   * language OR file with a text extension are analyzed.
   */
  private static List<InputFile> getInputFiles(SensorContext sensorContext, FilePredicate filePredicate) {
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

    List<Class<?>> secretChecks = new SecretsCheckList().checks();
    checks.addAll(checkFactory.<Check>create(SecretsRulesDefinition.REPOSITORY_KEY)
      .addAnnotatedChecks(secretChecks).all());
    return checks;
  }

  protected void initializeSpecificationBasedChecks(List<Check> checks, SensorContext sensorContext) {
    var specificationLoader = durationStatistics.timed("deserializingSpecifications" + DurationStatistics.SUFFIX_GENERAL,
      this::constructSpecificationLoader);

    var specificationConfiguration = new SpecificationConfiguration(sonarTests(sensorContext));
    durationStatistics.timed("initializingSecretMatchers" + DurationStatistics.SUFFIX_GENERAL, () -> {
      for (Check activeCheck : checks) {
        if (activeCheck instanceof SpecificationBasedCheck specificationBasedCheck) {
          (specificationBasedCheck).initialize(specificationLoader, durationStatistics, specificationConfiguration);
        }
      }
    });
  }

  private static void configureRegexEngineTimeout(SensorContext sensorContext, String key, Consumer<Integer> setTimeoutMs) {
    try {
      Optional<Integer> valueAsInt = sensorContext.config().getInt(key);
      valueAsInt.ifPresent(setTimeoutMs);
    } catch (IllegalStateException e) {
      // provided value not in the expected format - do nothing
      LOG.debug("Provided value with key \"{}\" is not parseable as an integer", key, e);
    }
  }

  private void logWarningConsoleUI(String message) {
    LOG.warn(message);
    analysisWarnings.addWarning(message);
  }

  protected SpecificationLoader constructSpecificationLoader() {
    return new SpecificationLoader();
  }

  public GitService getGitService() {
    return new GitService();
  }
}
