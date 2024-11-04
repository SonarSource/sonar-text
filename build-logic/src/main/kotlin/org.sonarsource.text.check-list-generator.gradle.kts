import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.FileFilterUtils
import org.sonarsource.text.CHECK_LIST_GENERATION_TASK_NAME
import org.sonarsource.text.CodeGenerationConfiguration
import org.sonarsource.text.GENERATED_SOURCES_DIR
import org.sonarsource.text.loadLicenseHeader
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
        return SECRET_CHECKS;
      }
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

        val result = constants.template
            .replace("//<LICENSE_HEADER>", loadLicenseHeader(codeGenerationConfiguration.licenseHeaderFile.asFile.get()))
            .replace("//<REPLACE-WITH-IMPORTS-OF-ALL-CHECKS>", generateImportsFor(constants.checksPackage, classNames))
            .replace("//<REPLACE-WITH-LIST-OF-CHECKS>", generateChecksMethodFor(classNames))

        writeToFile(result, "${constants.packagePrefix}/sonar/plugins/secrets/${constants.generatedClassName}.java")
    }
}

fun generateImportsFor(
    checksPackage: String,
    checkNames: List<String>,
): String = checkNames.joinToString(lineSeparator, postfix = lineSeparator) { "import $checksPackage$it;" }

fun generateChecksMethodFor(checkNames: List<String>): String =
    buildString {
        append("private static final List<Class<?>> SECRET_CHECKS = List.of($lineSeparator")
        append(checkNames.joinToString(",$lineSeparator", postfix = ");") { "    $it.class" })
    }
