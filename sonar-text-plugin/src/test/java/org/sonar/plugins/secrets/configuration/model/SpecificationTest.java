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
package org.sonar.plugins.secrets.configuration.model;

import org.junit.jupiter.api.Test;
import org.sonar.plugins.secrets.configuration.deserialization.ReferenceTestModel;
import org.sonar.plugins.secrets.configuration.model.matching.Detection;
import org.sonar.plugins.secrets.configuration.model.metadata.ProviderMetadata;
import org.sonar.plugins.secrets.configuration.model.metadata.RuleMetadata;

import static org.assertj.core.api.Assertions.assertThat;

class SpecificationTest {

  @Test
  void shouldRetrieveProviderMetadataWhenMessageIsNull() {
    Specification specification = ReferenceTestModel.constructReferenceSpecification();
    RuleMetadata ruleMetadata = specification.getProvider().getRules().get(0).getMetadata();
    ReferenceTestModel.setMetadataMessageNull(ruleMetadata);

    ProviderMetadata providerMetadata = specification.getProvider().getMetadata();

    assertThat(ruleMetadata.getMessage()).isEqualTo(providerMetadata.getMessage());
  }

  @Test
  void shouldNotThrowExceptionWhenProviderMetadataMessageIsNull() {
    Specification specification = ReferenceTestModel.constructReferenceSpecification();
    RuleMetadata ruleMetadata = specification.getProvider().getRules().get(0).getMetadata();
    ReferenceTestModel.setMetadataMessageNull(ruleMetadata);
    ReferenceTestModel.setMetadataMessageNull(specification.getProvider().getMetadata());

    assertThat(ruleMetadata.getMessage()).isNull();
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
