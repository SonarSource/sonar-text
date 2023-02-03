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

            actual.Last().StartIndex.Should().Be(50);
            actual.Last().Length.Should().Be(6);
            actual.Last().Text.Should().Be("abcdef");
        }
    }
}
