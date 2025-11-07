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
package org.sonar.plugins.secrets.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.ahocorasick.trie.PayloadEmit;
import org.ahocorasick.trie.PayloadTrie;
import org.ahocorasick.trie.handler.PayloadEmitHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.common.InputFileContext;
import org.sonar.plugins.common.analyzer.TextAndSecretsAnalyzer;
import org.sonar.plugins.common.measures.DurationStatistics;
import org.sonar.plugins.secrets.api.SecretsSpecificationLoader;
import org.sonar.plugins.secrets.api.SpecificationBasedCheck;
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
  private Collection<? extends Check> checksWithoutPreFilter;
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

  public CheckContainer() {
    // Default constructor for DI framework.
  }

  public void initialize(
    Collection<Check> checks,
    SecretsSpecificationLoader specificationLoader,
    DurationStatistics durationStatistics) {
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
    this.checksWithoutPreFilter = Stream.concat(
      suitableChecks.stream().filter(not(SpecificationBasedCheck.class::isInstance)),
      specificationBasedChecks.stream().filter(not(checksWithPreFilter::contains)))
      .collect(toSet());
    this.trie = durationStatistics.timed("trieBuild::general", () -> getPreprocessedTrie(checksByPreFilters));
    this.durationStatistics = durationStatistics;
    this.initialized = true;

    LOG.debug("ChecksContainer initialized successfully with {} checks without pre-filter and trie containing {} patterns.",
      checksWithoutPreFilter.size(), checksByPreFilters.size());
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
    var handler = new SingleEmittingEmitHandler<Collection<SpecificationBasedCheck>>();
    durationStatistics.timed("trieMatch::general", () -> trie.parseText(inputFileContext.content(), handler));

    var emits = handler.getEmits();
    emits.stream()
      .flatMap(Collection::stream)
      .forEach(executeCheck);
    checksWithoutPreFilter.forEach(executeCheck);

    inputFileContext.flushIssues();
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
