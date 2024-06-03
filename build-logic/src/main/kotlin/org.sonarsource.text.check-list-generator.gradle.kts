import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.FileFilterUtils
import org.sonarsource.text.GENERATED_SOURCES_DIR
import org.sonarsource.text.loadLicenseHeader
import org.sonarsource.text.writeToFile

private val checksLocation = "org/sonar/plugins/secrets/checks"
private val checksPackage = "org.sonar.plugins.secrets.checks."
private val lineSeparator = "\n"
private val generatedClassName = "SecretsCheckList"
private val template =
    """
    //<LICENSE_HEADER>
    package org.sonar.plugins.secrets;

    import java.util.List;
    //<REPLACE-WITH-IMPORTS-OF-ALL-CHECKS>
    // This class was generated automatically, the generation logic can be found in
    // org.sonarsource.text.check-list-generator.gradle.kts
    public class $generatedClassName {

      //<REPLACE-WITH-LIST-OF-CHECKS>

      public List<Class<?>> checks() {
        return SECRET_CHECKS;
      }
    }
    """.trimIndent()

tasks.register("generateSecretsCheckList") {
    description = "Generates $generatedClassName class based on all checks"
    group = "build"
    inputs.files("${project.projectDir}/src/main/java/$checksLocation/")
    inputs.file(rootProject.file("LICENSE_HEADER"))
    outputs.file(layout.buildDirectory.file("$GENERATED_SOURCES_DIR/org/sonar/plugins/secrets/$generatedClassName.java"))

    doLast {
        val filter =
            FileFilterUtils.and(
                FileFilterUtils.suffixFileFilter("Check.java"),
                FileFilterUtils.notFileFilter(FileFilterUtils.prefixFileFilter("Abstract"))
            )

        val files =
            FileUtils.listFiles(
                File("${project.projectDir}/src/main/java/$checksLocation/"),
                filter,
                FileFilterUtils.trueFileFilter()
            )

        val classNames =
            files.map { it.name.removeSuffix(".java") }
                .sorted()

        val result =
            template
                .replace("//<LICENSE_HEADER>", loadLicenseHeader())
                .replace("//<REPLACE-WITH-IMPORTS-OF-ALL-CHECKS>", generateImportsFor(classNames))
                .replace("//<REPLACE-WITH-LIST-OF-CHECKS>", generateChecksMethodFor(classNames))

        writeToFile(result, "org/sonar/plugins/secrets/$generatedClassName.java")
    }
}

fun generateImportsFor(checkNames: List<String>): String =
    checkNames.joinToString(lineSeparator, postfix = lineSeparator) { "import $checksPackage$it;" }

fun generateChecksMethodFor(checkNames: List<String>): String =
    buildString {
        append("private static final List<Class<?>> SECRET_CHECKS = List.of($lineSeparator")
        append(checkNames.joinToString(",$lineSeparator", postfix = ");") { "    $it.class" })
    }
