/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2025 SonarSource Sàrl
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
package org.sonar.plugins.secrets.configuration.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import org.sonar.plugins.secrets.configuration.model.matching.Detection;
import org.sonar.plugins.secrets.configuration.model.metadata.RuleMetadata;

public class Rule {

  private String id;
  private String rspecKey;
  private Selectivity selectivity = Selectivity.SPECIFIC;
  private RuleMetadata metadata;
  private Detection detection;
  private List<RuleExample> examples;
  @JsonIgnore
  private Provider provider;

  public Rule() {
    // default constructor to be used by Jackson
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getRspecKey() {
    return rspecKey;
  }

  public void setRspecKey(String rspecKey) {
    this.rspecKey = rspecKey;
  }

  public RuleMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(RuleMetadata metadata) {
    this.metadata = metadata;
    metadata.setRule(this);
  }

  public Detection getDetection() {
    return detection;
  }

  public void setDetection(Detection detection) {
    this.detection = detection;
    detection.setRule(this);
  }

  public List<RuleExample> getExamples() {
    return examples;
  }

  public void setExamples(List<RuleExample> examples) {
    this.examples = examples;
  }

  public Provider getProvider() {
    return provider;
  }

  public void setProvider(Provider provider) {
    this.provider = provider;
  }

  public Selectivity getSelectivity() {
    return selectivity;
  }

  public void setSelectivity(Selectivity selectivity) {
    this.selectivity = selectivity;
  }
}
