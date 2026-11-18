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
    testImplementation(libs.testcontainers.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
    systemProperty("khora.nats.conf", rootProject.file("nats/nats-server.conf").absolutePath)
    testLogging {
        showStandardStreams = true
    }
}
