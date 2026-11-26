plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testFixturesApi(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:all")
}
