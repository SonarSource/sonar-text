import kotlin.io.path.div
import org.sonarsource.text.CodeGenerationConfiguration
import org.sonarsource.text.GENERATED_SOURCES_DIR
import org.sonarsource.text.SPEC_LIST_GENERATION_TASK_NAME
import org.sonarsource.text.listSecretSpecificationFiles
import org.sonarsource.text.loadLicenseHeader
import org.sonarsource.text.writeToFile

val codeGenerationConfiguration = extensions.findByType<CodeGenerationConfiguration>()
    ?: extensions.create<CodeGenerationConfiguration>("codeGeneration")

private data class Constants(
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

tasks.register(SPEC_LIST_GENERATION_TASK_NAME) {
    description = "Generates spec files list class based on all specification files"
    group = "build"

    val constants = with(codeGenerationConfiguration) {
        packagePrefix.zip(specFileListClassName) { packagePrefix, generatedClassName ->
            Constants(packagePrefix, generatedClassName)
        }
    }

    inputs.dir(constants.map { "$projectDir/src/main/resources/${it.specFilesLocation}/" })
    inputs.file(codeGenerationConfiguration.licenseHeaderFile)
    outputs.file(
        constants.flatMap {
            layout.buildDirectory.file("$GENERATED_SOURCES_DIR/${it.packagePrefix}/sonar/plugins/secrets/${it.generatedClassName}.java")
        }
    )

    doLast {
        val constants = constants.get()

        val files = listSecretSpecificationFiles(projectDir.toPath() / "src/main/resources/${constants.specFilesLocation}")

        val result = constants.template
            .replace("//<LICENSE_HEADER>", loadLicenseHeader(codeGenerationConfiguration.licenseHeaderFile.asFile.get()))
            .replace("//<REPLACE-WITH-LIST-OF-FILES>", discoverSpecFiles(files))

        writeToFile(result, "${constants.packagePrefix}/sonar/plugins/secrets/${constants.generatedClassName}.java")
    }
}

fun discoverSpecFiles(files: List<File>): String {
    return files.joinToString(",\n", postfix = ");") { file ->
        "      \"${file.name}\""
    }
}
