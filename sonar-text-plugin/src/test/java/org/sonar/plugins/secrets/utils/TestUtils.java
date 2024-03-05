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
package org.sonar.plugins.secrets.utils;

import java.util.function.Supplier;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.sonar.plugins.common.DurationStatistics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {
  public static DurationStatistics mockDurationStatistics() {
    var mock = mock(DurationStatistics.class);
    when(mock.timed(anyString(), any(Supplier.class)))
      .then(invocation -> invocation.getArgument(1, Supplier.class).get());
    Mockito.doAnswer((Answer<Void>) invocation -> {
      invocation.getArgument(1, Runnable.class).run();
      return null;
    }).when(mock).timed(anyString(), any(Runnable.class));
    return mock;
  }
}
