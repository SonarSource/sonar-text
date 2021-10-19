/*
 * Copyright (C) 2021 SonarSource SA
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
            Action act = () => new Secret(-1, 1);
            act.Should().ThrowExactly<ArgumentOutOfRangeException>().And.ParamName.Should().Be("startIndex");
        }

        [TestMethod]
        public void Ctor_InvalidLength()
        {
            Action act = () => new Secret(1, 0);
            act.Should().ThrowExactly<ArgumentOutOfRangeException>().And.ParamName.Should().Be("length");
        }

        [TestMethod]
        [DataRow(0, 1)]
        [DataRow(1, 99)]
        public void Ctor_ValidValues_PropertiesSet(int startIndex, int length)
        {
            var testSubject = new Secret(startIndex, length);

            testSubject.StartIndex.Should().Be(startIndex);
            testSubject.Length.Should().Be(length);
        }
    }
}
