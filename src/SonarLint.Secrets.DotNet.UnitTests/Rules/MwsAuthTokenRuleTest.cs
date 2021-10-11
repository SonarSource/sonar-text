﻿/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// Ported from ...\sonar-secrets-plugin\src\test\java\com\sonarsource\secrets\rules\MwsAuthTokenRuleTest.java

using FluentAssertions;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SonarLint.Secrets.DotNet.Rules;
using SonarLint.VisualStudio.Integration.UnitTests;
using System.Linq;
using static SonarLint.Secrets.DotNet.UnitTests.TestUtils;

namespace SonarLint.Secrets.DotNet.UnitTests.Rules
{
    [TestClass]
    public class MwsAuthTokenRuleTest
    {
        [TestMethod]
        public void MefCtor_CheckIsExported()
        {
            MefTestHelpers.CheckTypeCanBeImported<MwsAuthTokenRule, ISecretDetector>(null, null);
        }

        [TestMethod]
        public void testRuleProperties()
        {
            var testSubject = new MwsAuthTokenRule();

            testSubject.RuleKey.Should().Be("S6292");
            testSubject.Name.Should().Be("Amazon MWS credentials should not be disclosed");
        }

        [TestMethod]
        public void testRuleRegexPositive()
        {
            var testSubject = new MwsAuthTokenRule();

            var input = "export MWS_TOKEN=amzn.mws.4ea38b7b-f563-7709-4bae-12ba540c0ac5";

            var secrets = testSubject.Find(input);

            secrets.Count().Should().Be(1);
            CheckExpectedSecretFound(input, "amzn.mws.4ea38b7b-f563-7709-4bae-12ba540c0ac5", secrets.First());
            CrossCheckWithJavaResult(input, new(1, 17, 1, 62), secrets.First());
        }

        [TestMethod]
        public void testRuleRegexNegative()
        {
            var testSubject = new MwsAuthTokenRule();

            var secrets = testSubject.Find("export MWS_TOKEN=amz.mws.4ea38b7b-f563-7709-4bae-12ba540c0ac5");

            secrets.Should().BeEmpty();
        }
    }
}