﻿/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// Ported from ...\sonar-secrets-plugin\src\test\java\com\sonarsource\secrets\rules\AwsAccessKeyIdRuleTest.java

using FluentAssertions;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SonarLint.Secrets.DotNet.Rules;
using SonarLint.VisualStudio.Integration.UnitTests;
using System.Linq;
using static SonarLint.Secrets.DotNet.UnitTests.TestUtils;

namespace SonarLint.Secrets.DotNet.UnitTests.Rules
{
    [TestClass]
    public class AwsAccessKeyIdRuleTest
    {
        [TestMethod]
        public void MefCtor_CheckIsExported()
        {
            MefTestHelpers.CheckTypeCanBeImported<AwsAccessKeyIdRule, ISecretDetector>(null, null);
        }

        [TestMethod]
        public void testRuleProperties()
        {
            var testSubject = new AwsAccessKeyIdRule();

            testSubject.RuleKey.Should().Be("S6290");
            testSubject.Name.Should().Be("Amazon Web Services credentials should not be disclosed");
        }

        [TestMethod]
        public void testRuleRegexPositive()
        {
            var testSubject = new AwsAccessKeyIdRule();

            var input = "public class Foo {\n"
              + "  public static final String KEY = \"AKIAIGKECZXA7AEIJLMQ\"\n"
              + "}";

            var secrets = testSubject.Find(input);

            secrets.Count().Should().Be(1);
            CheckExpectedSecretFound(input, "AKIAIGKECZXA7AEIJLMQ", secrets.First());
            CrossCheckWithJavaResult(input, new (2, 36, 2, 56), secrets.First());
        }

        [TestMethod]
        public void testRuleRegexNegative()
        {
            var testSubject = new AwsAccessKeyIdRule();

            var secrets = testSubject.Find("public class Foo {\n"
              + "  public static readonly String KEY = \"AKIGIGKECZXA7AEIJLMQ\"\n"
              + "}");

            secrets.Should().BeEmpty();
        }

        [TestMethod]
        public void testRuleRegexExamplePositive()
        {
            var testSubject = new AwsAccessKeyIdRule();

            var input = "public class Foo {\n"
              + "  public static final String KEY = \"AKIAIGKECZXA7EXAMPLF\"\n"
              + "}";

            var secrets = testSubject.Find(input);

            secrets.Count().Should().Be(1);
            CheckExpectedSecretFound(input, "AKIAIGKECZXA7EXAMPLF", secrets.First());
            CrossCheckWithJavaResult(input, new(2, 36, 2, 56), secrets.First());
        }

        [TestMethod]
        public void testRuleRegexExampleNegative()
        {
            var testSubject = new AwsAccessKeyIdRule();

            var secrets = testSubject.Find("public class Foo {\n"
              + "  public static readonly String KEY = \"AKIAIGKECZXA7EXAMPLE\"\n"
              + "}");

            secrets.Should().BeEmpty();
        }
    }
}
