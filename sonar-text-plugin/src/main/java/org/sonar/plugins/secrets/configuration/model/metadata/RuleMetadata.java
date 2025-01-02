/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.secrets.configuration.model.metadata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.secrets.configuration.model.Rule;

public class RuleMetadata extends Metadata {

  @Nullable
  private String charset;
  private boolean defaultProfile = true;

  @JsonIgnore
  private Rule rule;

  @Nullable
  public String getCharset() {
    return charset;
  }

  public void setCharset(@Nullable String charset) {
    this.charset = charset;
  }

  public boolean isDefaultProfile() {
    return defaultProfile;
  }

  public void setDefaultProfile(boolean defaultProfile) {
    this.defaultProfile = defaultProfile;
  }

  public Rule getRule() {
    return rule;
  }

  public void setRule(Rule rule) {
    this.rule = rule;
  }

  @Override
  public String getMessage() {
    if (super.getMessage() != null) {
      return super.getMessage();
    }
    return rule.getProvider().getMetadata().getMessage();
  }

  @CheckForNull
  @Override
  public String getImpact() {
    if (super.getImpact() != null) {
      return super.getImpact();
    }
    return rule.getProvider().getMetadata().getImpact();
  }

  @CheckForNull
  @Override
  public List<Reference> getReferences() {
    List<Reference> ruleReferences = super.getReferences();
    if (ruleReferences != null) {
      return ruleReferences;
    }
    return rule.getProvider().getMetadata().getReferences();
  }

  @CheckForNull
  @Override
  public String getFix() {
    if (super.getFix() != null) {
      return super.getFix();
    }
    return rule.getProvider().getMetadata().getFix();
  }
}
