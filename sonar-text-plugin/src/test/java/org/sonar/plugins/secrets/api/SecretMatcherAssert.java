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

import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;
import org.assertj.core.api.AbstractAssert;

public class SecretMatcherAssert extends AbstractAssert<SecretMatcherAssert, SecretMatcher> {

  // these strings are chosen, so that the post filter predicate has different return values based on the configuration of the filter
  static Set<String> testStringsForPostFilter = Set.of(
    "candidate secret with low entropy",
    "candidate secret with low entropy and patternNot:EXAMPLEKEY",
    "candidate secret with high entropy: lasdij2338f,.q29cm2acasd has patternNot:EXAMPLEKEY",
    "candidate secret with high entropy: lasdij2338f,.q29cm2acasd",
    "C:\\Users\\User",
    "asecretstr/ng",
    "https://sonarsource.com",
    "nonsense://secretstring");
  static BiPredicate<Pattern, Pattern> patternEquals = (p1, p2) -> Objects.equals(p1.pattern(), p2.pattern());

  protected SecretMatcherAssert(SecretMatcher matcher) {
    super(matcher, SecretMatcherAssert.class);
  }

  public static SecretMatcherAssert assertThat(SecretMatcher actual) {
    return new SecretMatcherAssert(actual);
  }

  public SecretMatcherAssert behavesLike(SecretMatcher expectedMatcher) {
    isNotNull();
    usingRecursiveComparison()
      .ignoringFields("preFilter")
      .ignoringFields("postFilter")
      .withEqualsForType(patternEquals, Pattern.class)
      .isEqualTo(expectedMatcher);
    postFilterBehavesLike(expectedMatcher);
    return this;
  }

  public SecretMatcherAssert postFilterBehavesLike(SecretMatcher expectedMatcher) {
    for (String string : testStringsForPostFilter) {
      if (actual.getPostFilter().test(string) != expectedMatcher.getPostFilter().test(string)) {
        failWithMessage("Expected post filter to behave identical, but found different behavior on \"%s\"", string);
      }
    }
    return this;
  }
}
