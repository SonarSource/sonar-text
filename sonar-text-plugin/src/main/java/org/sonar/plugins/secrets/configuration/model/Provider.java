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
package org.sonar.plugins.secrets.configuration.model;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.plugins.secrets.configuration.model.matching.Detection;
import org.sonar.plugins.secrets.configuration.model.metadata.ProviderMetadata;

public class Provider {

  private ProviderMetadata metadata;
  @Nullable
  private Detection detection;
  private List<Rule> rules;

  public ProviderMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(ProviderMetadata metadata) {
    this.metadata = metadata;
    metadata.setProvider(this);
  }

  @Nullable
  public Detection getDetection() {
    return detection;
  }

  public void setDetection(Detection detection) {
    this.detection = detection;
  }

  public List<Rule> getRules() {
    return rules;
  }

  public void setRules(List<Rule> rules) {
    this.rules = rules;
    for (Rule rule : rules) {
      rule.setProvider(this);
    }
  }

}
