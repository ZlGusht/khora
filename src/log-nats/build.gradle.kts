plugins {
    `java-library`
}

dependencies {
    api(project(":kernel-core"))
    implementation(libs.jnats)
    implementation(libs.batch.publish)

    testImplementation(testFixtures(project(":kernel-core")))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testcontainers)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.slf4j.nop)
}

tasks.withType<Test> {
    systemProperty("khora.nats.conf", rootProject.file("nats/nats-server.conf").absolutePath)
    systemProperty("java.util.logging.config.file", file("src/test/resources/logging.properties").absolutePath)
}
