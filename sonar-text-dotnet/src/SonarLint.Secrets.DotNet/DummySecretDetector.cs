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

using System.Collections.Generic;
using System.ComponentModel.Composition;
using System.Linq;
using System.Text.RegularExpressions;

namespace SonarLint.Secrets.DotNet
{
    [Export(typeof(ISecretDetector))]
    [PartCreationPolicy(CreationPolicy.Shared)]
    internal sealed class DummySecretDetector : ISecretDetector
    {
        private const string PasswordPattern = "sonarlint\\.internal\\.dummy\\.password\\=(?<pwd>[\\w-]+)\\s";
        private static readonly Regex StackRegExp = new Regex(PasswordPattern, RegexOptions.None, RegexConstants.DefaultTimeout);

        private const string RuleKey = "secrets:sonarlint.internal.dummy.pwd";

        string ISecretDetector.RuleKey => RuleKey;

        public string Message => "Don't hard-code credentials";

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
