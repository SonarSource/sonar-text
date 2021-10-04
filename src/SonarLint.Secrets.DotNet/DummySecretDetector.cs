/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

using System.Collections.Generic;
using System.ComponentModel.Composition;
using System.Linq;

namespace SonarLint.Secrets.DotNet
{
    [Export(typeof(ISecretDetector))]
    [PartCreationPolicy(CreationPolicy.Shared)]
    public class DummySecretDetector : ISecretDetector
    {
        public IEnumerable<ISecret> Find(string input) => Enumerable.Empty<ISecret>();
    }
}
