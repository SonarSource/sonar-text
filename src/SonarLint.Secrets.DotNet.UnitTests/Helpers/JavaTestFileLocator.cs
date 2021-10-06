/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

using Microsoft.VisualStudio.TestTools.UnitTesting;
using System.IO;

namespace SonarLint.Secrets.DotNet.UnitTests
{
    internal static class JavaTestFileLocator
    {
        public static string LocateJavaTestFile(string relativeJavaTestFilePath)
        {
            // We want to use the same test case files as for the Java tests.
            // The Java tests use file paths relative to the sonar-secrets-plugin\pom.xml file.
            // We're using our knowledge about the repo layout to translate the Java test relative paths
            // to absolute paths.

            var fullPath = Path.Combine(LocateRepoRootDirectory(), "sonar-secrets-plugin", relativeJavaTestFilePath);
            Assert.IsTrue(File.Exists(fullPath), @"Test setup error: could not find test file: {fullPath}");
            return fullPath;
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
