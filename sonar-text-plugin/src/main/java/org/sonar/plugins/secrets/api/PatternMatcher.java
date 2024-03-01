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
package org.sonar.plugins.secrets.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.secrets.api.task.InterruptibleCharSequence;
import org.sonar.plugins.secrets.api.task.RegexMatchingManager;
import org.sonar.plugins.secrets.configuration.model.matching.AuxiliaryPattern;
import org.sonar.plugins.secrets.configuration.model.matching.Matching;

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
   * Builds a {@link PatternMatcher} from a {@link AuxiliaryPattern} object.
   * @param auxiliaryPattern input {@link AuxiliaryPattern}
   * @return Constructed {@link PatternMatcher}
   */
  public static PatternMatcher build(AuxiliaryPattern auxiliaryPattern) {
    return new PatternMatcher(auxiliaryPattern.getPattern());
  }

  /**
   * Returns a list of {@link Match matches} which are detected using the {@link Pattern pattern} field.
   * @param content to be matched on
   * @param ruleId of the {@link org.sonar.plugins.secrets.configuration.model.Rule}, for logging purposes.
   * @return list of {@link Match matches} detected in the content
   */
  public List<Match> findIn(String content, String ruleId) {
    if (pattern == null) {
      return Collections.emptyList();
    }
    List<Match> matches = new ArrayList<>();
    var matcher = pattern.matcher(new InterruptibleCharSequence(content));

    boolean executedSuccessfully = RegexMatchingManager.runRegexMatchingWithTimeout(() -> {
      while (matcher.find()) {
        var matchResult = matcher.toMatchResult();
        if (matcher.groupCount() == 0) {
          matches.add(new Match(matchResult.group(), matchResult.start(), matchResult.end()));
        } else {
          matches.add(new Match(matchResult.group(1), matchResult.start(1), matchResult.end(1)));
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
