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
package org.sonar.plugins.secrets.configuration.validation;

import com.networknt.schema.AbsoluteIri;
import com.networknt.schema.resource.InputStreamSource;
import com.networknt.schema.resource.IriResourceLoader;
import java.io.InputStream;

final class ResourceSchemaLoader extends IriResourceLoader {

  private final String localSchemaFolder;

  public ResourceSchemaLoader(String localSchemaFolder) {
    this.localSchemaFolder = localSchemaFolder;
  }

  @Override
  public InputStreamSource getResource(AbsoluteIri absoluteIri) {
    var resourcePath = localSchemaFolder + absoluteIri;
    var resourceUrl = ResourceSchemaLoader.class.getResource(resourcePath);
    if (resourceUrl != null) {
      return new ResourceInputStreamSource(resourcePath);
    }

    return super.getResource(absoluteIri);
  }

  record ResourceInputStreamSource(String resourcePath) implements InputStreamSource {

    @Override
    public InputStream getInputStream() {
      return ResourceSchemaLoader.class.getResourceAsStream(resourcePath);
    }
  }
}
