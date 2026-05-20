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
package org.sonar.plugins.secrets.api.filters;

/**
 * A post-filter that evaluates a candidate secret string and returns a {@link FilterOutcome} indicating whether the
 * candidate passed and which filters, if any, were skipped.
 *
 * <p>Implementations may emit a rejection log line using the provided {@link RejectionLogContext}; the convenience
 * {@link #apply(String)} overload uses {@link RejectionLogContext#NONE}, which suppresses logging and is appropriate
 * for unit tests and other call sites without rule/file context.
 */
@FunctionalInterface
public interface PostFilter {

  FilterOutcome apply(String candidateSecret, RejectionLogContext context);

  default FilterOutcome apply(String candidateSecret) {
    return apply(candidateSecret, RejectionLogContext.NONE);
  }

  PostFilter ACCEPT_ALL = (candidateSecret, context) -> FilterOutcome.ACCEPTED;
}
