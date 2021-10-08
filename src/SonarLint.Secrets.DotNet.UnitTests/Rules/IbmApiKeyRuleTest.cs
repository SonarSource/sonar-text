/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// Ported from ...\sonar-secrets-plugin\src\test\java\com\sonarsource\secrets\rules\IbmApiKeyRuleTest.java

using FluentAssertions;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SonarLint.Secrets.DotNet.Rules;
using SonarLint.VisualStudio.Integration.UnitTests;
using System.Linq;

namespace SonarLint.Secrets.DotNet.UnitTests.Rules
{
    [TestClass]
    public class IbmApiKeyRuleTest
    {
        [TestMethod]
        public void MefCtor_CheckIsExported()
        {
            MefTestHelpers.CheckTypeCanBeImported<IbmApiKeyRule, ISecretDetector>(null, null);
        }

        [TestMethod]
        public void testRuleProperties()
        {
            var testSubject = new IbmApiKeyRule();

            testSubject.RuleKey.Should().Be("S6337");
            testSubject.Name.Should().Be("IBM API keys should not be disclosed");
            testSubject.Message.Should().Be("Make sure this IBM API key is not disclosed.");
        }

        [TestMethod]
        public void testRuleRegexPositive()
        {
            var testSubject = new IbmApiKeyRule();

            var secrets = testSubject.Find("\"apikey\": \"iT5wxMGq2-ZJlMAHYoODl5EuTeCPvNRkSp1h3m99HWrc\"");

            secrets.Count().Should().Be(1);
            // The Java test checks for the tuple(1, 11, 1, 55) (start.line, start.lineOffset, end.line, end.lineOffset) 
            secrets.First().StartIndex.Should().Be(11);
            secrets.First().Length.Should().Be(44);
        }

        [TestMethod]
        public void testRuleRegexNegative()
        {
            var testSubject = new IbmApiKeyRule();

            var secrets = testSubject.Find("\"apikey\": \"iT5wxMGq2-ZJlMAHYoODl5EuTeCPvWrc\"");

            secrets.Should().BeEmpty();
        }

        [TestMethod]
        public void testRuleRegexNegativeLowEntropy()
        {
            var testSubject = new IbmApiKeyRule();

            var secrets = testSubject.Find("\"apikey\": \"01234567890123456789012345678901234567890123\"");

            secrets.Should().BeEmpty();
        }
    }
}
