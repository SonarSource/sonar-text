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
using SonarLint.VisualStudio.Integration.UnitTests;
using System.Linq;

namespace SonarLint.Secrets.DotNet.UnitTests
{
    [TestClass]
    public class DummySecretDetectorTests
    {
        [TestMethod]
        public void MefCtor_CheckIsExported()
        {
            MefTestHelpers.CheckTypeCanBeImported<DummySecretDetector, ISecretDetector>(null, null);
        }

        [TestMethod]
        public void Find_Simple()
        {
            var testSubject = new DummySecretDetector();

            const string input = @"sonarlint.internal.dummy.password=9E7E72A5-10B6-4B72-8C04-349C045599C4  // should match
  woijwoj w woijw e
  ffef  password=abcdef

  sonarlint.internal.dummy.password=F12504D1-AE23-4008-A599-8DEDB2A0EEEB    // should match
";

            var actual = testSubject.Find(input);

            actual.Count().Should().Be(2);

            actual.First().StartIndex.Should().Be(34);
            actual.First().Length.Should().Be(36);

            actual.Last().StartIndex.Should().Be(169);
            actual.Last().Length.Should().Be(36);
        }
    }
}
