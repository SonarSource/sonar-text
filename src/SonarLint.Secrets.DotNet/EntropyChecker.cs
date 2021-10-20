/*
 * Copyright (C) 2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
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
