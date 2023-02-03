﻿/*
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
