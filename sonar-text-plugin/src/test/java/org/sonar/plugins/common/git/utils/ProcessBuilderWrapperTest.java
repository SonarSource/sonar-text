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
package org.sonar.plugins.common.git.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessBuilderWrapperTest {
  private static final List<String> SLEEP_COMMAND = List.of("sleep", "1000");

  @Test
  void shouldReturnFailureIfProcessHangs() throws IOException, InterruptedException {
    ProcessBuilderWrapper wrapper = spy(new ProcessBuilderWrapper());
    var process = mock(Process.class);
    doReturn(process).when(wrapper).startProcess(any());
    when(process.waitFor(anyLong(), any())).thenReturn(false);
    when(process.getInputStream()).thenAnswer(invocation -> {
      Thread.sleep(1000);
      return new ByteArrayInputStream(new byte[0]);
    });

    var status = wrapper.execute(SLEEP_COMMAND, (String line) -> {
    });

    assertEquals(ProcessBuilderWrapper.Status.FAILURE, status);
    verify(process).destroy();
  }

  @Test
  void shouldReturnFailureIfProcessReturnsNonZero() throws IOException, InterruptedException {
    var wrapper = spy(new ProcessBuilderWrapper());
    var process = mock(Process.class);
    doReturn(process).when(wrapper).startProcess(any());
    when(process.waitFor(anyLong(), any())).thenReturn(true);
    when(process.exitValue()).thenReturn(1);
    when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

    var status = wrapper.execute(SLEEP_COMMAND, (String line) -> {
    });

    assertEquals(ProcessBuilderWrapper.Status.FAILURE, status);
  }

  @Test
  void shouldReturnFailureIfProcessCrashes() throws IOException, InterruptedException {
    var wrapper = spy(new ProcessBuilderWrapper());
    var process = mock(Process.class);
    doReturn(process).when(wrapper).startProcess(any());
    when(process.waitFor(anyLong(), any())).thenThrow(new InterruptedException());
    when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

    var status = wrapper.execute(SLEEP_COMMAND, (String line) -> {
    });

    assertEquals(ProcessBuilderWrapper.Status.FAILURE, status);
  }
}
