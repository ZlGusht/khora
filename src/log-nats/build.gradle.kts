plugins {
    `java-library`
}

dependencies {
    api(project(":events"))
    implementation(libs.jnats)
    implementation(libs.batch.publish)

    testImplementation(testFixtures(project(":events")))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.testcontainers)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.slf4j.nop)
}

tasks.withType<Test> {
    systemProperty("khora.nats.conf", rootProject.file("nats/nats-server-always-sync.conf").absolutePath)
    systemProperty("java.util.logging.config.file", file("src/test/resources/logging.properties").absolutePath)
}

// The benchmark, against the dedicated `nats-default-sync` service of docker-compose.yml:
//   ./gradlew :log-nats:bench --args='--url nats://localhost:4223 --monitor http://localhost:8223 --wipe'
// Not part of `test`. It deletes and recreates the log stream on the server it is given, so it
// refuses to run without --wipe. Point it only at a server whose log may be thrown away.
val bench = sourceSets.create("bench") {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += output + compileClasspath + sourceSets.main.get().runtimeClasspath
}

dependencies {
    "benchImplementation"(libs.jnats)
    "benchRuntimeOnly"(libs.slf4j.nop)
}

tasks.register<JavaExec>("bench") {
    group = "verification"
    description = "Measures the NATS log store on the server given by --url."
    classpath = bench.runtimeClasspath
    mainClass.set("khora.events.log.nats.LogBench")
    workingDir = rootProject.projectDir
    maxHeapSize = "2g"
}
