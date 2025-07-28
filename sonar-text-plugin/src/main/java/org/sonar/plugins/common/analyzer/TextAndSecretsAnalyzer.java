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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.ahocorasick.trie.PayloadTrie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.common.TextAndSecretsSensor;
import org.sonar.plugins.common.measures.DurationStatistics;
import org.sonar.plugins.common.measures.MemoryMonitor;
import org.sonar.plugins.common.measures.TelemetryReporter;
import org.sonar.plugins.common.thread.ParallelizationManager;
import org.sonar.plugins.secrets.AbstractBinaryFileCheck;
import org.sonar.plugins.secrets.api.SecretsSpecificationLoader;

import static java.util.stream.Collectors.toSet;
import static org.sonar.plugins.secrets.utils.ContentPreFilterUtils.getChecksByContentPreFilters;
import static org.sonar.plugins.secrets.utils.ContentPreFilterUtils.getPreprocessedTrie;

public final class TextAndSecretsAnalyzer extends Analyzer {
  private static final Logger LOG = LoggerFactory.getLogger(TextAndSecretsAnalyzer.class);
  private static final String ANALYSIS_NAME = "text and secrets analysis";
  private final Set<Check> checksWithoutContentPreFilter;

  /**
   * A Trie that represents all content pre-filters and associated checks.<br/>
  * Implementation of Trie and `Trie#parseText` seems thread-safe, so we can have a single instance for the whole analyzer.
  * Some insights: <a href="https://github.com/robert-bor/aho-corasick/issues/74">issue on GitHub</a>,
  * <a href="https://github.com/robert-bor/aho-corasick/blob/e68720d0afd51093fbba1232edf154ad71a62ac3/src/test/java/org/ahocorasick/trie/TrieTest.java#L550">a test</a>
   * covering this case.
  * Moreover, building the trie is expensive compared to matching on small files (at least 35x was spotted in tests),
   * so we really want to reuse it.
   */
  private final PayloadTrie<Collection<Check>> trie;

  public TextAndSecretsAnalyzer(
    SensorContext sensorContext,
    ParallelizationManager parallelizationManager,
    DurationStatistics durationStatistics,
    List<Check> suitableChecks,
    TelemetryReporter telemetryReporter,
    MemoryMonitor memoryMonitor,
    SecretsSpecificationLoader specLoader) {
    super(sensorContext, parallelizationManager, durationStatistics, suitableChecks, ANALYSIS_NAME, telemetryReporter, memoryMonitor);

    var checksByContentPreFilters = getChecksByContentPreFilters(suitableChecks, specLoader);
    var checksWithContentPreFilter = checksByContentPreFilters.values().stream().flatMap(Collection::stream).collect(toSet());
    this.checksWithoutContentPreFilter = suitableChecks.stream().filter(check -> !checksWithContentPreFilter.contains(check)).collect(toSet());
    this.trie = durationStatistics.timed("trieBuild::general", () -> getPreprocessedTrie(checksByContentPreFilters));
  }

  @Override
  protected boolean shouldAnalyzeFile(InputFileContext inputFileContext) {
    return containsNoBinaryCharacters(inputFileContext);
  }

  @Override
  protected void analyzeAllChecks(InputFileContext inputFileContext) {
    // Currently not possible and desired to parallelize check execution per file, as we rely on the sequential and always same order of the
    // checks to achieve deterministic analysis results
    // The main reason is because of the calculation of overlapping reported secrets in InputFileContext
    try {
      var emits = durationStatistics.timed("trieMatch::general", () -> trie.parseText(inputFileContext.content()));
      emits.stream()
        .flatMap(emit -> emit.getPayload().stream())
        // Avoid running the same check multiple times, as it emits for every match.
        // We still need to process the entire text to make sure we don't miss any matches, so one option for optimization here
        // would be to remove fully matched branches from the trie, but it would require patching the library and careful measurements.
        .distinct()
        .forEach(check -> check.analyze(inputFileContext));
      checksWithoutContentPreFilter.forEach(check -> check.analyze(inputFileContext));

      inputFileContext.flushIssues();
    } catch (RuntimeException e) {
      logAnalysisError(inputFileContext.getInputFile(), e);
    }
  }

  /**
   * We suppose here that all provided files have been whitelisted, so we don't expected binary files.
   * In case it still happen, we don't add the extension to the blacklist as we consider it to be an exception.
   */
  private static boolean containsNoBinaryCharacters(InputFileContext inputFileContext) {
    boolean hasNonTextCharacters = inputFileContext.hasNonTextCharacters();
    if (hasNonTextCharacters) {
      LOG.warn("The file '{}' contains binary data and will not be included in the text and secrets analysis.", inputFileContext.getInputFile());
      if (inputFileContext.getInputFile().language() != null) {
        LOG.warn("Please check this file and/or exclude it from the analysis with sonar.exclusions property.");
      } else {
        LOG.warn("Please check this file and/or remove the extension from the '{}' property.", TextAndSecretsSensor.TEXT_INCLUSIONS_KEY);
      }
    }
    return !hasNonTextCharacters;
  }

  public static List<Check> filterSuitableChecks(Collection<Check> checks) {
    return checks.stream()
      .filter(check -> !(check instanceof AbstractBinaryFileCheck))
      .toList();
  }
}
