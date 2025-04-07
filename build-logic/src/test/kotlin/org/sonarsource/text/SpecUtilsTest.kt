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
package org.sonarsource.text

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class SpecUtilsTest {
    @Test
    fun `should expand common block`() {
        val commonSections = mapOf(
            "common1" to """
            line1
            line2
            """.trimIndent(),
            "common2" to """
            - line3
            - line4
            """.trimIndent()
        )

        val input = """
        key0:
            ${"$"}{common/common1}
        ${"$"}{common/common2}
        """.trimIndent()

        val expected = """
        key0:
            line1
            line2
        - line3
        - line4
        """.trimIndent()

        val result = input.lines().joinToString("\n") { it.expandCommonBlock(commonSections) }

        Assertions.assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `should throw error if referencing missing block`() {
        val commonSections = emptyMap<String, String>()

        val input = """
        key0:
            ${"$"}{common/common1}
        """.trimIndent()

        Assertions.assertThatThrownBy {
            input.lines().joinToString("\n") { it.expandCommonBlock(commonSections) }
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Missing file [common1] in configuration/common")
    }
}
