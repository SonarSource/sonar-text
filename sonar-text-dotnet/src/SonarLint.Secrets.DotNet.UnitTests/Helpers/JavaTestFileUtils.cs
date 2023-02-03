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

using Microsoft.VisualStudio.TestTools.UnitTesting;
using System.IO;
using System.Text;

namespace SonarLint.Secrets.DotNet.UnitTests
{
    internal static class JavaTestFileUtils
    {
        public static readonly Encoding UTF_8 = Encoding.UTF8;

        public static string LocateJavaTestFile(string relativeJavaTestFilePath)
        {
            // We want to use the same test case files as for the Java tests.
            // The Java tests use file paths relative to the sonar-secrets-plugin\pom.xml file.
            // We're using our knowledge about the repo layout to translate the Java test relative paths
            // to absolute paths.

            var fullPath = Path.Combine(LocateRepoRootDirectory(), "sonar-secrets-plugin", relativeJavaTestFilePath);
            Assert.IsTrue(File.Exists(fullPath), $"Test setup error: could not find test file: {fullPath}");
            return fullPath;
        }
        public static string readFileAndNormalize(string relativeJavaTestFilePath, Encoding charset)
        {
            var fullPath = LocateJavaTestFile(relativeJavaTestFilePath);

            return File.ReadAllText(fullPath, charset);
        }

        private static string LocateRepoRootDirectory()
        {
            var currentPath = Path.GetDirectoryName(typeof(EntropyCheckerTest).Assembly.Location);

            while (currentPath != null)
            {
                if (Directory.GetDirectories(currentPath, ".git", SearchOption.TopDirectoryOnly).Length > 0)
                {
                    return currentPath;
                }
                currentPath = Directory.GetParent(currentPath).FullName;
            }

            Assert.Fail("Test setup error: cannot locate repo root directory");
            return null;
        }

    }
}
