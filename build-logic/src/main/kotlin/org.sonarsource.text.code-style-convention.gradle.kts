/*
 * SonarQube Text Plugin
 * Copyright (C) 2021-2024 SonarSource SA
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
import com.diffplug.blowdryer.Blowdryer
import org.sonarsource.text.CodeStyleConvention

plugins {
    id("com.diffplug.spotless")
}

val codeStyleConvention = extensions.create<CodeStyleConvention>("codeStyleConvention")
val kotlinGradleDelimiter = "(package|import|plugins|pluginManagement|dependencyResolutionManagement|repositories) "

spotless {
    encoding(Charsets.UTF_8)
    java {
        importOrderFile(
            Blowdryer.immutableUrl(
                "https://raw.githubusercontent.com/SonarSource/sonar-developer-toolset/refs/heads/master/eclipse/sonar.importorder"
            )
        )
        removeUnusedImports()
        targetExclude("build/generated/sources/**")
        // point to immutable specific commit of sonar-formater.xml version 23
        eclipse("4.22")
            .withP2Mirrors(
                mapOf(
                    "https://download.eclipse.org/eclipse/" to "https://ftp.fau.de/eclipse/eclipse/"
                )
            )
            .configFile(
                Blowdryer.immutableUrl(
                    "https://raw.githubusercontent.com/SonarSource/sonar-developer-toolset/" +
                        "540ef32ba22c301f6d05a5305f4e1dbd204839f3/eclipse/sonar-formatter.xml"
                )
            )
        licenseHeaderFile(codeStyleConvention.licenseHeaderFile).updateYearWithLatest(true)
    }
    kotlinGradle {
        ktlint().setEditorConfigPath("$rootDir/.editorconfig")
        licenseHeaderFile(codeStyleConvention.licenseHeaderFile, kotlinGradleDelimiter).updateYearWithLatest(true)
    }
    format("javaMisc") {
        target("src/**/package-info.java")
        licenseHeaderFile(codeStyleConvention.licenseHeaderFile, "@javax.annotation").updateYearWithLatest(true)
    }
    codeStyleConvention.spotless?.invoke(this)
}

tasks.check { dependsOn("spotlessCheck") }
