/*
 * Copyright (C) 2018-2021 SonarSource SA
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

            const string input = @"password=123
  woijwoj w woijw e
  ffef  password=abcdef

";

            var actual = testSubject.Find(input);

            actual.Count().Should().Be(2);

            actual.First().StartIndex.Should().Be(9);
            actual.First().Length.Should().Be(3);

            actual.Last().StartIndex.Should().Be(52);
            actual.Last().Length.Should().Be(6);
        }
    }
}
