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
package org.sonarsource.text

data class Constants(
    val packagePrefix: String,
    val generatedClassName: String,
) {
    val specFilesLocation get() = "$packagePrefix/sonar/plugins/secrets/configuration"
    val template
        get() =
            """
    //<LICENSE_HEADER>
    package $packagePrefix.sonar.plugins.secrets;

    import java.util.Set;
    
    // This class was generated automatically, the generation logic can be found in
    // org.sonarsource.text.specification-files-list-generator.gradle.kts
    public class $generatedClassName {

      public static Set<String> existingSecretSpecifications() {
        return Set.of(
    //<REPLACE-WITH-LIST-OF-FILES>
      }
    }
            """.trimIndent()
}
