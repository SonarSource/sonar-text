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
using System;
using System.Linq;
using System.Security.Cryptography.X509Certificates;

// These tests are only relevant for signed packages, which we expect to
// be strong-named and Authenticode-signed.
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
    }
}
