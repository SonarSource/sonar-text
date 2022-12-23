/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2022 SonarSource SA
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
package org.sonar.plugins.secrets.rules;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public final class SecretCheckList {
  private SecretCheckList() {
    // utility class
  }

  public static List<Class<?>> checks() {
    return List.of(
      AlibabaCloudAccessKeyIDsRule.class,
      AlibabaCloudAccessKeySecretsRule.class,
      AwsAccessKeyIdRule.class,
      AwsAccessKeyRule.class,
      AwsSessionTokenRule.class,
      AzureStorageAccountKeyRule.class,
      GoogleApiKeyRule.class,
      GoogleCloudAccountKeyRule.class,
      IbmApiKeyRule.class,
      MwsAuthTokenRule.class);
  }

  public static List<SecretRule> createInstances() {
    List<SecretRule> instances = new ArrayList<>();
    try {
      for (Class<?> check : SecretCheckList.checks()) {
        instances.add((SecretRule) check.getDeclaredConstructor().newInstance());
      }
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException |
             InvocationTargetException | NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }
    return instances;
  }
}
