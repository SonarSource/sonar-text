/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

using FluentAssertions;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SonarLint.Secrets.DotNet.DotNetOnly;

namespace SonarLint.Secrets.DotNet.UnitTests.DotNetOnly
{
    [TestClass]
    public class TextRangeTest
    {
        [TestMethod]
        public void Ctor_ValidProperties()
        {
            var testSubject = new TextRange(0, 1);

            testSubject.StartIndex.Should().Be(0);
            testSubject.EndIndex.Should().Be(1);
        }

        [TestMethod]
        [DataRow(0, 5)]  // last char overlaps with fixedRange
        [DataRow(9, 10)] // first char overlaps with fixedRange
        [DataRow(5, 9)]  // ranges match
        [DataRow(6, 8)]  // fixRange contains newRange
        [DataRow(0, 10)] // newRange contains fixedRange
        [DataRow(4, 6)]  // newRange overlaps at start
        [DataRow(8, 10)] // newRange overlaps at end
        public void Overlap_OverlapExists(int startIndex, int endIndex)
        {
            // Test against a known range
            var fixedRange = new TextRange(5, 9);

            var newRange = new TextRange(startIndex, endIndex);

            // Result should be reflexive
            fixedRange.overlap(newRange).Should().BeTrue();
            newRange.overlap(fixedRange).Should().BeTrue();
        }

        [TestMethod]
        [DataRow(1, 5)]
        [DataRow(5, 5)]
        [DataRow(5, 9)]
        [DataRow(1, 9)]
        public void OverLap_SingleCharRange_OverlapExists(int startIndex, int endIndex)
        {
            // Test against a known single character range
            var fixedRange = new TextRange(5, 5);

            var newRange = new TextRange(startIndex, endIndex);

            // Overlap should be reflexive
            fixedRange.overlap(newRange).Should().BeTrue();
            newRange.overlap(fixedRange).Should().BeTrue();
        }

        [TestMethod]
        [DataRow(1, 4)]
        [DataRow(10, 11)]
        public void OverLap_NoOverlap(int startIndex, int endIndex)
        {
            // Test against a known range
            var fixedRange = new TextRange(5, 9);

            var newRange = new TextRange(startIndex, endIndex);

            // Overlap should be reflexive
            fixedRange.overlap(newRange).Should().BeFalse();
            newRange.overlap(fixedRange).Should().BeFalse();
        }
    }
}
