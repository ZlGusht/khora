plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    api(project(":events"))
    testFixturesApi(testFixtures(project(":events")))
    testFixturesApi(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:all")
}
