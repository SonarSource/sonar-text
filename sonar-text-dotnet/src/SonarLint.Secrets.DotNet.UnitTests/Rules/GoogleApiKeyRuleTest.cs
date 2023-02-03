/*
 * SonarAnalyzer for Text
 * Copyright (C) 2021-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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

            testSubject.RuleKey.Should().Be("secrets:S6334");
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
