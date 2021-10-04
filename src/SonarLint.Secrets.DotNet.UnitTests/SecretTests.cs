/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

using FluentAssertions;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System;

namespace SonarLint.Secrets.DotNet.UnitTests
{
    [TestClass]
    public class SecretTests
    {
        [TestMethod]
        public void Ctor_InvalidRuleKey()
        {
            Action act = () => new Secret(null, 1, 1);
            act.Should().ThrowExactly<ArgumentNullException>().And.ParamName.Should().Be("ruleKey");
        }

        [TestMethod]
        public void Ctor_InvalidStartIndex()
        {
            Action act = () => new Secret("any", -1, 0);
            act.Should().ThrowExactly<ArgumentOutOfRangeException>().And.ParamName.Should().Be("startIndex");
        }

        [TestMethod]
        public void Ctor_InvalidEndIndex()
        {
            Action act = () => new Secret("any", 0, -1);
            act.Should().ThrowExactly<ArgumentOutOfRangeException>().And.ParamName.Should().Be("endIndex");
        }

        [TestMethod]
        [DataRow("any", 0, 1)]
        [DataRow("rule1", 1, 0)]
        public void Ctor_ValidValues_PropertiesSet(string ruleKey, int startIndex, int endIndex)
        {
            var testSubject = new Secret(ruleKey, startIndex, endIndex);

            testSubject.RuleKey.Should().Be(ruleKey);
            testSubject.StartIndex.Should().Be(startIndex);
            testSubject.EndIndex.Should().Be(endIndex);
        }
    }
}
