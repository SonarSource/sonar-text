/*
 * Copyright (C) 2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
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
