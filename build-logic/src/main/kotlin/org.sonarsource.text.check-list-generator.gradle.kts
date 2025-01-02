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
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.FileFilterUtils
import org.sonarsource.text.CHECK_LIST_GENERATION_TASK_NAME
import org.sonarsource.text.CodeGenerationConfiguration
import org.sonarsource.text.GENERATED_SOURCES_DIR
import org.sonarsource.text.loadLicenseHeader
import org.sonarsource.text.toSnakeCase
import org.sonarsource.text.writeToFile

val codeGenerationConfiguration = extensions.findByType<CodeGenerationConfiguration>()
    ?: extensions.create<CodeGenerationConfiguration>("codeGeneration")

private data class Constants(
    val packagePrefix: String,
    val generatedClassName: String,
) {
    val checksLocation get() = "$packagePrefix/sonar/plugins/secrets/checks"
    val checksPackage get() = "$packagePrefix.sonar.plugins.secrets.checks."
    val template
        get() =
            """
    //<LICENSE_HEADER>
    package $packagePrefix.sonar.plugins.secrets;

    import java.util.List;
    //<REPLACE-WITH-IMPORTS-OF-ALL-CHECKS>
    // This class was generated automatically, the generation logic can be found in
    // org.sonarsource.text.check-list-generator.gradle.kts
    public class $generatedClassName {

      //<REPLACE-WITH-LIST-OF-CHECKS>

      public List<Class<?>> checks() {
        //<REPLACE-WITH-CHECKS-METHOD>
      }
          
    //<REPLACE-WITH-ADDITIONAL-METHODS>
    }
            """.trimIndent()
}

private val lineSeparator = "\n"

tasks.register(CHECK_LIST_GENERATION_TASK_NAME) {
    description = "Generates checks list class based on all checks"
    group = "build"

    val constants = with(codeGenerationConfiguration) {
        packagePrefix.zip(checkListClassName) { packagePrefix, generatedClassName ->
            Constants(packagePrefix, generatedClassName)
        }
    }

    inputs.dir(constants.map { "$projectDir/src/main/java/${it.checksLocation}/" })
    inputs.file(codeGenerationConfiguration.licenseHeaderFile)
    outputs.file(
        constants.flatMap {
            layout.buildDirectory.file("$GENERATED_SOURCES_DIR/${it.packagePrefix}/sonar/plugins/secrets/${it.generatedClassName}.java")
        }
    )

    doLast {
        val constants = constants.get()

        val filter = FileFilterUtils.and(
            FileFilterUtils.suffixFileFilter("Check.java"),
            FileFilterUtils.notFileFilter(FileFilterUtils.prefixFileFilter("Abstract"))
        )

        val files = FileUtils.listFiles(
            projectDir.resolve("src/main/java/${constants.checksLocation}/"),
            filter,
            FileFilterUtils.trueFileFilter()
        )

        val classNames = files
            .map { it.name.removeSuffix(".java") }
            .sorted()

        val result = with(codeGenerationConfiguration) {
            constants.template
                .replace("//<LICENSE_HEADER>", loadLicenseHeader(licenseHeaderFile.asFile.get()))
                .replace("//<REPLACE-WITH-IMPORTS-OF-ALL-CHECKS>", generateImportsFor(constants.checksPackage, classNames))
                .replace(
                    "//<REPLACE-WITH-LIST-OF-CHECKS>",
                    generateSecretChecksFieldFor(classNames, checkListClassesToEmbed.get())
                )
                .replace("//<REPLACE-WITH-CHECKS-METHOD>", generateChecksMethod(checkListClassesToEmbed.get()))
                .replace("//<REPLACE-WITH-ADDITIONAL-METHODS>", generateAdditionalMethods(codeGenerationConfiguration))
        }

        writeToFile(result, "${constants.packagePrefix}/sonar/plugins/secrets/${constants.generatedClassName}.java")
    }
}

fun generateImportsFor(
    checksPackage: String,
    checkNames: List<String>,
): String = checkNames.joinToString(lineSeparator, postfix = lineSeparator) { "import $checksPackage$it;" }

fun generateSecretChecksFieldFor(
    checkNames: List<String>,
    checkListClassesToEmbed: Set<String>,
): String =
    buildString {
        append("private static final List<Class<?>> SECRET_CHECKS = List.of($lineSeparator")
        append(checkNames.joinToString(",$lineSeparator", postfix = ");") { "    $it.class" })
        for (checkListName in checkListClassesToEmbed) {
            val checkListFieldName = checkListClassToFieldName(checkListName)
            append(lineSeparator)
            append("  private static final List<Class<?>> $checkListFieldName = new $checkListName().checks();")
        }
    }

fun generateChecksMethod(checkClassesToEmbed: Set<String>): String =
    buildString {
        append("var allChecks = new java.util.ArrayList<Class<?>>();$lineSeparator")
        for (checkClass in checkClassesToEmbed) {
            append("    allChecks.addAll(${checkListClassToFieldName(checkClass)});$lineSeparator")
        }
        append("    allChecks.addAll(SECRET_CHECKS);$lineSeparator")
        append("    return allChecks;")
    }

fun generateAdditionalMethods(codeGenerationConfiguration: CodeGenerationConfiguration): String =
    buildString {
        append("  public static List<Class<?>> getCurrentEditionChecks() { return SECRET_CHECKS; }$lineSeparator")
        for (checkClass in codeGenerationConfiguration.checkListClassesToEmbed.get()) {
            val checkClassName = checkClass.substringAfterLast('.')
            val checkListFieldName = checkListClassToFieldName(checkClass)
            append("  public static List<Class<?>> get${checkClassName}Checks() { return $checkListFieldName; }$lineSeparator")
        }
    }

private fun checkListClassToFieldName(checkListClass: String): String {
    return checkListClass.substringAfterLast('.').toSnakeCase().uppercase() + "_CHECKS"
}
