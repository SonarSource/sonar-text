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
import javax.annotation.Nullable;
import org.sonar.plugins.secrets.configuration.model.Provider;
import org.sonar.plugins.secrets.configuration.model.Rule;
import org.sonar.plugins.secrets.configuration.model.matching.filter.PostModule;
import org.sonar.plugins.secrets.configuration.model.matching.filter.PreModule;

public class Detection {

  @Nullable
  private Matching matching;
  @Nullable
  private PreModule pre;
  @Nullable
  private PostModule post;

  // only one of them should be non null
  @JsonIgnore
  private Rule possibleParentRule;
  @JsonIgnore
  private Provider possibleParentProvider;

  @Nullable
  public Matching getMatching() {
    if (matching == null && possibleParentProvider != null && possibleParentProvider.getDetection() != null) {
      return possibleParentProvider.getDetection().getMatching();
    }
    return matching;
  }

  public void setMatching(@Nullable Matching matching) {
    this.matching = matching;
  }

  @Nullable
  public PreModule getPre() {
    if (pre == null && possibleParentProvider != null && possibleParentProvider.getDetection() != null) {
      return possibleParentProvider.getDetection().getPre();
    }
    return pre;
  }

  public void setPre(@Nullable PreModule pre) {
    this.pre = pre;
  }

  @Nullable
  public PostModule getPost() {
    if (post == null && possibleParentProvider != null && possibleParentProvider.getDetection() != null) {
      return possibleParentProvider.getDetection().getPost();
    }
    return post;
  }

  public void setPost(@Nullable PostModule post) {
    this.post = post;
  }

  public Rule getPossibleParentRule() {
    return possibleParentRule;
  }

  public void setPossibleParentRule(Rule possibleParentRule) {
    this.possibleParentRule = possibleParentRule;
  }

  public Provider getPossibleParentProvider() {
    return possibleParentProvider;
  }

  public void setPossibleParentProvider(Provider possibleParentProvider) {
    this.possibleParentProvider = possibleParentProvider;
  }
}
