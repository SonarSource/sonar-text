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
using System;
using System.Collections.Generic;

namespace SonarLint.Secrets.DotNet.UnitTests.DotNetOnly
{
    [TestClass]
    public class ConditionalMatcherTest_NoJavaTests
    {
        [TestMethod]
        public void Find_PredicateIsFalse_ReturnsEmptyList()
        {
            var predicate = new Moq.Mock<Predicate<string>>();
            predicate.Setup(x => x.Invoke("expected content")).Returns(false);
            var secretsMatcher = new Moq.Mock<ISecretsMatcher>();
            var testSubject = new ConditionalMatcher(predicate.Object, secretsMatcher.Object);

            var actual = testSubject.FindIn("expected content");

            actual.Should().BeEmpty();
            predicate.VerifyAll();
            secretsMatcher.Invocations.Count.Should().Be(0);
        }

        public void Find_PredicateIsTrue_MatcherIsCalled()
        {
            var expectedMatches = new List<Match> { new Match("any", 1, 2) };

            var predicate = new Moq.Mock<Predicate<string>>();
            predicate.Setup(x => x.Invoke("expected content")).Returns(true);

            var secretsMatcher = new Moq.Mock<ISecretsMatcher>();
            secretsMatcher.Setup(x => x.FindIn("expected content")).Returns(expectedMatches);

            var testSubject = new ConditionalMatcher(predicate.Object, secretsMatcher.Object);

            var actual = testSubject.FindIn("expected content");

            actual.Count.Should().Be(1);
            actual.Should().BeSameAs(expectedMatches);
            predicate.VerifyAll();
            secretsMatcher.VerifyAll();
        }
    }
}
