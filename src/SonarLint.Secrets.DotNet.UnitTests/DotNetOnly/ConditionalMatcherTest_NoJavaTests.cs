/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
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
            var secretsMatcher = new Moq.Mock<SecretsMatcher>();
            var testSubject = new ConditionalMatcher(predicate.Object, secretsMatcher.Object);

            var actual = testSubject.findIn("expected content");

            actual.Should().BeEmpty();
            predicate.VerifyAll();
            secretsMatcher.Invocations.Count.Should().Be(0);
        }

        public void Find_PredicateIsTrue_MatcherIsCalled()
        {
            var expectedMatches = new List<Match> { new Match("any", 1, 2) };

            var predicate = new Moq.Mock<Predicate<string>>();
            predicate.Setup(x => x.Invoke("expected content")).Returns(true);

            var secretsMatcher = new Moq.Mock<SecretsMatcher>();
            secretsMatcher.Setup(x => x.findIn("expected content")).Returns(expectedMatches);

            var testSubject = new ConditionalMatcher(predicate.Object, secretsMatcher.Object);

            var actual = testSubject.findIn("expected content");

            actual.Count.Should().Be(1);
            actual.Should().BeSameAs(expectedMatches);
            predicate.VerifyAll();
            secretsMatcher.VerifyAll();
        }
    }
}
