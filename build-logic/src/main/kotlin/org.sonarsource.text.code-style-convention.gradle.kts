import com.diffplug.blowdryer.Blowdryer
import org.sonarsource.text.CodeStyleConvention

plugins {
    id("com.diffplug.spotless")
}

val codeStyleConvention = extensions.create<CodeStyleConvention>("codeStyleConvention")

spotless {
    encoding(Charsets.UTF_8)
    java {
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
    }
    format("javaMisc") {
        target("src/**/package-info.java")
        licenseHeaderFile(codeStyleConvention.licenseHeaderFile, "@javax.annotation").updateYearWithLatest(true)
    }
    codeStyleConvention.spotless?.invoke(this)
}

tasks.check { dependsOn("spotlessCheck") }
