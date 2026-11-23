plugins {
    `java-library`
}

dependencies {
    api(project(":kernel-core"))
    testImplementation(testFixtures(project(":kernel-core")))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:all")
}
