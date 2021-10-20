/*
 * Copyright (C) 2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// Ported from ...\sonar-secrets-plugin\src\test\java\com\sonarsource\secrets\rules\AwsAccessKeyRuleTest.java

using FluentAssertions;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SonarLint.Secrets.DotNet.Rules;
using SonarLint.VisualStudio.Integration.UnitTests;
using System.Linq;
using static SonarLint.Secrets.DotNet.UnitTests.TestUtils;

namespace SonarLint.Secrets.DotNet.UnitTests.Rules
{
    [TestClass]
    public class AwsAccessKeyRuleTest
    {
        [TestMethod]
        public void MefCtor_CheckIsExported()
        {
            MefTestHelpers.CheckTypeCanBeImported<AwsAccessKeyRule, ISecretDetector>(null, null);
        }

        [TestMethod]
        public void TestRuleProperties()
        {
            var testSubject = new AwsAccessKeyRule();

            testSubject.RuleKey.Should().Be("S6290");
            testSubject.Name.Should().Be("Amazon Web Services credentials should not be disclosed");
        }

        [TestMethod]
        public void TestRuleFirstRegexPositive()
        {
            var testSubject = new AwsAccessKeyRule();

            var input = "var creds = new AWS.Credentials({ " +
              "     secretAccessKey: 'kHeUAwnSUizTWpSbyGAz4f+As5LshPIjvtpswqGb' " +
              "});";

            var secrets = testSubject.Find(input);

            secrets.Count().Should().Be(1);
            CheckExpectedSecretFound(input, "kHeUAwnSUizTWpSbyGAz4f+As5LshPIjvtpswqGb", secrets.First());
            CrossCheckWithJavaResult(input, new(1, 57, 1, 97), secrets.First());
        }

        [TestMethod]
        public void TestRuleSecondRegexPositive()
        {
            var testSubject = new AwsAccessKeyRule();

            var input = "aws_secret_access_key=kHeUAwnSUizTWpSbyGAz4f+As5LshPIjvtpswqGb";
            var secrets = testSubject.Find(input);

            secrets.Count().Should().Be(1);
            CheckExpectedSecretFound(input, "kHeUAwnSUizTWpSbyGAz4f+As5LshPIjvtpswqGb", secrets.First());
            CrossCheckWithJavaResult(input, new(1, 22, 1, 62), secrets.First());
        }

        [TestMethod]
        public void TestRuleRegexNegative()
        {
            var testSubject = new AwsAccessKeyRule();

            var secrets = testSubject.Find("public class Foo {\n"
              + "  public static readonly String KEY = \"AKIGKECZXA7AEIJLMQ\"\n"
              + "}");

            secrets.Should().BeEmpty();
        }
    }
}
