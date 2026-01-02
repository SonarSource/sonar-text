/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.secrets.configuration.model.matching;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.matching.filter.PreModule;
import org.sonar.plugins.secrets.configuration.model.matching.filter.TopLevelPostModule;

public class Detection {

  @Nullable
  private Matching matching;
  @Nullable
  private PreModule pre;
  @Nullable
  private TopLevelPostModule post;
  @JsonIgnore
  private Rule rule;

  public Detection() {
    // empty constructor to be used by Jackson
  }

  private boolean associatedProviderDetectionExists() {
    return rule != null && rule.getProvider().getDetection() != null;
  }

  @CheckForNull
  public Matching getMatching() {
    if (matching == null && associatedProviderDetectionExists()) {
      return rule.getProvider().getDetection().getMatching();
    }
    return matching;
  }

  public void setMatching(@Nullable Matching matching) {
    this.matching = matching;
  }

  @CheckForNull
  public PreModule getPre() {
    if (pre == null && associatedProviderDetectionExists()) {
      return rule.getProvider().getDetection().getPre();
    }
    return pre;
  }

  public void setPre(@Nullable PreModule pre) {
    this.pre = pre;
  }

  @CheckForNull
  public TopLevelPostModule getPost() {
    if (post == null && associatedProviderDetectionExists()) {
      return rule.getProvider().getDetection().getPost();
    }
    return post;
  }

  public void setPost(@Nullable TopLevelPostModule post) {
    this.post = post;
  }

  public void setRule(Rule rule) {
    this.rule = rule;
  }
}
