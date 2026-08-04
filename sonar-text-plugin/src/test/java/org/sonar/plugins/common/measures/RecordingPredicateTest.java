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
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecordingPredicateTest {

  @SuppressWarnings("unchecked")
  private final Consumer<InputFile> onMatch = mock(Consumer.class);
  private final FilePredicate delegate = mock(FilePredicate.class);
  private final InputFile inputFile = mock(InputFile.class);

  @Test
  void shouldRunOnMatchAndReturnTrueWhenDelegateMatches() {
    when(delegate.apply(inputFile)).thenReturn(true);
    var recordingPredicate = new RecordingPredicate(delegate, onMatch);

    boolean result = recordingPredicate.apply(inputFile);

    assertThat(result).isTrue();
    verify(onMatch).accept(inputFile);
  }

  @Test
  void shouldNotRunOnMatchAndReturnFalseWhenDelegateRejects() {
    when(delegate.apply(inputFile)).thenReturn(false);
    var recordingPredicate = new RecordingPredicate(delegate, onMatch);

    boolean result = recordingPredicate.apply(inputFile);

    assertThat(result).isFalse();
    verify(onMatch, never()).accept(any());
  }
}
