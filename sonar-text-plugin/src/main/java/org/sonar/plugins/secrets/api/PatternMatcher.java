/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2023 SonarSource SA
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
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.plugins.secrets.configuration.model.matching.Matching;

public class PatternMatcher {
  private final Pattern pattern;

  PatternMatcher(@Nullable String stringPattern) {
    if (stringPattern != null) {
      this.pattern = Pattern.compile(stringPattern);
    } else {
      this.pattern = null;
    }
  }

  public static PatternMatcher build(@Nullable Matching matching) {
    if (matching != null) {
      return new PatternMatcher(matching.getPattern());
    }
    return new PatternMatcher(null);
  }

  public List<Match> findIn(String content) {
    if (pattern == null) {
      return Collections.emptyList();
    }
    List<Match> matches = new ArrayList<>();
    Matcher matcher = pattern.matcher(content);
    while (matcher.find()) {
      MatchResult matchResult = matcher.toMatchResult();
      matches.add(new Match(matchResult.group(1), matchResult.start(1), matchResult.end(1)));
    }
    return matches;
  }
}
