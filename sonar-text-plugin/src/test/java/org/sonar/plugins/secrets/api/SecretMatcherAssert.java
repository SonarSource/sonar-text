/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
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
      .ignoringFields("durationStatistics")
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
