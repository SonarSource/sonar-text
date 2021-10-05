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
        public void Ctor_InvalidStartIndex()
        {
            Action act = () => new Secret(-1, 0);
            act.Should().ThrowExactly<ArgumentOutOfRangeException>().And.ParamName.Should().Be("startIndex");
        }

        [TestMethod]
        public void Ctor_InvalidEndIndex()
        {
            Action act = () => new Secret(0, -1);
            act.Should().ThrowExactly<ArgumentOutOfRangeException>().And.ParamName.Should().Be("endIndex");
        }

        [TestMethod]
        [DataRow(0, 1)]
        [DataRow(1, 0)]
        public void Ctor_ValidValues_PropertiesSet(int startIndex, int endIndex)
        {
            var testSubject = new Secret(startIndex, endIndex);

            testSubject.StartIndex.Should().Be(startIndex);
            testSubject.EndIndex.Should().Be(endIndex);
        }
    }
}
