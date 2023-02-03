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

namespace SonarLint.Secrets.DotNet.UnitTests
{
    internal static class TestUtils
    {
        public static void CheckExpectedSecretFound(string input, string expectedText, ISecret actual)
        {
            var expectedStartIndex = input.IndexOf(expectedText, 0);

            actual.StartIndex.Should().Be(expectedStartIndex);
            actual.Length.Should().Be(expectedText.Length);
        }

        public static void CrossCheckWithJavaResult(string input, JavaLineBasedResult javaResult, ISecret dotNetResult)
        {
            javaResult.StartLine.Should().Be(javaResult.EndLine, "Invalid assumption in test code: not expecting the Java result to be multi-line");

            // Calculate the .NET equivalents of the Java line-based result
            var javaAbsoluteStatIndex = CountCharsInLines(input, javaResult.StartLine - 1) + javaResult.StartLineOffset;
            var javaLength = javaResult.EndLineOffset - javaResult.StartLineOffset;

            dotNetResult.StartIndex.Should().Be(javaAbsoluteStatIndex);
            dotNetResult.Length.Should().Be(javaLength);
        }

        private static int CountCharsInLines(string input, int linesToCount)
        {
            var charCount = 0;
            var countedLines = 0;
            var currentIndex = 0;
            while (countedLines < linesToCount)
            {
                charCount++;
                if (input[currentIndex] == '\n')
                {
                    countedLines++;
                }

                currentIndex++;
            }
            return charCount;
        }
    }
}
