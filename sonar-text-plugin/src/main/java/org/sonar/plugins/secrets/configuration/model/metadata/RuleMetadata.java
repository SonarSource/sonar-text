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

package org.sonar.plugins.secrets.configuration.model.metadata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.plugins.secrets.configuration.model.Rule;

public class RuleMetadata extends Metadata {

  private String charset;
  private boolean disabled = false;

  @JsonIgnore
  private Rule rule;

  public String getCharset() {
    return charset;
  }

  public void setCharset(String charset) {
    this.charset = charset;
  }

  public boolean isDisabled() {
    return disabled;
  }

  public void setDisabled(boolean disabled) {
    this.disabled = disabled;
  }

  public Rule getRule() {
    return rule;
  }

  public void setRule(Rule rule) {
    this.rule = rule;
  }

  @Nullable
  @Override
  public String getMessage() {
    if (super.getMessage() != null) {
      return super.getMessage();
    }
    return rule.getProvider().getMetadata().getMessage();
  }

  @Nullable
  @Override
  public String getImpact() {
    if (super.getImpact() != null) {
      return super.getImpact();
    }
    return rule.getProvider().getMetadata().getImpact();
  }

  @Nullable
  @Override
  public List<Reference> getReferences() {
    List<Reference> ruleReferences = super.getReferences();
    if (ruleReferences != null) {
      return ruleReferences;
    }
    return rule.getProvider().getMetadata().getReferences();
  }

  @Nullable
  @Override
  public String getFix() {
    if (super.getFix() != null) {
      return super.getFix();
    }
    return rule.getProvider().getMetadata().getFix();
  }
}
