/*
 * SonarAnalyzer for Text
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

using FluentAssertions;

namespace SonarLint.Secrets.DotNet.UnitTests
{
    /// <summary>
    /// Adapter that provides an API that looks like the one used in the Java tests.
    /// </summary>
    /// <remarks>Not strictly necessary, but it makes it easier to compare the Java and .NET tests</remarks>
    internal static class JavaAssertions
    {
        public static T assertThat<T>(T input) => input; // no-op

        public static void isTrue(this bool input) => input.Should().BeTrue();

        public static void isFalse(this bool input) => input.Should().BeFalse();

        public static void isEqualTo(this double input, double expected) => input.Should().Be(expected);

        public static void isLessThan(this double input, double expected) => input.Should().BeLessThan(expected);
    }
}
