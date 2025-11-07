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
package org.sonar.plugins.secrets.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.secrets.api.task.InterruptibleCharSequence;
import org.sonar.plugins.secrets.api.task.RegexMatchingManager;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPattern;
import org.sonar.plugins.secrets.configuration.model.matching.Matching;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

/**
 * Used to match regular expressions in Strings.
 */
public class PatternMatcher {
  private static final Logger LOG = LoggerFactory.getLogger(PatternMatcher.class);
  private final Pattern pattern;

  PatternMatcher(@Nullable String stringPattern) {
    if (stringPattern != null) {
      this.pattern = Pattern.compile(stringPattern);
    } else {
      this.pattern = null;
    }
  }

  /**
   * Builds a {@link PatternMatcher} from a {@link Matching} object.
   * @param matching input {@link Matching}
   * @return Constructed {@link PatternMatcher}
   */
  public static PatternMatcher build(@Nullable Matching matching) {
    if (matching != null) {
      return new PatternMatcher(matching.getPattern());
    }
    return new PatternMatcher(null);
  }

  /**
   * Builds a {@link PatternMatcher} from a pattern as a string.
   * @param pattern input pattern as a string
   * @return Constructed {@link PatternMatcher}
   */
  public static PatternMatcher build(String pattern) {
    return new PatternMatcher(pattern);
  }

  /**
   * Builds a {@link PatternMatcher} from a {@link AuxiliaryPattern} object.
   * @param auxiliaryPattern input {@link AuxiliaryPattern}
   * @return Constructed {@link PatternMatcher}
   */
  public static PatternMatcher build(AuxiliaryPattern auxiliaryPattern) {
    return new PatternMatcher(auxiliaryPattern.getPattern());
  }

  /**
   * Returns a list of {@link Match matches} which are detected using the {@link Pattern pattern} field.
   *
   * @param content     to be matched on
   * @param ruleId      of the {@link org.sonar.plugins.secrets.configuration.model.Rule}, for logging purposes.
   * @return list of {@link Match matches} detected in the content
   */
  public List<Match> findIn(String content, String ruleId) {
    return findIn(content, ruleId, emptyList());
  }

  /**
   * Returns a list of {@link Match matches} which are detected using the {@link Pattern pattern} field.
   *
   * @param content     to be matched on
   * @param ruleId      of the {@link org.sonar.plugins.secrets.configuration.model.Rule}, for logging purposes.
   * @param expectedNamedGroups set of names of named capturing groups to be returned in the {@link Match} object
   * @return list of {@link Match matches} detected in the content
   */
  public List<Match> findIn(String content, String ruleId, Collection<String> expectedNamedGroups) {
    if (pattern == null) {
      return emptyList();
    }
    List<Match> matches = new ArrayList<>();
    var matcher = pattern.matcher(new InterruptibleCharSequence(content));

    boolean executedSuccessfully = RegexMatchingManager.runRegexMatchingWithTimeout(() -> {
      while (matcher.find()) {
        var matchResult = matcher.toMatchResult();
        if (matcher.groupCount() == 0) {
          matches.add(new Match(matchResult.group(), matchResult.start(), matchResult.end(), emptyMap()));
        } else {
          var namedMatches = expectedNamedGroups.stream()
            .filter(it -> matcher.group(it) != null)
            .collect(toMap(Function.identity(), it -> new Match(matcher.group(it), matcher.start(it), matcher.end(it), emptyMap())));

          // convention for issue location: the first group takes precedence
          matches.add(new Match(matchResult.group(1), matchResult.start(1), matchResult.end(1), namedMatches));
        }
      }
    }, pattern.pattern(), ruleId);

    if (!executedSuccessfully) {
      String patternToDisplay = pattern.pattern().replace("\\", "\\\\");
      LOG.warn("Running pattern in rule with id \"{}\" on content of length {} has timed out after {}ms." +
        " Related pattern is \"{}\".",
        ruleId, content.length(), RegexMatchingManager.getTimeoutMs(), patternToDisplay);
    }
    return matches;
  }

}
