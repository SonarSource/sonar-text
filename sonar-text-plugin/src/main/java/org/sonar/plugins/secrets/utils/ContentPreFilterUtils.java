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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.ahocorasick.trie.PayloadTrie;
import org.sonar.plugins.common.Check;
import org.sonar.plugins.secrets.api.SecretsSpecificationLoader;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.matching.filter.PreModule;

public final class ContentPreFilterUtils {
  private ContentPreFilterUtils() {
    // Utility class
  }

  public static <T extends Check> Map<String, Collection<T>> getChecksByContentPreFilters(Iterable<T> checks, SecretsSpecificationLoader specLoader) {
    Map<String, Collection<T>> checksByPreFilters = new HashMap<>();
    for (var check : checks) {
      var rules = specLoader.getRulesForKey(check.getRuleKey().rule());
      if (!hasOptimizableContentPrefilters(rules)) {
        continue;
      }
      for (var rule : rules) {
        var include = Optional.ofNullable(rule.getDetection().getPre())
          .map(PreModule::getInclude);
        if (include.isEmpty()) {
          continue;
        }
        var content = include.get().getContent();
        content.forEach(preFilter -> checksByPreFilters
          .computeIfAbsent(preFilter, k -> new HashSet<>())
          .add(check));
      }
    }
    return checksByPreFilters;
  }

  public static <T extends Check> PayloadTrie<Collection<T>> getPreprocessedTrie(Map<String, Collection<T>> checksByPreFilters) {
    var trieBuilder = PayloadTrie.<Collection<T>>builder();
    for (var entry : checksByPreFilters.entrySet()) {
      // Lower-case needed even if the trie should be case-insensitive, weird behavior in the library
      String preFilter = entry.getKey().toLowerCase(Locale.ROOT);
      Collection<T> checks = entry.getValue();
      trieBuilder.addKeyword(preFilter, checks);
    }
    return trieBuilder.ignoreCase().build();
  }

  /**
   * Checks if execution of content pre-filters can be optimized.<p/>
   * If not all rules within a check have content pre-filters, don't try to apply an optimization.
   * The check will be executed normally by the TextAndSecretsAnalyzer.<p/>
   * Note: we are deliberately not filtering individual rules here, because it will overcomplicate the logic without substantial gains.
   */
  public static boolean hasOptimizableContentPrefilters(Collection<Rule> rules) {
    return rules.stream().allMatch(ContentPreFilterUtils::hasIncludeContentPreFilter);
  }

  private static boolean hasIncludeContentPreFilter(Rule rule) {
    var pre = rule.getDetection().getPre();
    if (pre == null) {
      return false;
    }
    var include = pre.getInclude();
    if (include == null) {
      return false;
    }
    var content = include.getContent();
    return !content.isEmpty();
  }
}
