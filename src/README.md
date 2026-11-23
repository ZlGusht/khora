# khora code

Java 25 everywhere: the code is compiled and tested with Java 25, and the Gradle daemon runs on Java 25. Gradle downloads JDK 25 by itself when the machine has none.

## Modules

| Module | Package | What it holds |
| --- | --- | --- |
| `kernel` | `khora.kernel.surface`, `khora.kernel.event`, `khora.kernel.stub` | The surface contract: the call context, frontiers, reads and writes, and an in-memory stub surface. No dependencies. |
| `kernel-core` | `khora.kernel.log` | `LogStore`, `LogRecord`, `Subscription`, `LogStoreException`. Test fixtures: `InMemoryLogStore` and `LogStoreContract`, the tests every store must pass. No NATS dependency. |
| `log-nats` | `khora.kernel.log.nats` | `NatsLogStore`: the log on NATS JetStream. Tests run against real NATS in Docker. `bench` source set: `LogBench`. |

Design, decisions and measurements: the runs `blob/Issue #21 Surface contract types.md` (`kernel`) and `blob/Issue #19 Log store on NATS.md` (`kernel-core`, `log-nats`).

## Prerequisites

- **Docker**, running. The `log-nats` tests start NATS 2.12.15 in containers.
- **Any Java 17 or later** to start Gradle's wrapper. Gradle then switches to Java 25 for its daemon and the build, and downloads JDK 25 into `~/.gradle/jdks` if it is missing. Installing JDK 25 yourself (e.g. Temurin 25) avoids the download.
- Nothing else. Gradle 9.9.1 comes with the wrapper.

Java 25 is fixed in two places; change both together:

| File | Sets |
| --- | --- |
| `build.gradle.kts` | the toolchain: the Java the code is compiled and tested with |
| `gradle/gradle-daemon-jvm.properties` | the Java the Gradle daemon runs on (regenerate with `./gradlew updateDaemonJvm --jvm-version=25`) |

## First run

```sh
git clone https://github.com/ZlGusht/khora.git
cd khora
./gradlew test
```

The first run downloads Gradle, and JDK 25 if needed. The tests take a few minutes: most of them start their own NATS container.

On macOS, keep the machine awake for long runs (`caffeinate -i ./gradlew test`): when the Mac sleeps, Docker pauses and the tests stall.

## Using the log from code

```java
try (LogStore log = NatsLogStore.open("nats://localhost:4222")) {
    List<Long> positions = log.append(List.of(
        LogRecord.unappended("a1", "action",  List.of(),               actionJson),
        LogRecord.unappended("o1", "outcome", List.of(),               outcomeJson),
        LogRecord.unappended("e1", "effect",  List.of("p:kitchen-1"),  effectJson)));

    List<LogRecord> history = log.history("p:kitchen-1");

    try (Subscription s = log.subscribe(1, record -> apply(record))) {
        // records from position 1 on, then new ones as they arrive
    }
}
```

What the store promises is in the Javadoc of `LogStore`. In short:

- `append` stores all records or none. An exception does not always mean "not stored": after a timeout the batch may be stored whole. Retrying with the same event ids within 2 minutes is safe and returns the original positions.
- A duplicate event id is caught within the deduplication window (2 minutes on NATS). Beyond it, uniqueness comes from content-hash event ids (K1).
- More than 1000 records in one append are refused. Event ids are printable ASCII, with no space at either end.
- A subscription may stop on a record it cannot read; the store logs the position.

For tests of code that uses the log, depend on `kernel-core`'s test fixtures and use `InMemoryLogStore`. To check a new `LogStore` implementation, extend `LogStoreContract`.

## Run NATS locally

```sh
docker compose up -d        # NATS on localhost:4222, monitoring on localhost:8222
docker compose down         # stop; add -v to delete the stored log
```

`KHORA_NATS_SYNC` picks the disk sync of the `nats` service:

- `always` (default): every write is on disk before it is acknowledged.
- `default`: NATS's default sync. Faster, but a host crash can lose recent acknowledged writes.

```sh
KHORA_NATS_SYNC=default docker compose up -d nats
```

The ports are bound to 127.0.0.1 and there is no authentication: this is a development setup.

## Benchmark

The benchmark deletes the log stream on the server it is given, so it refuses to run without `--wipe`. Run it against the separate `nats-default-sync` service (port 4223), never against a log you keep:

```sh
docker compose up -d nats-default-sync
./gradlew :log-nats:bench --args='--url nats://localhost:4223 --monitor http://localhost:8223 --wipe'
```

Results are printed and written to `src/log-nats/build/bench/`. A full run takes about 10 minutes per server.

## For agents (Claude Code)

- Start every session with `docker info`. If Docker is not running, ask the person to start Docker Desktop; do not work around it.
- Verify with `./gradlew test`. Pass `--rerun-tasks` when you need the tests to run again although nothing changed.
- Keep test output quiet: no `System.out`, and no logging below WARNING in tests.
- Never run the benchmark against port 4222 or without the person's go-ahead: it deletes the stream.
- Do not change the Java version, the NATS image tag (`nats:2.12.15-alpine`) or the stream settings without a Decision: the duplicate handling in `NatsLogStore` depends on NATS 2.12.15 behaviour that `AtomicBatchSpikeTest` pins.
- Repository rules (Decisions, `blob/`, loop runs) are in `CLAUDE.md` at the root.
