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
package org.sonar.plugins.common.measures;

import java.util.function.Consumer;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;

/**
 * Wraps a delegate {@link FilePredicate} and runs a side effect whenever it matches an input file.
 * Used to collect telemetry (counts, detected categories/tools/vendors) in a single {@code fileSystem.inputFiles()}
 * pass instead of issuing one filesystem query per pattern.
 * Using a delegate circumvents any optimization the scanner is doing on the predicate, use with care!
 */
final class RecordingPredicate implements FilePredicate {

  private final FilePredicate delegate;
  private final Consumer<InputFile> onMatch;

  RecordingPredicate(FilePredicate delegate, Consumer<InputFile> onMatch) {
    this.delegate = delegate;
    this.onMatch = onMatch;
  }

  @Override
  public boolean apply(InputFile inputFile) {
    boolean matches = delegate.apply(inputFile);
    if (matches) {
      onMatch.accept(inputFile);
    }
    return matches;
  }
}
