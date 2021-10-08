/*
 * Copyright (C) 2018-2021 SonarSource SA
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

            var secrets = testSubject.Find("android:value=\"AIzaSyCis4NzxMw1aJyvUIrjGILjPkSdxrRfof4\"");

            secrets.Count().Should().Be(1);

            // The Java test checks for the tuple(1, 15, 1, 54) (start.line, start.lineOffset, end.line, end.lineOffset) 
            secrets.First().StartIndex.Should().Be(15);
            secrets.First().Length.Should().Be(39);
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
