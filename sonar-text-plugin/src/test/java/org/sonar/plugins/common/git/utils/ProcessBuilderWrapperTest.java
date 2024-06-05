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
package org.sonar.plugins.common.git.utils;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class ProcessBuilderWrapperTest {
  @Test
  void shouldReturnFailureIfProcessHangs() throws IOException, InterruptedException {
    var wrapper = spy(new ProcessBuilderWrapper(List.of("sleep", "1000")));
    var process = mock(Process.class);
    when(wrapper.startProcess()).thenReturn(process);
    when(process.waitFor(anyLong(), any())).thenReturn(false);

    var status = wrapper.execute((String line) -> {
    });

    assertEquals(ProcessBuilderWrapper.Status.FAILURE, status);
  }

  @Test
  void shouldReturnFailureIfProcessReturnsNonZero() throws IOException {
    var wrapper = spy(new ProcessBuilderWrapper(List.of("sleep", "1000")));
    var process = mock(Process.class);
    when(wrapper.startProcess()).thenReturn(process);
    when(process.exitValue()).thenReturn(1);

    var status = wrapper.execute((String line) -> {
    });

    assertEquals(ProcessBuilderWrapper.Status.FAILURE, status);
  }

  @Test
  void shouldReturnFailureIfProcessCrashes() throws IOException, InterruptedException {
    var wrapper = spy(new ProcessBuilderWrapper(List.of("sleep", "1000")));
    var process = mock(Process.class);
    when(wrapper.startProcess()).thenReturn(process);
    when(process.waitFor(anyLong(), any())).thenThrow(new InterruptedException());

    var status = wrapper.execute((String line) -> {
    });

    assertEquals(ProcessBuilderWrapper.Status.FAILURE, status);
  }
}
