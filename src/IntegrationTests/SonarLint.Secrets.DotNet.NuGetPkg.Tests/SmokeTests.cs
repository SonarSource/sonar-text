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
""private_key"": ""-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCqypYh1ms+SNYj\n8o7F1a4v9JO2h4Rj6TO7sUaiBlmqERTnjMdiNmQiJB6zS+iVExh804vleIYHv75E\n5BEZxeUTuA6s3PDs9fGhql6YFY8p7sVarbfg3X50trVvfz85XmvnNj/+dlWTu73g\nAnsCiDmgdjmQXbZsAXaY0S3eZO2cgkG7btyG31+uqSOUrohscJkLIqEaI4p6rDWa\nw9TgtFiGt8GlrRSAEXREmbHQeOvgfPdSHVCp0MuZ2XQPLH+zoydk5AxgMDEPPWOn\ngMEmnPU41S9VT4CYcTNs4L+keoECNggRqYvNfGr6MdQmfOkwk6VU31DLKLNtzkKS\n+9ucI9wVAgMBAAECggEAGa8If8MucnnilhpSZSbaoISPuBnOC3ZU3Mtue0woLL6P\n3bhafYb/bPDwmvb13YE3JAJuHc6PnVe05chEWBNd/SanMlCxfHkJ4s1rl0RauKwb\nCqVoKw00CJo3U7KXj+mGoYkTe5FREjvf9HF7DSjuPZa51l95YOmha569Xp3f67Kd\n31olO+gqKvZJhgvDMjhSMormGRtucCwKigTvqT+jvw0NbajtyReD5AnslHroRI6h\nawMASrCl2gp6iR5h98FP9fi5dtSnTyWQjoWZqHYov95h490t3ZDUAvfjhNIEBiaC\n5PgG8NqiKRu7gGjZw66/vYox5V/tscCOtZ4M2/LCawKBgQDhF9ESmsCh3lMIwVpn\nJA4xrKpA89Bxk7X11L5ORCXeI+hicwQpv3aaUDFhSw039joSyBUsu6+cnR/2rqor\nhHJ4aRfNByFjWallp/dFoEjTAVLLX3YYWGjeFcnls8yIjC273TuztrC45+BTwPQ/\npi49cHVib1upZGogKd765cFovwKBgQDCPga9afKA5fmYDpV9nNEZkDyJqHx84OxV\ng2iQrBT91EnfQ4jWwDUbPgoV/PxNFGIJriO+h80jpOFfdr8BR8tmagGkNW35PHVW\nA23kXA77WBDlwLOvYJMayLiS+6qPD9GKXUOs68330nKhV7gCw9ptP6dj8nm4tARC\nqtzWEAW8KwKBgQDc3zX0f43OSA5KZRCEbMjQzZEyswwyprLCSsOCoTRXSfzZveqz\n3IBQ46fQxIhwBVju3Q6KGpEzqKqYsMOieBCrPtRwBzMn/e9PJQZqd+F9y6qmjUGg\nmgAtDExU7Z0h0AuAcJIIwpeemUlyZcBGRJbTCurkcEkNas8ISI3YvGKQmQKBgGuT\nfpgYvT22IG2eZhyTZRsLTvvOXGOtDjat+JNnOpj5oZANDxQgj0jvKxbSbckiqMlD\nsHgtLee3wTnlwhMrd/LYcuBG1wlZ+oIQlCQM+B8rvu//sYRHsDD9SXvd9bAVyq8e\nyARU9FV0MBg7RKYDYk1vG323JMKusE1b48KKTSLRAoGAGGILCOCFKlBt43u5bj9j\n6xDEOBn/LG8zyuGOFqIkE3iHbLuMaFpXpKT4dsx51gSrN67Zsgmj/Zf2OxU7vTfF\nHP+SrqniXDOSsmjHWXs+V7enSC/7e/Lwaw8rNI42hizLMzNNqPx+5Vd/vBIN6z0U\ncU6a4WWC772nb+mLIODMlx0=\n-----END PRIVATE KEY-----\n"",

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
                ("secrets:S6335", "-----BEGIN PRIVATE KEY-----\\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCqypYh1ms+SNYj\\n8o7F1a4v9JO2h4Rj6TO7sUaiBlmqERTnjMdiNmQiJB6zS+iVExh804vleIYHv75E\\n5BEZxeUTuA6s3PDs9fGhql6YFY8p7sVarbfg3X50trVvfz85XmvnNj/+dlWTu73g\\nAnsCiDmgdjmQXbZsAXaY0S3eZO2cgkG7btyG31+uqSOUrohscJkLIqEaI4p6rDWa\\nw9TgtFiGt8GlrRSAEXREmbHQeOvgfPdSHVCp0MuZ2XQPLH+zoydk5AxgMDEPPWOn\\ngMEmnPU41S9VT4CYcTNs4L+keoECNggRqYvNfGr6MdQmfOkwk6VU31DLKLNtzkKS\\n+9ucI9wVAgMBAAECggEAGa8If8MucnnilhpSZSbaoISPuBnOC3ZU3Mtue0woLL6P\\n3bhafYb/bPDwmvb13YE3JAJuHc6PnVe05chEWBNd/SanMlCxfHkJ4s1rl0RauKwb\\nCqVoKw00CJo3U7KXj+mGoYkTe5FREjvf9HF7DSjuPZa51l95YOmha569Xp3f67Kd\\n31olO+gqKvZJhgvDMjhSMormGRtucCwKigTvqT+jvw0NbajtyReD5AnslHroRI6h\\nawMASrCl2gp6iR5h98FP9fi5dtSnTyWQjoWZqHYov95h490t3ZDUAvfjhNIEBiaC\\n5PgG8NqiKRu7gGjZw66/vYox5V/tscCOtZ4M2/LCawKBgQDhF9ESmsCh3lMIwVpn\\nJA4xrKpA89Bxk7X11L5ORCXeI+hicwQpv3aaUDFhSw039joSyBUsu6+cnR/2rqor\\nhHJ4aRfNByFjWallp/dFoEjTAVLLX3YYWGjeFcnls8yIjC273TuztrC45+BTwPQ/\\npi49cHVib1upZGogKd765cFovwKBgQDCPga9afKA5fmYDpV9nNEZkDyJqHx84OxV\\ng2iQrBT91EnfQ4jWwDUbPgoV/PxNFGIJriO+h80jpOFfdr8BR8tmagGkNW35PHVW\\nA23kXA77WBDlwLOvYJMayLiS+6qPD9GKXUOs68330nKhV7gCw9ptP6dj8nm4tARC\\nqtzWEAW8KwKBgQDc3zX0f43OSA5KZRCEbMjQzZEyswwyprLCSsOCoTRXSfzZveqz\\n3IBQ46fQxIhwBVju3Q6KGpEzqKqYsMOieBCrPtRwBzMn/e9PJQZqd+F9y6qmjUGg\\nmgAtDExU7Z0h0AuAcJIIwpeemUlyZcBGRJbTCurkcEkNas8ISI3YvGKQmQKBgGuT\\nfpgYvT22IG2eZhyTZRsLTvvOXGOtDjat+JNnOpj5oZANDxQgj0jvKxbSbckiqMlD\\nsHgtLee3wTnlwhMrd/LYcuBG1wlZ+oIQlCQM+B8rvu//sYRHsDD9SXvd9bAVyq8e\\nyARU9FV0MBg7RKYDYk1vG323JMKusE1b48KKTSLRAoGAGGILCOCFKlBt43u5bj9j\\n6xDEOBn/LG8zyuGOFqIkE3iHbLuMaFpXpKT4dsx51gSrN67Zsgmj/Zf2OxU7vTfF\\nHP+SrqniXDOSsmjHWXs+V7enSC/7e/Lwaw8rNI42hizLMzNNqPx+5Vd/vBIN6z0U\\ncU6a4WWC772nb+mLIODMlx0=\\n-----END PRIVATE KEY-----\\n"),
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