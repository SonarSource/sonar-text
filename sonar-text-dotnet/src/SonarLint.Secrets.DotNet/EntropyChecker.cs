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

// Ported from ...\sonar-secrets-plugin\src\main\java\com\sonarsource\secrets\EntropyChecker.java

using System;
using System.Collections.Generic;

namespace SonarLint.Secrets.DotNet
{
    internal static class EntropyChecker
    {
        internal const double ENTROPY_THRESHOLD = 4.2;

        public static bool HasLowEntropy(string str) {
            return CalculateShannonEntropy(str) < ENTROPY_THRESHOLD;
        }

        public static double CalculateShannonEntropy(string str) {
            if (string.IsNullOrEmpty(str)) {
                return 0.0;
            }

            IDictionary<char, int> charMap = new Dictionary<char, int>();
            for (int i = 0; i < str.Length; i++) {
                char c = str[i];
                if (!charMap.ContainsKey(c))
                {
                    charMap[c] = 0;
                }
                charMap[c]++;
            }

            double entropy = 0.0;
            foreach (int count in charMap.Values) {
                double frequency = 1.0 * count / str.Length;
                entropy -= frequency * Math.Log(frequency) / Math.Log(2);
            }
            return entropy;
        }
    }
}
