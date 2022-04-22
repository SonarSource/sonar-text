/*
 * Copyright (C) 2021-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// Ported from ...\sonar-secrets-plugin\src\test\java\com\sonarsource\secrets\rules\GoogleApiKeyRuleTest.java

using FluentAssertions;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SonarLint.Secrets.DotNet.Rules;
using SonarLint.VisualStudio.Integration.UnitTests;
using System.Collections.Generic;
using System.Linq;
using static SonarLint.Secrets.DotNet.UnitTests.TestUtils;

namespace SonarLint.Secrets.DotNet.UnitTests.Rules
{
    [TestClass]
    public class GoogleApiKeyRuleTest
    {
        [TestMethod]
        public void MefCtor_CheckIsExported()
        {
            MefTestHelpers.CheckTypeCanBeImported<GoogleApiKeyRule, ISecretDetector>(null, null);
        }

        [TestMethod]
        public void TestRuleProperties()
        {
            var testSubject = new GoogleApiKeyRule();

            testSubject.RuleKey.Should().Be("S6334");
            testSubject.Name.Should().Be("Google API keys should not be disclosed");
            testSubject.Message.Should().Be("Make sure this Google API Key is not disclosed.");
        }

        [TestMethod]
        public void TestRuleRegexPositive()
        {
            var testSubject = new GoogleApiKeyRule();

            var input = "android:value=\"AIzaSyCis4NzxMw1aJyvUIrjGILjPkSdxrRfof4\"";

            var secrets = testSubject.Find("android:value=\"AIzaSyCis4NzxMw1aJyvUIrjGILjPkSdxrRfof4\"");

            secrets.Count().Should().Be(1);
            CheckExpectedSecretFound(input, "AIzaSyCis4NzxMw1aJyvUIrjGILjPkSdxrRfof4", secrets.First());
            CrossCheckWithJavaResult(input, new(1, 15, 1, 54), secrets.First());
        }

        [TestMethod]
        public void TestRuleRegexNegative()
        {
            var testSubject = new GoogleApiKeyRule();

            IEnumerable<ISecret> secrets = testSubject.Find("android:value=\"AIzaSyCis4NzxMw1aJyvUIrjGfof4\"");

            secrets.Should().BeEmpty();
        }
    }
}
