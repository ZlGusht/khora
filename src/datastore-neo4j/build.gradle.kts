plugins {
    `java-library`
}

dependencies {
    api(project(":query"))
    api(libs.neo4j.driver)

    testImplementation(testFixtures(project(":query")))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.neo4j)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.slf4j.nop)
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:all")
}
