/*
 * Copyright (C) 2021-2023 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// Ported from ...\sonar-secrets-plugin\src\test\java\com\sonarsource\secrets\rules\AlibabaCloudAccessKeyIDsRuleTest.java

using FluentAssertions;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SonarLint.Secrets.DotNet.Rules;
using SonarLint.VisualStudio.Integration.UnitTests;
using System.Linq;
using static SonarLint.Secrets.DotNet.UnitTests.TestUtils;

namespace SonarLint.Secrets.DotNet.UnitTests.Rules
{
    [TestClass]
    public class AlibabaCloudAccessKeyIDsRuleTest
    {
        [TestMethod]
        public void MefCtor_CheckIsExported()
        {
            MefTestHelpers.CheckTypeCanBeImported<AlibabaCloudAccessKeyIDsRule, ISecretDetector>(null, null);
        }

        [TestMethod]
        public void TestRuleProperties()
        {
            var testSubject = new AlibabaCloudAccessKeyIDsRule();

            testSubject.RuleKey.Should().Be("secrets:S6336");
            testSubject.Name.Should().Be("Alibaba Cloud AccessKeys should not be disclosed");
            testSubject.Message.Should().Be("Make sure this Alibaba Cloud Access Key ID is not disclosed.");
        }

        [TestMethod]
        public void TestRuleRegexPositive()
        {
            var testSubject = new AlibabaCloudAccessKeyIDsRule();

            var input = "LTAI5tBcc9SecYAomgyUSFs8";
            var secrets = testSubject.Find(input);

            secrets.Count().Should().Be(1);
            CheckExpectedSecretFound(input, "LTAI5tBcc9SecYAomgyUSFs8", secrets.First());
            CrossCheckWithJavaResult(input, new(1, 0, 1, 24), secrets.First());
        }

        [TestMethod]
        public void TestRuleRegexNegative()
        {
            var testSubject = new AlibabaCloudAccessKeyIDsRule();

            var secrets = testSubject.Find("LNTTAI5tBcc9SecYAomgyUSFs8");

            secrets.Should().BeEmpty();
        }
    }
}
