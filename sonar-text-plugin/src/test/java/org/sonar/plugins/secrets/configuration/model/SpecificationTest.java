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
package org.sonar.plugins.secrets.configuration.model;

import org.junit.jupiter.api.Test;
import org.sonar.plugins.secrets.configuration.deserialization.ReferenceTestModel;
import org.sonar.plugins.secrets.configuration.model.matching.Detection;
import org.sonar.plugins.secrets.configuration.model.metadata.ProviderMetadata;
import org.sonar.plugins.secrets.configuration.model.metadata.RuleMetadata;

import static org.assertj.core.api.Assertions.assertThat;

class SpecificationTest {

  @Test
  void providerMetadataShouldBeRetrievedWhenRuleMetadataFieldsAreNull() {
    Specification specification = ReferenceTestModel.constructReferenceSpecification();
    RuleMetadata ruleMetadata = specification.getProvider().getRules().get(0).getMetadata();
    ReferenceTestModel.setSpecificMetadataFieldsNull(ruleMetadata);

    ProviderMetadata providerMetadata = specification.getProvider().getMetadata();

    assertThat(ruleMetadata.getFix()).isEqualTo(providerMetadata.getFix());
    assertThat(ruleMetadata.getMessage()).isEqualTo(providerMetadata.getMessage());
    assertThat(ruleMetadata.getImpact()).isEqualTo(providerMetadata.getImpact());
    assertThat(ruleMetadata.getReferences()).isEqualTo(providerMetadata.getReferences());
  }

  @Test
  void metadataFieldsShouldBeNullAndNotThrowException() {
    Specification specification = ReferenceTestModel.constructReferenceSpecification();
    RuleMetadata ruleMetadata = specification.getProvider().getRules().get(0).getMetadata();
    ReferenceTestModel.setSpecificMetadataFieldsNull(ruleMetadata);
    ReferenceTestModel.setSpecificMetadataFieldsNull(specification.getProvider().getMetadata());

    assertThat(ruleMetadata.getFix()).isNull();
    assertThat(ruleMetadata.getMessage()).isNull();
    assertThat(ruleMetadata.getImpact()).isNull();
    assertThat(ruleMetadata.getReferences()).isNull();
  }

  @Test
  void providerDetectionShouldBeRetrievedWhenRuleDetectionFieldsAreNull() {
    Specification specification = ReferenceTestModel.constructReferenceSpecification();
    Detection ruleDetection = specification.getProvider().getRules().get(0).getDetection();
    ReferenceTestModel.setDetectionFieldsNull(ruleDetection);

    Detection providerDetection = ReferenceTestModel.constructBasicDetection("\\b(provider matching pattern)\\b");
    ReferenceTestModel.enrichDetection(providerDetection);
    specification.getProvider().setDetection(providerDetection);

    assertThat(ruleDetection.getMatching()).isEqualTo(providerDetection.getMatching());
    assertThat(ruleDetection.getPre()).isEqualTo(providerDetection.getPre());
    assertThat(ruleDetection.getPost()).isEqualTo(providerDetection.getPost());
  }

  @Test
  void detectionFieldsShouldBeNullAndNotThrowException() {
    Specification specification = ReferenceTestModel.constructReferenceSpecification();
    Detection ruleDetection = specification.getProvider().getRules().get(0).getDetection();
    ReferenceTestModel.setDetectionFieldsNull(ruleDetection);
    ReferenceTestModel.setDetectionFieldsNull(specification.getProvider().getDetection());

    assertThat(ruleDetection.getMatching()).isNull();
    assertThat(ruleDetection.getPre()).isNull();
    assertThat(ruleDetection.getPost()).isNull();
  }
}
