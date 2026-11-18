rootProject.name = "khora"

include(":kernel-core")
project(":kernel-core").projectDir = file("src/kernel-core")

include(":log-nats")
project(":log-nats").projectDir = file("src/log-nats")
