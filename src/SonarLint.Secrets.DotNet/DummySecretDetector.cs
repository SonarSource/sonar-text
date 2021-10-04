/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

using System.ComponentModel.Composition;

namespace SonarLint.Secrets.DotNet
{
    public interface ISecretDetector
    {

    }

    [Export(typeof(ISecretDetector))]
    [PartCreationPolicy(CreationPolicy.Shared)]
    public class DummySecretDetector : ISecretDetector
    {
    }
}
