plugins {
    // Downloads JDK 25 for the toolchain when none is installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "khora"

include(":kernel")
project(":kernel").projectDir = file("src/kernel")

include(":kernel-core")
project(":kernel-core").projectDir = file("src/kernel-core")

include(":log-nats")
project(":log-nats").projectDir = file("src/log-nats")
