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

// Ported from ...\sonar-secrets-plugin\src\test\java\com\sonarsource\secrets\rules\GoogleCloudAccountKeyRuleTest.java

using FluentAssertions;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using SonarLint.Secrets.DotNet.Rules;
using SonarLint.VisualStudio.Integration.UnitTests;
using System.Linq;
using static SonarLint.Secrets.DotNet.UnitTests.JavaTestFileUtils;
using static SonarLint.Secrets.DotNet.UnitTests.TestUtils;

namespace SonarLint.Secrets.DotNet.UnitTests.Rules
{
    [TestClass]
    public class GoogleCloudAccountKeyRuleTest
    {
        [TestMethod]
        public void MefCtor_CheckIsExported()
        {
            MefTestHelpers.CheckTypeCanBeImported<GoogleCloudAccountKeyRule, ISecretDetector>(null, null);
        }

        [TestMethod]
        public void TestRuleProperties()
        {
            var testSubject = new GoogleCloudAccountKeyRule();

            testSubject.RuleKey.Should().Be("secrets:S6335");
            testSubject.Name.Should().Be("Google Cloud service accounts keys should not be disclosed");
            testSubject.Message.Should().Be("Make sure this Google Cloud service account key is not disclosed.");
        }

        [TestMethod]
        public void TestRuleRegexPositive()
        {
            var testSubject = new GoogleCloudAccountKeyRule();

            // Note: the expected value in the file contains backslashes, which need to be escaped in the following string variable
            var expectedSecret = "-----BEGIN PRIVATE KEY-----\\nWlQKvHwr2SWL2BxsRc/0mJj66+8ICu0djuCfLRMunzN8teUknpKDlUTncWPxL00H\\n4UCI0sUaCSNY9mN3qYumWx4BNpd1bpVzLfeMPmk8ILbGh8r2k0h0GPYH3b7Xp6/n\\n9nn3haodNzRlvxtS+nRoh869tKWOrATPM1K6UBVjMemg/NmFUoZcpXfauqJKrMZn\\nTbLGZA/JFHPdV0/Tyq6cNRA2WKMD4k50s0z9bSTtqqJUhcrGs2maSWMcOWXp0HSu\\nPPYVAcSSKNZxhSUUsflb/4tIAxur7DGWKpfFqIgqkGDf+p89bd+UwQwXwpvcUAYg\\nhdZ4NpDW084EBnLkCyq3GYz76uibndIWVw/7DdvZNnVBY4wjQkNwk7NvQ4/thCJ8\\nziWeoRUycEqtd/jb+0xcDlL0WQqLfcRlRZbPir70CouTRusRV+vheGFnJ9vLAjt0\\nZeOq+G5SfyNp45iCxM3hTKglu0gm8vLt7v7NSbUoeQcT6pwR06n1tlso+D9jATWW\\n0tvJ+IWw4OddHAreEBychWaVcSPoeK1x6t5M70Trj49RioUJl5S/uUOdtMGs6Iru\\n0SkoISjQVh5qZvN0/fcm53dlR1sutByOMP4CeGbFVbqn2xr+0Su0FfB9P/JTA1wt\\nVqRkEkWFdJD3N2wuKuvNhLjGn7Zg9vf6S9FLTgG192W8PLwZOpifGVmkhl9JyQIV\\nEJlQxYNE0GRs1n4PK0EUn0z9XvDIk+DttbEfzTC2ORsae7v7uu/yl1IEkp6cvft3\\ncx/ZWYp4KOfIUz430Jo3/fiFG08k0zHIxB+U8IlIeg0JV0XXxddY4D4+pGtW3eel\\ngKksBj7DxSY4sLWGpkZWwWQ6JaFCpBR0cPDl1EvoiMBmSaRQWZGSru1x6rWBRFn8\\naY2QhLRY1LOWuEoJv1TC70UU0fsKV7KQyFl6lbJkiV3TTfcBcmf2ZYbbdh2cthc6\\nb/x2tvyusIumibbVd3II2U4NEj2dlH4aS+SUUmiU1XKqD16cgh/CMHdthXyjFuiY\\nQtGlvta3V0TfkNjPqZ+eIBBl6yN+10oGPYMkvUTZgiUY0/iGNW5Sxeqo5us+SdcV\\nMnDya0tIgk+zUQx7pgxehJjKIysaCWgdg/lNIXGODwMmg3Ho+xUMT+Odd52PUsfw\\ne7tVEWyFNVOX99ES3ddfcHwixiJQ746kqpimJYXn0o0+w3ghBkYRoauJKlywnVKW\\n2EHU0sjet3g0HzZGbCpXfp8p7LOxABarQqi4wztiaUxy7Hbkz4uXF0QMIrZoQYQ4\\n32jrr7jPjgWx4JvzKD/JhqY3MIwfMeFodGIfyX6EURNzdsU/9ARm0hXNdsSVLedf\\nNIuybpb0KG1CPyZt1xilzsB8V8bfel6PNHYI/v+Tz4Ptsu+RFy6n94TugyQfn8Vz\\nMm2hNrvFzBZHQhQA/wMTrSTV1DCMS4FDjqTgF2eodB1HelR/Vqh4vch7Fgsj1V0H\\n9wyABJ7isyP34lQQZD4CXOihiZOyJ7zvNtLVZqKmv/L+zrctDPxy4yXuoqrO8ILx\\n/upKN83I5SgYdkU4Xu87KbDpJrHI09qZAn0fbETIY1Fmr/rdJA4OMR093abwAuJ9\\nBty2lP3sNAjVJr+K0E0MePm=\\n-----END PRIVATE KEY-----\\n"; //NOSONAR

            var input = readFileAndNormalize(Constants.RootPath + "checks\\GoogleCloudAccountKeyCheck\\GoogleCloudAccountPositive.json", UTF_8);
            var secrets = testSubject.Find(input);

            secrets.Count().Should().Be(1);
            CheckExpectedSecretFound(input, expectedSecret, secrets.First());
            CrossCheckWithJavaResult(input, new(5, 18, 5, 1750), secrets.First());
        }

        [TestMethod]
        public void TestRuleRegexNegative()
        {
            var testSubject = new GoogleCloudAccountKeyRule();

            var secrets = testSubject.Find(readFileAndNormalize(Constants.RootPath + "checks\\GoogleCloudAccountKeyCheck\\GoogleCloudAccountNegative.json", UTF_8));

            secrets.Should().BeEmpty();
        }
    }
}
