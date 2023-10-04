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
package org.sonar.plugins.secrets.api.task;

public class InterruptibleCharSequence implements CharSequence {
  CharSequence wrappedSequence;

  public InterruptibleCharSequence(CharSequence wrappedSequence) {
    super();
    this.wrappedSequence = wrappedSequence;
  }

  // Should be wrapped in runtime exception as we want the thread to kill itself
  @SuppressWarnings("java:S112")
  public char charAt(int index) {
    if (Thread.interrupted()) {
      throw new RuntimeException(new InterruptedException());
    }
    return wrappedSequence.charAt(index);
  }

  public int length() {
    return wrappedSequence.length();
  }

  public CharSequence subSequence(int start, int end) {
    return new InterruptibleCharSequence(wrappedSequence.subSequence(start, end));
  }

  @Override
  public String toString() {
    return wrappedSequence.toString();
  }
}
