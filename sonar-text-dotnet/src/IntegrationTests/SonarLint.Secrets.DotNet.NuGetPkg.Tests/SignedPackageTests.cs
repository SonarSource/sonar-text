/*
 * Copyright (C) 2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

using FluentAssertions;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System;
using System.Linq;
using System.Security.Cryptography.X509Certificates;

// These tests are only relevant for signed packages, which we expect to
// be strong-named, Authenticode-signed and obfuscated.
// Normally these will only be executed on the CI machine for rolling builds
// on master, not for local builds or PR builds.

namespace SonarLint.Secrets.DotNet.NuGetPkg.Tests
{
    [TestClass]
    public class SignedPackageTests
    {
        private static void SkipIfUnsigned()
        {
            // This method is here so that the tests will always be run and appear in the test results,
            // even for unsigned builds - makes it easier to see what is happening for each build.
#if !SignAssembly
            Assert.Inconclusive("Not a signed build");
#endif
        }

        [TestMethod]
        public void IsStrongNamed()
        {
            SkipIfUnsigned();

            var productAssembly = typeof(ISecretDetector).Assembly;

            var name = productAssembly.GetName();

            var publicKeyToken = name.GetPublicKeyToken();
            publicKeyToken.Should().NotBeNull();
            publicKeyToken.Length.Should().BeGreaterThan(0, because: "the assembly should be strong-named");
        }

        [TestMethod]
        public void IsAuthenticodeSigned()
        {
            SkipIfUnsigned();

            var productFilePath = typeof(ISecretDetector).Assembly.Location;

            X509Certificate cert = null;

            Action act = () => cert = X509Certificate.CreateFromSignedFile(productFilePath);

            act.Should().NotThrow(because: $"the assembly should be Authenticode-signed. Could not retrieve certificate from {productFilePath}");
            cert.Should().NotBeNull();

            // Check the certificate date is valid
            var fromDate = DateTimeOffset.Parse(cert.GetEffectiveDateString());
            fromDate.Should().BeBefore(DateTimeOffset.UtcNow);
            
            var toDate = DateTimeOffset.Parse(cert.GetExpirationDateString());
            toDate.Should().BeAfter(DateTimeOffset.UtcNow);
        }

        [TestMethod]
        public void Obfuscation_OnlyExpectedInterfacesInSonarLintNamespace()
        {
            SkipIfUnsigned();

            // Check that the only types under the SonarLint namespace are the ones we expect.
            // All other types should have been moved to other namespaces.
            string[] expectedTypeNames = {
                "SonarLint.Secrets.DotNet.ISecret",
                "SonarLint.Secrets.DotNet.ISecretDetector"
            };

            var typesInSonarLintNs = typeof(ISecretDetector).Assembly.DefinedTypes
                .Where(x => x.FullName.StartsWith("SonarLint.", StringComparison.Ordinal))
                .Select(x => x.FullName)
                .ToArray();

            typesInSonarLintNs.Should().BeEquivalentTo(expectedTypeNames);
        }

        [TestMethod]
        // Matching classes
        [DataRow("ConditionalMatcher")]
        [DataRow("ISecretsMatcher")]
        [DataRow("Match")]
        [DataRow("RegexMatcher")]
        // Rule classes
        [DataRow("AbstractSecretRule")]
        [DataRow("AbstractAlibabaCloudAccessKeyRule")]
        [DataRow("AbstractAwsRule")]
        [DataRow("AzureStorageAccountKeyRule")]
        [DataRow("GoogleApiKeyRule")]
        [DataRow("DummySecretDetector")]
        // Miscellaneous
        [DataRow("EntropyChecker")]
        public void Obfuscation_ImplemenationClassesAreObfuscated(string simpleTypeName)
        {
            SkipIfUnsigned();

            // The internal implemenation classes should be obfuscated and renamed.
            // As long as we can't find some of the known secrets implementation classes by
            // name then we'll assume that they have been renamed and obfuscated.

            var actualTypes = typeof(ISecretDetector).Assembly.DefinedTypes;

            var match = actualTypes.FirstOrDefault(x => string.Equals(simpleTypeName, x.Name, StringComparison.Ordinal));
            match.Should().BeNull();
        }
    }
}
