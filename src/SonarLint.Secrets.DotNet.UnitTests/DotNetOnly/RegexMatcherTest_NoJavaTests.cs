/*
 * Copyright (C) 2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

using FluentAssertions;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SonarLint.Secrets.DotNet.Rules.Matching;
using System.Linq;

namespace SonarLint.Secrets.DotNet.UnitTests.DotNetOnly
{
    [TestClass]
    public class RegexMatcherTest_NoJavaTests
    {
        [TestMethod]
        public void Find_Simple()
        {
            const string PasswordPattern = "password=(?<pwd>\\w+)\\s";

            var testSubject = new RegexMatcher(PasswordPattern);

            const string input = @"password=123
  woijwoj w woijw e
  ffef  password=abcdef

";

            var actual = testSubject.FindIn(input);

            actual.Count().Should().Be(2);

            actual.First().StartIndex.Should().Be(9);
            actual.First().Length.Should().Be(3);
            actual.First().Text.Should().Be("123");

            actual.Last().StartIndex.Should().Be(52);
            actual.Last().Length.Should().Be(6);
            actual.Last().Text.Should().Be("abcdef");
        }
    }
}
