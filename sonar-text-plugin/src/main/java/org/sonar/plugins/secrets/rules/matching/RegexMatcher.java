/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2022 SonarSource SA
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
package org.sonar.plugins.secrets.rules.matching;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexMatcher implements SecretsMatcher {
  private final Pattern pattern;

  public RegexMatcher(String stringPattern) {
    this.pattern = Pattern.compile(stringPattern);
  }

  @Override
  public List<Match> findIn(String content) {
    List<Match> matches = new ArrayList<>();
    Matcher matcher = pattern.matcher(content);
    while (matcher.find()) {
      MatchResult matchResult = matcher.toMatchResult();
      matches.add(new Match(matchResult.group(1), matchResult.start(1), matchResult.end(1)));
    }
    return matches;
  }
}
