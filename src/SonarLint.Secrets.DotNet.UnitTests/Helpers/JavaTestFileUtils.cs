/*
 * Copyright (C) 2021-2022 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
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
