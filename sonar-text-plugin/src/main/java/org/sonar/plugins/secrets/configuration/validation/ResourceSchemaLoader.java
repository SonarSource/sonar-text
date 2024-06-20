/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
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
package org.sonar.plugins.secrets.configuration.validation;

import com.networknt.schema.AbsoluteIri;
import com.networknt.schema.resource.InputStreamSource;
import com.networknt.schema.resource.UriSchemaLoader;
import java.io.IOException;
import java.io.InputStream;

final class ResourceSchemaLoader extends UriSchemaLoader {

  private final String localSchemaFolder;

  public ResourceSchemaLoader(String localSchemaFolder) {
    this.localSchemaFolder = localSchemaFolder;
  }

  @Override
  public InputStreamSource getSchema(AbsoluteIri absoluteIri) {
    var resourcePath = localSchemaFolder + absoluteIri.toString();
    var resourceUrl = ResourceSchemaLoader.class.getResource(resourcePath);
    if (resourceUrl != null) {
      return new ResourceInputStreamSource(resourcePath);
    }

    return super.getSchema(absoluteIri);
  }

  final class ResourceInputStreamSource implements InputStreamSource {

    private final String resourcePath;

    public ResourceInputStreamSource(String resourcePath) {
      this.resourcePath = resourcePath;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return ResourceSchemaLoader.class.getResourceAsStream(resourcePath);
    }
  }
}
