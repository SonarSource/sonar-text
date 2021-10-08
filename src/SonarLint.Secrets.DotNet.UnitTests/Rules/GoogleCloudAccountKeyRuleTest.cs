/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// Ported from ...\sonar-secrets-plugin\src\test\java\com\sonarsource\secrets\rules\GoogleCloudAccountKeyRuleTest.java

using FluentAssertions;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SonarLint.Secrets.DotNet.Rules;
using SonarLint.VisualStudio.Integration.UnitTests;
using System.Linq;

using static SonarLint.Secrets.DotNet.UnitTests.JavaTestFileUtils;
namespace SonarLint.Secrets.DotNet.UnitTests.Rules
{
    [TestClass]
    public class GoogleCloudAccountKeyRuleTest
    {
        [TestMethod]
        public void MefCtor_CheckIsExported()
        {
            MefTestHelpers.CheckTypeCanBeImported<GoogleCloudAccountKeyRule, ISecretDetector>(null, null);
        }

        [TestMethod]
        public void TestRuleProperties()
        {
            var testSubject = new GoogleCloudAccountKeyRule();

            testSubject.RuleKey.Should().Be("S6335");
            testSubject.Name.Should().Be("Google Cloud service accounts keys should not be disclosed");
            testSubject.Message.Should().Be("Make sure this Google Cloud service account key is not disclosed.");
        }

        [TestMethod]
        public void TestRuleRegexPositive()
        {
            var testSubject = new GoogleCloudAccountKeyRule();

            var secrets = testSubject.Find(readFileAndNormalize("src/test/files/google-cloud-account-key/GoogleCloudAccountPositive.json", UTF_8));

            secrets.Count().Should().Be(1);
            secrets.First().StartIndex.Should().Be(153);
            // The Java test checks for the tuple(5, 18, 5, 1750) (start.line, start.lineOffset, end.line, end.lineOffset)
            secrets.First().Length.Should().Be(1750 - 18);
        }

        [TestMethod]
        public void TestRuleRegexNegative()
        {
            var testSubject = new GoogleCloudAccountKeyRule();

            var secrets = testSubject.Find(readFileAndNormalize("src/test/files/google-cloud-account-key/GoogleCloudAccountNegative.json", UTF_8));

            secrets.Should().BeEmpty();
        }
    }

}