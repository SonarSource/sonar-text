/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource SA
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
package org.sonar.plugins.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractCheckListTest {

  @Test
  protected void eachCheckShouldBeDeclaredInTheCheckList() throws IOException {
    int expectedCount = 0;
    for (var checksPackage : checksPackagePaths()) {
      try (Stream<Path> list = Files.walk(checksPackage)) {
        expectedCount += (int) list.filter(file -> file.toString().endsWith("Check.java")).count();
      }
    }
    List<Class<?>> checkClassList = checkClassList();
    assertThat(checkClassList).hasSize(expectedCount);
  }

  protected abstract Collection<Path> checksPackagePaths();

  protected abstract List<Class<?>> checkClassList();

}
