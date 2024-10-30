import org.sonarsource.text.GENERATED_SOURCES_DIR
import org.sonarsource.text.listSecretSpecificationFiles
import org.sonarsource.text.loadLicenseHeader
import org.sonarsource.text.specFilesLocation
import org.sonarsource.text.writeToFile

private val lineSeparator = "\n"
private val generatedClassName = "SecretsSpecificationFilesDefinition"
private val template =
    """
    //<LICENSE_HEADER>
    package org.sonar.plugins.secrets;

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

tasks.register("generateSecretsSpecFilesList") {
    description = "Generates $generatedClassName class based on all specification files"
    group = "build"
    inputs.files("${project.projectDir}/src/main/resources/$specFilesLocation/")
    inputs.file(rootProject.file("LICENSE_HEADER"))
    outputs.file(layout.buildDirectory.file("$GENERATED_SOURCES_DIR/org/sonar/plugins/secrets/$generatedClassName.java"))

    doLast {
        val files = listSecretSpecificationFiles("$projectDir", "src/main/resources/$specFilesLocation")

        val result =
            template
                .replace("//<LICENSE_HEADER>", loadLicenseHeader())
                .replace("//<REPLACE-WITH-LIST-OF-FILES>", discoverSpecFiles(files))

        writeToFile(result, "org/sonar/plugins/secrets/$generatedClassName.java")
    }
}

fun discoverSpecFiles(files: List<File>): String {
    return files.joinToString(",\n", postfix = ");") { file ->
        "      \"${file.name}\""
    }
}
