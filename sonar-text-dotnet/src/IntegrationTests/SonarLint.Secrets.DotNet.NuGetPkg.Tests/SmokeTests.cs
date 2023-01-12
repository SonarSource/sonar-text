/*
 * Copyright (C) 2021 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */

using FluentAssertions;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System.Collections.Generic;
using System.ComponentModel.Composition.Hosting;
using System.Linq;

// These smoke tests should work, regardless of whether the assemblies have been signed/strong-named/obfuscated.

namespace SonarLint.Secrets.DotNet.NuGetPkg.Tests
{
    [TestClass]
    public class SmokeTests
    {
        [TestMethod]
        public void MefImports_ExpectedRulesImported()
        {
            var actualExports = GetSecretDetectors();
            actualExports.Length.Should().Be(11);
        }

        [TestMethod]
        public void FindSecrets()
        {
            var input = @"

1. AlibabaCloudAccessKeyIDsRule
LTAI5tBcc9SecYAomgyUSFs8 // S6336

2. AlibabaCloudAccessKeySecretsRule
String aliyunAccessKeySecret=""KmkwlDrPBC68bgvZiNtrjonKIYmVT8"";  // S6336

3. AwsAccessKeyIdRule
  public static final String KEY = ""AKIAIGKECZXA7AEIJLMQ"" // S6290

4. AwsAccessKeyRule
{     secretAccessKey: 'kHeUAwnSUizTWpSbyGAz4f+As5LshPIjvtpswqGb' } // S6290
aws_secret_access_key=kHeUAwnSUizTWpSbyGAz4f+As5LshPIjvtpswqGb // S6290

5. AwsSessionTokenRule
AWS_SESSION_TOKEN=IQoJb3JpZ2luX2VjEKL//////////wEaDGV1LWNlbnRyYWwtMSJHMEUCIQDFlDUEvUa6slxlkKKn8zbLkN/j1f7lKJdXJ03PQ5T5ZwIgDYlshciO8nyfnmjUfFy4I2+rEuPHBexsvfBo3MlCdgQqugMISxAAGgw4NTk4OTY2NzUzMDYiDFKPV7D/QmnqFWRYpiqXAypJf6TksPZXImVpIUU0Yj0uJhNN0o/HcO8hfQ4BXuCvpm1DOiVsH6VXMxgNdpGTWr8CjNpEt/eYwSk6MAVPOtjg5+lY2qoGJrUuxwhiKe+BquVM17h0giZ18h1B4ozDGkfxA/vGSJa/qBznF0yEpLE+fJoesGe4ZpATs8oUN94/XkrL/eYzXsW3ZD1ZX66QzmSFHhgTJc24d9bezGjR32fEJD/dBm9La+7wpc4+jrXCmt6yxHox0gCuGrSagcJfPh9pVYneM81fnD/S7Kicb1Pw8MiChfqW0hao1twr4wMgp9N3JlYQNK3fZKbMU/qlvoKTz8D0Joa4elSp4rU4reVUsujCXVE95PDyj4LD3IDXHF5SAd/23/M/IucMRyeWlRE4pCtry68ENpojXr0tdyyVs8XSkgCGgup/BqDTkBnEBD+V5hOIrHJv5rJ6KpaxEZG0ozUJdaUpCseSSKK4Jn7liqVqF5EzOOXelqTAACcJmILKQHqke8n3imNs72oi8tu1N+oqbFp60K9whtLDm0JZSavpmRDkMODb8/4FOusBHFYZCuxMUmotN9Dkzp4InT7kJdKZ/kr61SMhU4hj7vTdjhcRHItO2P+jR7+38kQLDR4O1HR1XkHzLMwDvDwZULeOl6afS1ZpbO8XpePHaaLnEqJeZ8BpnfwBEiylK3HGzGAP7WcAgFlMO9AEqoGnnbUBFcL+IYnZ3JFPy0sGsrH4cOC8Gxy2icQKrGpdIyMqGjb2hZsSc1S4njGpK0AlCEKrAjzpr6SzPSwLnFtAJpztHbgb9Z7D2jdsjugQYdFwi6/9GKOI/slKqt5/vb7dLnSyeAY+jTaoveUZf6D5yM8PCKrvw5/k+A1XJw==  // S6290

6. AzureStorageAccountKeyRule - S6338
const accountKey = process.env.ACCOUNT_KEY || ""4dVw+l0W8My+FwuZ08dWXn+gHxcmBtS7esLAQSrm6/Om3jeyUKKGMkfAh38kWZlItThQYsg31v23A0w/uVP4pg=="";
const connStr = ""DefaultEndpointsProtocol=https;AccountName=testaccountname;AccountKey=4dVw+l0W8My+FwuZ08dWXn+gHxcmBtS7esLAQSrm6/Om3jeyUKKGMkfAh38kWZlItThQYsg31v23A0w/uVP4pg=="";
const connStr = ""DefaultEndpointsProtocol=https;AccountName=testaccountname;AccountKey=4dVw+l0W8My+FwuZ08dWXn+gHxcmBtS7esLAQSrm6/Om3jeyUKKGMkfAh38kWZlItThQYsg31v23A0w/uVP4pg==;EndpointSuffix=core.windows.net"";

7. GoogleApiKeyRule - S6334
android:value=""AIzaSyCis4NzxMw1aJyvUIrjGILjPkSdxrRfof4"";

8. GoogleCloudAccountKeyRule - S6335
""private_key"": ""-----BEGIN PRIVATE KEY-----\nWlQKvHwr2SWL2BxsRc/0mJj66+8ICu0djuCfLRMunzN8teUknpKDlUTncWPxL00H\n4UCI=sUaCSNY9mN3qYumWx4BNpd1bpVzLfeMPmk8ILbGh8r2k0h0GPYH3b7Xp6/n\n9nn3haodNzRlvxtS+nRoh869tKWOrATPM1K6UBVjMemg/NmFUoZcpXfauqJKrMZn\nTbLGZA/JFHPdV0/Tyq6cNRA2WKMD4k5=s=z9bSTtqqJUhcrGs2maSWMcOWXp0HSu\nPPYVAcSSKNZxhSUUsflb/4tIAxur7DGWKpfFqIgqkGDf+p89bd+UwQwXwpvcUAYg\nhdZ4NpDW=84EBnLkCyq3GYz76uibndIWVw/7DdvZNnVBY4wjQkNwk7NvQ4/thCJ8\nziWeoRUycEqtd/jb+=xcDlL0WQqLfcRlRZbPir7=CouTRusRV+vheGFnJ9vLAjt=\nZeOq+G5SfyNp45iCxM3hTKglu0gm8vLt7v7NSbUoeQcT6pwR06n1tlso+D9jATWW\n0tvJ+IWw4OddHAreEBychWaVcSPoeK1x6t5M70Trj49RioUJl5S/uUOdtMGs6Iru\n0SkoISjQVh5qZvN0/fcm53dlR1sutByOMP4CeGbFVbqn2xr+=Su0FfB9P/JTA1wt\nVqRkEkWFdJD3N2wuKuvNhLjGn7Zg9vf6S9FLTgG192W8PLwZOpifGVmkhl9JyQIV\nEJlQxYNE0GRs1n4PK=EUn=z9XvDIk+DttbEfzTC2ORsae7v7uu/yl1IEkp6cvft3\ncx/ZWYp4KOfIUz43=Jo3/fiFG=8k0zHIxB+U8IlIeg0JV=XXxddY4D4+pGtW3eel\ngKksBj7DxSY4sLWGpkZWwWQ6JaFCpBR=cPDl1EvoiMBmSaRQWZGSru1x6rWBRFn8\naY2QhLRY1LOWuEoJv1TC7=UU0fsKV7KQyFl6lbJkiV3TTfcBcmf2ZYbbdh2cthc6\nb/x2tvyusIumibbVd3II2U4NEj2dlH4aS+SUUmiU1XKqD16cgh/CMHdthXyjFuiY\nQtGlvta3V0TfkNjPqZ+eIBBl6yN+1=oGPYMkvUTZgiUY0/iGNW5Sxeqo5us+SdcV\nMnDya=tIgk+zUQx7pgxehJjKIysaCWgdg/lNIXGODwMmg3Ho+xUMT+Odd52PUsfw\ne7tVEWyFNVOX99ES3ddfcHwixiJQ746kqpimJYXn=o=+w3ghBkYRoauJKlywnVKW\n2EHU0sjet3g0HzZGbCpXfp8p7LOxABarQqi4wztiaUxy7Hbkz4uXF0QMIrZoQYQ4\n32jrr7jPjgWx4JvzKD/JhqY3MIwfMeFodGIfyX6EURNzdsU/9ARm0hXNdsSVLedf\nNIuybpb0KG1CPyZt1xilzsB8V8bfel6PNHYI/v+Tz4Ptsu+RFy6n94TugyQfn8Vz\nMm2hNrvFzBZHQhQA/wMTrSTV1DCMS4FDjqTgF2eodB1HelR/Vqh4vch7Fgsj1V0H\n9wyABJ7isyP34lQQZD4CXOihiZOyJ7zvNtLVZqKmv/L+zrctDPxy4yXuoqrO8ILx\n/upKN83I5SgYdkU4Xu87KbDpJrHI=9qZAn=fbETIY1Fmr/rdJA4OMR093abwAuJ9\nBty2lP3sNAjVJr+K=E=MePmV\n-----END PRIVATE KEY-----\n"",

9. IbmApiKeyRule - S6337
""apikey"": ""iT5wxMGq2-ZJlMAHYoODl5EuTeCPvNRkSp1h3m99HWrc""

""apikey"": ""01234567890123456789012345678901234567890123""); //   SHOULD NOT BE REPORTED - low entropy

10. MwsAuthTokenRule - S6292
export MWS_TOKEN=amzn.mws.4ea38b7b-f563-7709-4bae-12ba540c0ac5;
            ";

            var expected = new (string, string)[]
            {
                ("secrets:S6336", "LTAI5tBcc9SecYAomgyUSFs8"),
                ("secrets:S6336", "KmkwlDrPBC68bgvZiNtrjonKIYmVT8"),
                ("secrets:S6290", "AKIAIGKECZXA7AEIJLMQ"),
                ("secrets:S6290", "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"),
                ("secrets:S6290", "kHeUAwnSUizTWpSbyGAz4f+As5LshPIjvtpswqGb"),
                ("secrets:S6290", "=IQoJb3JpZ2luX2VjEKL//////////wEaDGV1LWNlbnRyYWwtMSJHMEUCIQDFlDUEvUa6slxlkKKn8zbLkN/j1f7lKJdXJ03PQ5T5ZwIgDYlshciO8nyfnmjUfFy4I2+rEuPHBexsvfBo3MlCdgQqugMISxAAGgw4NTk4OTY2NzUzMDYiDFKPV7D/QmnqFWRYpiqXAypJf6TksPZXImVpIUU0Yj0uJhNN0o/HcO8hfQ4BXuCvpm1DOiVsH6VXMxgNdpGTWr8CjNpEt/eYwSk6MAVPOtjg5+lY2qoGJrUuxwhiKe+BquVM17h0giZ18h1B4ozDGkfxA/vGSJa/qBznF0yEpLE+fJoesGe4ZpATs8oUN94/XkrL/eYzXsW3ZD1ZX66QzmSFHhgTJc24d9bezGjR32fEJD/dBm9La+7wpc4+jrXCmt6yxHox0gCuGrSagcJfPh9pVYneM81fnD/S7Kicb1Pw8MiChfqW0hao1twr4wMgp9N3JlYQNK3fZKbMU/qlvoKTz8D0Joa4elSp4rU4reVUsujCXVE95PDyj4LD3IDXHF5SAd/23/M/IucMRyeWlRE4pCtry68ENpojXr0tdyyVs8XSkgCGgup/BqDTkBnEBD+V5hOIrHJv5rJ6KpaxEZG0ozUJdaUpCseSSKK4Jn7liqVqF5EzOOXelqTAACcJmILKQHqke8n3imNs72oi8tu1N+oqbFp60K9whtLDm0JZSavpmRDkMODb8/4FOusBHFYZCuxMUmotN9Dkzp4InT7kJdKZ/kr61SMhU4hj7vTdjhcRHItO2P+jR7+38kQLDR4O1HR1XkHzLMwDvDwZULeOl6afS1ZpbO8XpePHaaLnEqJeZ8BpnfwBEiylK3HGzGAP7WcAgFlMO9AEqoGnnbUBFcL+IYnZ3JFPy0sGsrH4cOC8Gxy2icQKrGpdIyMqGjb2hZsSc1S4njGpK0AlCEKrAjzpr6SzPSwLnFtAJpztHbgb9Z7D2jdsjugQYdFwi6/9GKOI/slKqt5/vb7dLnSyeAY+jTaoveUZf6D5yM8PCKrvw5/k+A1XJw=="),
                ("secrets:S6338", "4dVw+l0W8My+FwuZ08dWXn+gHxcmBtS7esLAQSrm6/Om3jeyUKKGMkfAh38kWZlItThQYsg31v23A0w/uVP4pg=="),
                ("secrets:S6338", "4dVw+l0W8My+FwuZ08dWXn+gHxcmBtS7esLAQSrm6/Om3jeyUKKGMkfAh38kWZlItThQYsg31v23A0w/uVP4pg=="),
                ("secrets:S6338", "4dVw+l0W8My+FwuZ08dWXn+gHxcmBtS7esLAQSrm6/Om3jeyUKKGMkfAh38kWZlItThQYsg31v23A0w/uVP4pg=="),
                ("secrets:S6334", "AIzaSyCis4NzxMw1aJyvUIrjGILjPkSdxrRfof4"),
                ("secrets:S6335", "-----BEGIN PRIVATE KEY-----\\nWlQKvHwr2SWL2BxsRc/0mJj66+8ICu0djuCfLRMunzN8teUknpKDlUTncWPxL00H\\n4UCI=sUaCSNY9mN3qYumWx4BNpd1bpVzLfeMPmk8ILbGh8r2k0h0GPYH3b7Xp6/n\\n9nn3haodNzRlvxtS+nRoh869tKWOrATPM1K6UBVjMemg/NmFUoZcpXfauqJKrMZn\\nTbLGZA/JFHPdV0/Tyq6cNRA2WKMD4k5=s=z9bSTtqqJUhcrGs2maSWMcOWXp0HSu\\nPPYVAcSSKNZxhSUUsflb/4tIAxur7DGWKpfFqIgqkGDf+p89bd+UwQwXwpvcUAYg\\nhdZ4NpDW=84EBnLkCyq3GYz76uibndIWVw/7DdvZNnVBY4wjQkNwk7NvQ4/thCJ8\\nziWeoRUycEqtd/jb+=xcDlL0WQqLfcRlRZbPir7=CouTRusRV+vheGFnJ9vLAjt=\\nZeOq+G5SfyNp45iCxM3hTKglu0gm8vLt7v7NSbUoeQcT6pwR06n1tlso+D9jATWW\\n0tvJ+IWw4OddHAreEBychWaVcSPoeK1x6t5M70Trj49RioUJl5S/uUOdtMGs6Iru\\n0SkoISjQVh5qZvN0/fcm53dlR1sutByOMP4CeGbFVbqn2xr+=Su0FfB9P/JTA1wt\\nVqRkEkWFdJD3N2wuKuvNhLjGn7Zg9vf6S9FLTgG192W8PLwZOpifGVmkhl9JyQIV\\nEJlQxYNE0GRs1n4PK=EUn=z9XvDIk+DttbEfzTC2ORsae7v7uu/yl1IEkp6cvft3\\ncx/ZWYp4KOfIUz43=Jo3/fiFG=8k0zHIxB+U8IlIeg0JV=XXxddY4D4+pGtW3eel\\ngKksBj7DxSY4sLWGpkZWwWQ6JaFCpBR=cPDl1EvoiMBmSaRQWZGSru1x6rWBRFn8\\naY2QhLRY1LOWuEoJv1TC7=UU0fsKV7KQyFl6lbJkiV3TTfcBcmf2ZYbbdh2cthc6\\nb/x2tvyusIumibbVd3II2U4NEj2dlH4aS+SUUmiU1XKqD16cgh/CMHdthXyjFuiY\\nQtGlvta3V0TfkNjPqZ+eIBBl6yN+1=oGPYMkvUTZgiUY0/iGNW5Sxeqo5us+SdcV\\nMnDya=tIgk+zUQx7pgxehJjKIysaCWgdg/lNIXGODwMmg3Ho+xUMT+Odd52PUsfw\\ne7tVEWyFNVOX99ES3ddfcHwixiJQ746kqpimJYXn=o=+w3ghBkYRoauJKlywnVKW\\n2EHU0sjet3g0HzZGbCpXfp8p7LOxABarQqi4wztiaUxy7Hbkz4uXF0QMIrZoQYQ4\\n32jrr7jPjgWx4JvzKD/JhqY3MIwfMeFodGIfyX6EURNzdsU/9ARm0hXNdsSVLedf\\nNIuybpb0KG1CPyZt1xilzsB8V8bfel6PNHYI/v+Tz4Ptsu+RFy6n94TugyQfn8Vz\\nMm2hNrvFzBZHQhQA/wMTrSTV1DCMS4FDjqTgF2eodB1HelR/Vqh4vch7Fgsj1V0H\\n9wyABJ7isyP34lQQZD4CXOihiZOyJ7zvNtLVZqKmv/L+zrctDPxy4yXuoqrO8ILx\\n/upKN83I5SgYdkU4Xu87KbDpJrHI=9qZAn=fbETIY1Fmr/rdJA4OMR093abwAuJ9\\nBty2lP3sNAjVJr+K=E=MePmV\\n-----END PRIVATE KEY-----\\n"),
                ("secrets:S6337", "iT5wxMGq2-ZJlMAHYoODl5EuTeCPvNRkSp1h3m99HWrc"),
                ("secrets:S6292", "amzn.mws.4ea38b7b-f563-7709-4bae-12ba540c0ac5")
            };

            var detectors = GetSecretDetectors();

            var actual = GetSecrets(input, detectors);

            actual.Should().BeEquivalentTo(expected);
        }

        private static (string ruleKey, string)[] GetSecrets(string input, IEnumerable<ISecretDetector> detectors) =>
            detectors.SelectMany(
                rule => rule.Find(input)
                            .Select(secret => (rule.RuleKey, input.Substring(secret.StartIndex, secret.Length))))
            .ToArray();

        private static ISecretDetector[] GetSecretDetectors()
        {
            var productAssembly = typeof(ISecretDetector).Assembly;
            var catalog = new AssemblyCatalog(productAssembly);

            using (CompositionContainer container = new CompositionContainer(catalog))
            {
                return container.GetExportedValues<ISecretDetector>().ToArray();
            }
        }
    }
}