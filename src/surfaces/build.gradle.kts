plugins {
    `java-library`
}

dependencies {
    api(project(":events"))
    testImplementation(testFixtures(project(":events")))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:all")
}
