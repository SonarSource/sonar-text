/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

// NOTE: the Match API in .NET is not the same as the Java version

using FluentAssertions;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SonarLint.Secrets.DotNet.Rules.Matching;

namespace SonarLint.Secrets.DotNet.UnitTests.DotNetOnly
{
    [TestClass]
    public class MatchTest
    {
        [TestMethod]
        public void Ctor_PropertiesSet()
        {
            var testSubject = new Match("my text", 0, 100);

            testSubject.Text.Should().Be("my text");
            testSubject.StartIndex.Should().Be(0);
            testSubject.Length.Should().Be(100);
        }

        [TestMethod]
        [DataRow(0, 5)]  // last char overlaps with knownMatch
        [DataRow(9, 10)] // first char overlaps with knownMatch
        [DataRow(5, 9)]  // ranges match
        [DataRow(6, 8)]  // knownMatch contains newMatch
        [DataRow(0, 10)] // newMatch contains knownMatch
        [DataRow(4, 6)]  // newMatch overlaps at start
        [DataRow(8, 10)] // newMatch overlaps at end
        public void Overlap_OverlapExists(int rangeStart, int rangeEnd)
        {
            // Test against a known range [5..10]
            var fixedMatch = CreateMatchFromRange(5, 10);

            var newRange = CreateMatchFromRange(rangeStart, rangeEnd);

            // Result should be reflexive
            fixedMatch.Overlaps(newRange).Should().BeTrue();
            newRange.Overlaps(fixedMatch).Should().BeTrue();
        }

        [TestMethod]
        [DataRow(1, 5)]
        [DataRow(5, 5)]
        [DataRow(5, 9)]
        [DataRow(1, 9)]
        public void Overlap_SingleCharRange_OverlapExists(int rangeStart, int rangeEnd)
        {
            // Test against a known single character range [5..5]
            var fixedMatch = CreateMatchFromRange(5, 5);

            var newRange = CreateMatchFromRange(rangeStart, rangeEnd);

            // Overlap should be reflexive
            fixedMatch.Overlaps(newRange).Should().BeTrue();
            newRange.Overlaps(fixedMatch).Should().BeTrue();
        }

        [TestMethod]
        [DataRow(1, 4)]
        [DataRow(10, 11)]
        public void Overlap_NoOverlap(int rangeStart, int rangeEnd)
        {
            // Test against a known range
            var fixedMatch = CreateMatchFromRange(5, 9);

            var newRange = CreateMatchFromRange(rangeStart, rangeEnd);

            // Overlap should be reflexive
            fixedMatch.Overlaps(newRange).Should().BeFalse();
            newRange.Overlaps(fixedMatch).Should().BeFalse();
        }

        private static Match CreateMatchFromRange(int rangeStart, int rangeEnd) =>
            new Match("any", rangeStart, rangeEnd - rangeStart + 1);
    }
}
