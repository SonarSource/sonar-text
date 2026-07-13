/*
 * SonarQube Text Plugin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.plugins.secrets.configuration.SecretsSpecificationContainer;
import org.sonar.plugins.secrets.utils.CheckContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.common.TestUtils.SONARQUBE_RUNTIME;
import static org.sonar.plugins.common.TestUtils.activeRules;
import static org.sonar.plugins.common.TestUtils.toRuleKeys;

class TextAndSecretsSensorTest extends AbstractTextAndSecretsSensorTest {

  private static final TestUtils TEST_UTILS = new TestUtils();

  @Override
  protected TextAndSecretsSensor sensor(Check... checks) {
    CheckFactory checkFactory = new CheckFactory(activeRules(toRuleKeys(checks)));
    return new TextAndSecretsSensor(SONARQUBE_RUNTIME, checkFactory, new SecretsSpecificationContainer(), new CheckContainer()) {
      @Override
      protected List<Check> getActiveChecks() {
        return Arrays.stream(checks).toList();
      }
    };
  }

  @Override
  protected TextAndSecretsSensor sensor(SensorContext sensorContext) {
    return new TextAndSecretsSensor(sensorContext.runtime(), new CheckFactory(sensorContext.activeRules()), new SecretsSpecificationContainer(),
      new CheckContainer());
  }

  @Override
  protected TestUtils testUtils() {
    return TEST_UTILS;
  }

  @Override
  protected String sensorName() {
    return "TextAndSecretsSensor";
  }

  @Test
  void shouldResolvePluginVersionFromClasspathResource() {
    assertThat(TextAndSecretsSensor.resolvePluginVersion(getClass().getClassLoader())).isEqualTo("1.2.3-test");
  }

  @Test
  void shouldFallBackToUnknownPluginVersionWhenResourceIsMissing() {
    ClassLoader emptyClassLoader = new URLClassLoader(new URL[0], null);
    assertThat(TextAndSecretsSensor.resolvePluginVersion(emptyClassLoader)).isEqualTo("unknown");
  }

  @Test
  void shouldFallBackToUnknownPluginVersionWhenResourceCannotBeRead() {
    ClassLoader brokenClassLoader = new ClassLoader() {
      @Override
      public InputStream getResourceAsStream(String name) {
        return new InputStream() {
          @Override
          public int read() throws IOException {
            throw new IOException("boom");
          }
        };
      }
    };
    assertThat(TextAndSecretsSensor.resolvePluginVersion(brokenClassLoader)).isEqualTo("unknown");
  }

  @Test
  void shouldFallBackToUnknownPluginVersionWhenPlaceholderWasNotSubstituted() {
    ClassLoader unsubstitutedClassLoader = new ClassLoader() {
      @Override
      public InputStream getResourceAsStream(String name) {
        return new ByteArrayInputStream("plugin.version=${version}".getBytes(StandardCharsets.UTF_8));
      }
    };
    assertThat(TextAndSecretsSensor.resolvePluginVersion(unsubstitutedClassLoader)).isEqualTo("unknown");
  }
}
