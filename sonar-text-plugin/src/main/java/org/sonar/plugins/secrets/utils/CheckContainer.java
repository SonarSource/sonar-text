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
package org.sonar.plugins.secrets.utils;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.ahocorasick.trie.PayloadEmit;
import org.ahocorasick.trie.PayloadTrie;
import org.ahocorasick.trie.handler.PayloadEmitHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.common.analyzer.TextAndSecretsAnalyzer;
import org.sonar.plugins.common.measures.DurationStatistics;
import org.sonar.plugins.secrets.api.SecretsSpecificationLoader;
import org.sonar.plugins.secrets.api.SpecificationBasedCheck;
import org.sonar.plugins.secrets.api.filters.SkippedFilter;
import org.sonarsource.api.sonarlint.SonarLintSide;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;
import static org.sonar.plugins.secrets.utils.ContentPreFilterUtils.getChecksByContentPreFilters;
import static org.sonar.plugins.secrets.utils.ContentPreFilterUtils.getPreprocessedTrie;
import static org.sonarsource.api.sonarlint.SonarLintSide.MODULE;

/**
 * Container for checks and their associated trie, that requires one-time initialization.
 * This class improves performance in SonarLint by avoiding repeated expensive trie construction.
 * In the sonar-scanner, it acts only as a wrapper for checks and trie.
 */
@ScannerSide
@SonarLintSide(lifespan = MODULE)
public class CheckContainer {
  private static final Logger LOG = LoggerFactory.getLogger(CheckContainer.class);

  private DurationStatistics durationStatistics;
  private FilePredicate secretsSuffixExclusionPredicate;
  private Collection<? extends Check> specChecksWithoutPreFilter;
  private Collection<? extends Check> nonSecretChecks;
  /**
   * A Trie that represents all content pre-filters and associated checks.<br/>
   * Implementation of Trie and `Trie#parseText` seems thread-safe, so we can have a single instance for the whole analyzer.
   * Some insights: <a href="https://github.com/robert-bor/aho-corasick/issues/74">issue on GitHub</a>,
   * <a href="https://github.com/robert-bor/aho-corasick/blob/e68720d0afd51093fbba1232edf154ad71a62ac3/src/test/java/org/ahocorasick/trie/TrieTest.java#L550">a test</a>
   * covering this case.
   * Moreover, building the trie is expensive compared to matching on small files (at least 35x was spotted in tests),
   * so we really want to reuse it.
   */
  private PayloadTrie<Collection<SpecificationBasedCheck>> trie;
  private boolean initialized;
  private boolean automaticTestFileDetectionEnabled;
  private boolean testFilesFilterSkipped;
  private boolean collectSkippedPaths;
  private final AtomicInteger automaticTestFilesSkippedCount = new AtomicInteger(0);
  private final ConcurrentLinkedQueue<String> automaticTestFilesSkippedPaths = new ConcurrentLinkedQueue<>();

  public CheckContainer() {
    // Default constructor for DI framework.
  }

  public void initialize(
    Collection<Check> checks,
    SecretsSpecificationLoader specificationLoader,
    DurationStatistics durationStatistics,
    FilePredicate secretsExclusionPredicate) {
    initialize(checks, specificationLoader, durationStatistics, secretsExclusionPredicate, false, Set.of(), false);
  }

  public void initialize(
    Collection<Check> checks,
    SecretsSpecificationLoader specificationLoader,
    DurationStatistics durationStatistics,
    FilePredicate secretsExclusionPredicate,
    boolean automaticTestFileDetectionEnabled,
    Set<SkippedFilter> skippedFilters,
    boolean collectSkippedPaths) {
    // Re-apply config and reset per-analysis skipped tracking on every call (before the early-return guard), so a
    // reused instance — e.g. a SonarLint module re-analysis — starts each analysis clean. The CLI aggregates its
    // per-directory counts in SecretFinder, so resetting here does not lose cross-directory totals.
    this.automaticTestFileDetectionEnabled = automaticTestFileDetectionEnabled;
    this.testFilesFilterSkipped = skippedFilters.contains(SkippedFilter.TEST_FILES_FILTER);
    this.collectSkippedPaths = collectSkippedPaths;
    automaticTestFilesSkippedCount.set(0);
    automaticTestFilesSkippedPaths.clear();

    if (this.initialized) {
      LOG.debug("ChecksContainer is already initialized, skipping re-initialization.");
      return;
    }

    LOG.debug("Initializing ChecksContainer with checks and trie construction.");

    var suitableChecks = TextAndSecretsAnalyzer.filterSuitableChecks(checks);

    var specificationBasedChecks = suitableChecks.stream()
      .filter(SpecificationBasedCheck.class::isInstance)
      .map(SpecificationBasedCheck.class::cast)
      .collect(toSet());
    var checksByPreFilters = getChecksByContentPreFilters(specificationBasedChecks, specificationLoader);
    var checksWithPreFilter = checksByPreFilters.values().stream().flatMap(Collection::stream).collect(toSet());
    this.specChecksWithoutPreFilter = specificationBasedChecks.stream().filter(not(checksWithPreFilter::contains)).collect(toSet());

    this.nonSecretChecks = suitableChecks.stream()
      .filter(not(SpecificationBasedCheck.class::isInstance))
      .collect(toSet());

    this.trie = durationStatistics.timed("trieBuild::general", () -> getPreprocessedTrie(checksByPreFilters));
    this.durationStatistics = durationStatistics;
    this.secretsSuffixExclusionPredicate = secretsExclusionPredicate;
    this.initialized = true;

    LOG.debug("ChecksContainer initialized successfully with {} checks without pre-filter and trie containing {} patterns.",
      specChecksWithoutPreFilter.size(), checksByPreFilters.size());
  }

  public void analyze(InputFileContext inputFileContext) {
    analyze(inputFileContext, check -> check.analyze(inputFileContext));
  }

  // For testing purposes, allows to analyze a specific rule.
  public void analyze(InputFileContext inputFileContext, String ruleId) {
    analyze(inputFileContext, (Check check) -> {
      if (check instanceof SpecificationBasedCheck specificationBasedCheck) {
        specificationBasedCheck.analyze(inputFileContext, ruleId);
      } else {
        check.analyze(inputFileContext);
      }
    });
  }

  private void analyze(InputFileContext inputFileContext, Consumer<Check> executeCheck) {
    validateInitialized();

    // The test-file classification was computed once per file at InputFileContext construction and cached there. The
    // analyzer only runs the heuristic when detection is enabled (see Analyzer#buildInputFileContext), so when it is
    // disabled the cached value is false; reading it here yields the same value every rule's pre-filter sees.
    var skipSecretChecks = automaticTestFileDetectionEnabled && !testFilesFilterSkipped && inputFileContext.isAutomaticallyDetectedTestFile();

    if (skipSecretChecks) {
      automaticTestFilesSkippedCount.incrementAndGet();
      // Only retain the path list when a downstream caller asked for it (e.g. CLI --show-skipped-files). For large
      // monorepos with automatic detection active this saves a String per skipped file otherwise never read.
      // Store the relative path (not just the filename) so the listing is useful for locating files in the logs.
      if (collectSkippedPaths) {
        automaticTestFilesSkippedPaths.add(relativePathOf(inputFileContext));
      }
    }

    // secret checks will not be analyzed on files with excluded secrets suffixes,
    // nor on files that the automatic test-file filter rejected (unless the filter is configured as skipped).
    if (!skipSecretChecks && secretsSuffixExclusionPredicate.apply(inputFileContext.getInputFile())) {
      var handler = new SingleEmittingEmitHandler<Collection<SpecificationBasedCheck>>();
      durationStatistics.timed("trieMatch::general", () -> trie.parseText(inputFileContext.content(), handler));

      var emits = handler.getEmits();
      emits.stream()
        .flatMap(Collection::stream)
        .forEach(executeCheck);
      specChecksWithoutPreFilter.forEach(executeCheck);
    }
    nonSecretChecks.forEach(executeCheck);

    inputFileContext.flushIssues();
  }

  /**
   * Path of the file relative to the analysis base directory, with forward-slash separators, for the skipped-file
   * listing. Derived from the file URI rather than the deprecated {@code InputFile#relativePath()}, mirroring the
   * relativization already done in {@code AutomaticTestFileFilter}.
   */
  private static String relativePathOf(InputFileContext inputFileContext) {
    var path = Path.of(inputFileContext.getInputFile().uri());
    var baseDir = inputFileContext.getFileSystem().baseDir().toPath();
    try {
      return baseDir.relativize(path).normalize().toString().replace('\\', '/');
    } catch (IllegalArgumentException e) {
      // Cross-root paths (e.g. a different filesystem root) can't be relativized; fall back to the absolute path.
      return path.toString().replace('\\', '/');
    }
  }

  /**
   * Number of files skipped from secret analysis because the automatic test-file filter classified them as test files
   * during the most recent analysis. Always {@code 0} when the filter is disabled or skipped via configuration. Reset
   * at the start of every {@link #initialize(Collection, SecretsSpecificationLoader, DurationStatistics, FilePredicate,
   * boolean, Set, boolean)} call (before the early-return guard), so it reflects a single analysis. The CLI calls
   * {@code initialize()} once per directory argument and aggregates these per-directory counts in {@code SecretFinder}.
   */
  public int getAutomaticTestFilesSkippedCount() {
    return automaticTestFilesSkippedCount.get();
  }

  /**
   * Paths of the files skipped due to the automatic test-file filter during the most recent analysis (reset on every
   * {@code initialize()} call, like {@link #getAutomaticTestFilesSkippedCount()}). The order is the order files were
   * processed, which is non-deterministic under parallel analysis — callers that need a stable order should sort.
   * Populated only when the {@code collectSkippedPaths} flag was set at initialization time (e.g. by the CLI's
   * {@code --show-skipped-files} flag); empty otherwise — even when the filter has skipped files (use
   * {@link #getAutomaticTestFilesSkippedCount()} for the count without retaining paths).
   */
  public List<String> getAutomaticTestFilesSkippedPaths() {
    return List.copyOf(automaticTestFilesSkippedPaths);
  }

  /**
   * Master switch for the heuristic test-file detection: {@code true} when the project did not declare its own test
   * files (e.g. {@code sonar.tests} is unset), so the filename/path heuristic should run. The analyzer reads this to
   * decide whether to classify files at all — when {@code false}, the per-file classification is skipped entirely
   * since its result would never be read. Distinct from {@link #isAutomaticTestFileFilterActive()}, which additionally
   * accounts for the filter being skipped via configuration.
   */
  public boolean isAutomaticTestFileDetectionEnabled() {
    return automaticTestFileDetectionEnabled;
  }

  /**
   * Whether the automatic test-file filter is active (enabled and not configured to be skipped). Used by callers
   * deciding whether to surface the skipped-file count.
   */
  public boolean isAutomaticTestFileFilterActive() {
    return automaticTestFileDetectionEnabled && !testFilesFilterSkipped;
  }

  private void validateInitialized() {
    if (!initialized) {
      throw new IllegalStateException("ChecksContainer must be initialized before use. Call initialize() first.");
    }
  }

  boolean isInitialized() {
    return initialized;
  }

  private static class SingleEmittingEmitHandler<T> implements PayloadEmitHandler<T> {
    private final Set<T> emits = new HashSet<>();

    public Set<T> getEmits() {
      return emits;
    }

    @Override
    public boolean emit(PayloadEmit<T> emit) {
      // `emit` is called for every matched keyword, but we only care about whether the keyword has matched at all,
      // so we keep a single occurrence.
      return emits.add(emit.getPayload());
    }
  }
}
