plugins {
    `kotlin-dsl`
    java
    jacoco
}

dependencies {
    implementation(libs.jfrog.buildinfo.gradle) {
        exclude("ch.qos.logback", "logback-core")
    }
    implementation(libs.sonar.scanner.gradle)
    implementation(libs.diffplug.spotless)
    implementation(libs.diffplug.blowdryer)
    implementation(libs.commons.io)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.gradle.shadow)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
}

tasks.test {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
    }
}

plugins.withType<JacocoPlugin> {
    tasks["test"].finalizedBy("jacocoTestReport")
}
