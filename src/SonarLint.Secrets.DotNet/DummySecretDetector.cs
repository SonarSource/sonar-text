/*
 * Copyright (C) 2018-2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

using System.Collections.Generic;
using System.ComponentModel.Composition;
using System.Linq;
using System.Text.RegularExpressions;

namespace SonarLint.Secrets.DotNet
{
    [Export(typeof(ISecretDetector))]
    [PartCreationPolicy(CreationPolicy.Shared)]
    public class DummySecretDetector : ISecretDetector
    {
        private const string PasswordPattern = "password=((?<pwd>\\w+)\\s)";
        private static readonly Regex StackRegExp = new Regex(PasswordPattern);

        private const string RuleKey = "secrets:DUMMY";

        string ISecretDetector.RuleKey => RuleKey;

        public string Message => "Don't hard-code passwords";

        public IEnumerable<ISecret> Find(string input)
        {
            if (string.IsNullOrEmpty(input))
            {
                return Enumerable.Empty<Secret>();
            }

            var results = new List<Secret>();

            foreach (Match match in StackRegExp.Matches(input))
            {
                var pwdGrp = match.Groups["pwd"];

                var newRange = new Secret(pwdGrp.Index, pwdGrp.Value.Length);
                results.Add(newRange);
            }

            return results;
        }
    }
}
