plugins {
    // Downloads JDK 25 for the toolchain when none is installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "khora"

include(":events")
project(":events").projectDir = file("src/events")

include(":surfaces")
project(":surfaces").projectDir = file("src/surfaces")

include(":log-nats")
project(":log-nats").projectDir = file("src/log-nats")

include(":query")
project(":query").projectDir = file("src/query")

include(":datastore-neo4j")
project(":datastore-neo4j").projectDir = file("src/datastore-neo4j")
