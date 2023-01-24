/*
 * Copyright (C) 2021-2023 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
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
