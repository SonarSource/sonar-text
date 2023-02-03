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
