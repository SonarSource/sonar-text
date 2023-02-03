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
