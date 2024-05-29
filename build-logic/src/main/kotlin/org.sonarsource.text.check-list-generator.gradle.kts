import java.time.LocalDate
import kotlin.io.path.Path
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.FileFilterUtils

private val checksLocation = "org/sonar/plugins/secrets/checks"
private val checksPackage = "org.sonar.plugins.secrets.checks."
private val template =
    """
    //<LICENSE_HEADER>
    package org.sonar.plugins.secrets;

    import java.util.List;
    //<REPLACE-WITH-IMPORTS-OF-ALL-CHECKS>
    // This class was generated automatically, the generation logic can be found in
    // org.sonarsource.text.check-list-generator.gradle.kts
    public class SecretsCheckList {

      //<REPLACE-WITH-LIST-OF-CHECKS>

      public List<Class<?>> checks() {
        return SECRET_CHECKS;
      }
    }
    """.trimIndent()

tasks.register("generateSecretsCheckList") {
    description = "Generates SecretsCheckList class based on all checks"
    group = "build"
    inputs.files(Path("${project.projectDir}/src/main/java/$checksLocation/"))
    outputs.dir(Path("$buildDir/generated/sources/secrets/java/main"))

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

        val licenseHeader =
            rootProject.file("LICENSE_HEADER").readText(Charsets.UTF_8)
                .replace("\$YEAR", LocalDate.now().year.toString())
                .trimEnd()

        val result =
            template
                .replace("//<LICENSE_HEADER>", licenseHeader)
                .replace("//<REPLACE-WITH-IMPORTS-OF-ALL-CHECKS>", generateImportsFor(classNames))
                .replace("//<REPLACE-WITH-LIST-OF-CHECKS>", generateChecksMethodFor(classNames))

        val dir = File("$buildDir/generated/sources/secrets/java/main/org/sonar/plugins/secrets")
        dir.mkdirs()
        val file = File(dir, "SecretsCheckList.java")
        file.writeText(result + System.lineSeparator())
    }
}

fun generateImportsFor(checkNames: List<String>): String {
    val sb = StringBuilder()
    for (checkName in checkNames) {
        sb.append("import org.sonar.plugins.secrets.checks.")
        sb.append(checkName)
        sb.append(";")
        sb.append(System.lineSeparator())
    }
    return sb.toString()
}

fun generateChecksMethodFor(checkNames: List<String>): String {
    val sb = StringBuilder()
    sb.append("private static final List<Class<?>> SECRET_CHECKS = List.of(")
    sb.append(System.lineSeparator())
    for (i in checkNames.indices) {
        sb.append("    ")
        sb.append(checkNames[i])
        sb.append(".class")
        if (i == checkNames.size - 1) {
            sb.append(");")
        } else {
            sb.append(",")
            sb.append(System.lineSeparator())
        }
    }
    return sb.toString()
}
