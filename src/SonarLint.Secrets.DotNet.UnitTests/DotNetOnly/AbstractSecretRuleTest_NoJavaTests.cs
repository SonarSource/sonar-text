/*
 * Copyright (C) 2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

using FluentAssertions;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SonarLint.Secrets.DotNet.Rules;
using SonarLint.Secrets.DotNet.Rules.Matching;
using System.Linq;

namespace SonarLint.Secrets.DotNet.UnitTests.DotNetOnly
{
    [TestClass]
    public class AbstractSecretRuleTest_NoJavaTests
    {
        [TestMethod]
        public void Ctor_PropertiesAreSet()
        {
            var testSubject = new ConcreteSecretRule("my key", "my name", "my message");

            testSubject.RuleKey.Should().Be("my key");
            testSubject.Name.Should().Be("my name");
            testSubject.Message.Should().Be("my message");
        }

        [TestMethod]
        public void Find_OverlappingMatchesAreDiscarded()
        {
            var testSubject = new ConcreteSecretRule("any", "any", "any",
                CreateMatcher(),
                CreateMatcher(
                    new Match("any", 1, 10), // 1. included
                    new Match("any", 1, 10)  // 2. duplicate of 1, ignored
                ),
                CreateMatcher(
                    new Match("any", 20, 2), // 3. unique, included
                    new Match("any", 1, 2)   // 4. overlaps 1, ignored
                ),
                CreateMatcher(
                    new Match("any", 21, 2),  // 5. overlaps 3, ignore, ignored
                    new Match("any", 40, 10)  // 6. unique, included
                ));

            var actual = testSubject.Find("any")
                .ToArray();

            actual.Length.Should().Be(3);

            actual[0].StartIndex.Should().Be(1);
            actual[0].Length.Should().Be(10);

            actual[1].StartIndex.Should().Be(20);
            actual[1].Length.Should().Be(2);

            actual[2].StartIndex.Should().Be(40);
            actual[2].Length.Should().Be(10);
        }

        [TestMethod]
        public void Find_PotentialFalsePositivesAreDiscarded()
        {
            var testSubject = new ConfigurableFalsePositivesTestRule(
                new string[] { "fp1", "fp2" }, // text values that should be treated as FPs

                // NB the text spans must be unique so they are not filtered out
                //    for being overlapping
                CreateMatcher(
                    new Match("true positive 1", 10, 1), // should be included
                    new Match("fp1", 20, 1)
                ),
                CreateMatcher(
                    new Match("fp1", 30, 1),
                    new Match("fp2", 40, 1),
                    new Match("true positive 2", 50, 1) // should be included
                ));

            var actual = testSubject.Find("any")
                .ToArray();

            actual.Length.Should().Be(2);

            actual[0].StartIndex.Should().Be(10);
            actual[0].Length.Should().Be(1);

            actual[1].StartIndex.Should().Be(50);
            actual[1].Length.Should().Be(1);
        }

        private static ISecretsMatcher CreateMatcher(params Match[] matchesToReturn)
        {
            var matcher = new Moq.Mock<ISecretsMatcher>();
            matcher.Setup(x => x.FindIn(Moq.It.IsAny<string>())).Returns(matchesToReturn.ToList());
            return matcher.Object;
        }

        // Concrete sub-class: doesn't change any of the base class functionality
        private class ConcreteSecretRule : AbstractSecretRule
        {
            public ConcreteSecretRule(string ruleKey, string name, string message, params ISecretsMatcher[] matchers)
                : base(ruleKey, name, message, matchers)
            { }
        }

        // Test rule that allows the set of "isPotentialFalsePositive" strings to be configured
        private class ConfigurableFalsePositivesTestRule : AbstractSecretRule
        {
            private readonly string[] falsePositives;

            public ConfigurableFalsePositivesTestRule(string[] falsePositiveText,
                params ISecretsMatcher[] matchers)
                : base("any", "any", "any", matchers)
            {
                falsePositives = falsePositiveText;
            }

            protected override bool IsProbablyFalsePositive(string matchedText) =>
                falsePositives.Contains(matchedText);
        }
    }
}
