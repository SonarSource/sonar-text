/*
 * Copyright (C) 2021-2023 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

using FluentAssertions;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System;

namespace SonarLint.Secrets.DotNet.UnitTests.Helpers
{
    /// <summary>
    /// Tests that the assertion adapter methods work
    /// </summary>
    [TestClass]
    public class JavaAssertionTests
    {
        [TestMethod]
        public void AssertThat()
        {
            var input = new object();
            JavaAssertions.assertThat(input).Should().BeSameAs(input);
        }

        [TestMethod]
        public void IsTrue()
        {
            CheckDoesNotAssert(() => true.isTrue());
            CheckDoesAssert(() => false.isTrue());
        }

        [TestMethod]
        public void IsFalse()
        {
            CheckDoesNotAssert(() => false.isFalse());
            CheckDoesAssert(() => true.isFalse());
        }

        [TestMethod]
        public void IsLessThan()
        {
            CheckDoesNotAssert(() => 1.2d.isLessThan(1.3));
            CheckDoesAssert(() => 2.9d.isLessThan(1.1));
        }

        [TestMethod]
        public void IsEqualTo()
        {
            CheckDoesNotAssert(() => 1.2d.isEqualTo(1.2d));
            CheckDoesAssert(() => 2.9d.isEqualTo(9.9d));
        }

        private static void CheckDoesNotAssert(Action op) => op.Should().NotThrow();

        private static void CheckDoesAssert(Action op) => op.Should().ThrowExactly<AssertFailedException>();
    }
}
