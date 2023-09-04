/*
 * SonarQube Text Plugin
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
package org.sonar.plugins.secrets;

import java.util.List;
import org.sonar.api.SonarRuntime;
import org.sonar.plugins.common.CommonRulesDefinition;
import org.sonar.plugins.common.DefaultQualityProfileDefinition;
import org.sonar.plugins.secrets.checks.AlchemyApiKeyCheck;
import org.sonar.plugins.secrets.checks.AlibabaCloudAccessKeyCheck;
import org.sonar.plugins.secrets.checks.AwsCheck;
import org.sonar.plugins.secrets.checks.AzureCheck;
import org.sonar.plugins.secrets.checks.AzureStorageAccountKeyCheck;
import org.sonar.plugins.secrets.checks.ClarifaiCheck;
import org.sonar.plugins.secrets.checks.DiscordWebhookURLCheck;
import org.sonar.plugins.secrets.checks.DjangoCheck;
import org.sonar.plugins.secrets.checks.FacebookCheck;
import org.sonar.plugins.secrets.checks.FirebaseCheck;
import org.sonar.plugins.secrets.checks.GenericpublickeycryptographyCheck;
import org.sonar.plugins.secrets.checks.GitHubCheck;
import org.sonar.plugins.secrets.checks.GitLabCheck;
import org.sonar.plugins.secrets.checks.GoogleApiKeyCheck;
import org.sonar.plugins.secrets.checks.GoogleCheck;
import org.sonar.plugins.secrets.checks.GoogleCloudAccountKeyCheck;
import org.sonar.plugins.secrets.checks.GoogleUniqueNameCheck;
import org.sonar.plugins.secrets.checks.IbmApiKeyCheck;
import org.sonar.plugins.secrets.checks.MailgunCheck;
import org.sonar.plugins.secrets.checks.MicrosoftTeamsWebhookUrlCheck;
import org.sonar.plugins.secrets.checks.MongoDBCheck;
import org.sonar.plugins.secrets.checks.MwsAuthTokenCheck;
import org.sonar.plugins.secrets.checks.MySQLCheck;
import org.sonar.plugins.secrets.checks.ODBCJDBCConnectionStringCheck;
import org.sonar.plugins.secrets.checks.OpenSSHCheck;
import org.sonar.plugins.secrets.checks.OpenWeatherMapCheck;
import org.sonar.plugins.secrets.checks.PlanetscaleCheck;
import org.sonar.plugins.secrets.checks.PostgreSQLCheck;
import org.sonar.plugins.secrets.checks.RapidAPICheck;
import org.sonar.plugins.secrets.checks.RedisCheck;
import org.sonar.plugins.secrets.checks.RiotCheck;
import org.sonar.plugins.secrets.checks.SendgridCheck;
import org.sonar.plugins.secrets.checks.SlackCheck;
import org.sonar.plugins.secrets.checks.SlackWebhookURLCheck;
import org.sonar.plugins.secrets.checks.SlackWebhookURLUniqueNameCheck;
import org.sonar.plugins.secrets.checks.SonarQubeCheck;
import org.sonar.plugins.secrets.checks.SpotifyCheck;
import org.sonar.plugins.secrets.checks.StripeWebhookSecretCheck;
import org.sonar.plugins.secrets.checks.StripeWebhookSecretUniqueNameCheck;
import org.sonar.plugins.secrets.checks.TelegramCheck;
import org.sonar.plugins.secrets.checks.WeChatCheck;
import org.sonar.plugins.secrets.checks.ZapierWebhookUrlCheck;

public class SecretsRulesDefinition extends CommonRulesDefinition {

  public static final String REPOSITORY_KEY = "secrets";
  public static final String REPOSITORY_NAME = "Sonar Secrets Analyzer";

  public SecretsRulesDefinition(SonarRuntime sonarRuntime) {
    super(sonarRuntime, REPOSITORY_KEY, REPOSITORY_NAME, SecretsLanguage.KEY, checks());
  }

  public static class DefaultQualityProfile extends DefaultQualityProfileDefinition {
    public DefaultQualityProfile() {
      super(REPOSITORY_KEY, SecretsLanguage.KEY);
    }
  }

  public static List<Class<?>> checks() {
    return List.of(
      AlchemyApiKeyCheck.class,
      AlibabaCloudAccessKeyCheck.class,
      AwsCheck.class,
      AzureCheck.class,
      AzureStorageAccountKeyCheck.class,
      ClarifaiCheck.class,
      DiscordWebhookURLCheck.class,
      DjangoCheck.class,
      FacebookCheck.class,
      FirebaseCheck.class,
      GenericpublickeycryptographyCheck.class,
      GitHubCheck.class,
      GitLabCheck.class,
      GoogleApiKeyCheck.class,
      GoogleCheck.class,
      GoogleCloudAccountKeyCheck.class,
      GoogleUniqueNameCheck.class,
      IbmApiKeyCheck.class,
      MailgunCheck.class,
      MicrosoftTeamsWebhookUrlCheck.class,
      MongoDBCheck.class,
      MwsAuthTokenCheck.class,
      MySQLCheck.class,
      ODBCJDBCConnectionStringCheck.class,
      OpenSSHCheck.class,
      OpenWeatherMapCheck.class,
      PlanetscaleCheck.class,
      PostgreSQLCheck.class,
      RapidAPICheck.class,
      RedisCheck.class,
      RiotCheck.class,
      SendgridCheck.class,
      SlackCheck.class,
      SlackWebhookURLCheck.class,
      SlackWebhookURLUniqueNameCheck.class,
      SonarQubeCheck.class,
      SpotifyCheck.class,
      StripeWebhookSecretCheck.class,
      StripeWebhookSecretUniqueNameCheck.class,
      TelegramCheck.class,
      WeChatCheck.class,
      ZapierWebhookUrlCheck.class);
  }
}
