/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

using Microsoft.VisualStudio.TestTools.UnitTesting;
using SonarLint.VisualStudio.Integration.UnitTests;

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
    }
}
