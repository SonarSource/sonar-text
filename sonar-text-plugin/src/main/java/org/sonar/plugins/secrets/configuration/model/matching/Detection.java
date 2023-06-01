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

package org.sonar.plugins.secrets.configuration.model.matching;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.matching.filter.PostModule;
import org.sonar.plugins.secrets.configuration.model.matching.filter.PreModule;

public class Detection {

  @Nullable
  private Match matching;
  @Nullable
  private PreModule pre;
  @Nullable
  private PostModule post;
  @JsonIgnore
  private Rule rule;

  private boolean associatedProviderDetectionExists() {
    return rule != null && rule.getProvider().getDetection() != null;
  }

  @CheckForNull
  public Match getMatching() {
    if (matching == null && associatedProviderDetectionExists()) {
      return rule.getProvider().getDetection().getMatching();
    }
    return matching;
  }

  public void setMatching(@Nullable Match matching) {
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
  public PostModule getPost() {
    if (post == null && associatedProviderDetectionExists()) {
      return rule.getProvider().getDetection().getPost();
    }
    return post;
  }

  public void setPost(@Nullable PostModule post) {
    this.post = post;
  }

  public void setRule(Rule rule) {
    this.rule = rule;
  }
}
