# khora code

Java 25 everywhere: the code is compiled and tested with Java 25, and the Gradle daemon runs on Java 25. Gradle downloads JDK 25 by itself when the machine has none.

## Modules

| Module | Package | What it holds |
| --- | --- | --- |
| `events` | `khora.events`, `khora.events.json`, `khora.events.log` | Events and the log. An `Event` is a fact by whoever emitted it, and names at most one other event, its trigger: the event it responds to. An emitted action, whose asserts are tuples `(identity, attribute, value)`, is its one kind so far. Ids are salted hashes of canonical JSON; recorded time is a hybrid logical clock. `Statements` is one reading of events, the reference for datastores. `EventLog` stores events; it does not judge them. Test fixtures: `InMemoryEventLog` and `EventLogContract`, the tests every log must pass. No dependencies. |
| `surfaces` | `khora.surfaces`, `khora.surfaces.stub` | Surfaces, in the language of events: a `Surface` receives the events its interests match, reads them its own way, and emits actions in response, each triggered by what it received; a `Gate` guards a shape that needs prevention, and may refuse an action on it before it becomes an event; the `Kernel` turns actions into events in an `EventLog` and delivers them. `Projects` is a stub gate for project names; `Entities` a reference surface that answers reads. Test fixtures (`khora.surfaces.contract`): `GateContract` and `SurfaceContract`, the tests every gate and surface must pass, as scenarios through the kernel; `Echo`, a reference surface. |
| `log-nats` | `khora.events.log.nats` | `NatsEventLog`: the event log on NATS JetStream. Tests run against real NATS in Docker. `bench` source set: `LogBench`. |
| `query` | `khora.query` | Searches as data: a `Query` is an envelope (log position, set or multiset, returned columns, order, page) around a body of operators, `Scan`, `Filter`, `Not` and `Aggregate`, checked when it is built. Its canonical JSON is stored as the value of the `khora/query` attribute. A `Datastore` answers queries; `EventsDatastore` works answers out from a log's events, through `Evaluator` over a `Graph`. Test fixture: `DatastoreContract`, the tests every datastore must pass. Depends on `events`. |
| `datastore-neo4j` | `khora.query.neo4j` | The graph datastore on Neo4j: `Neo4jDatastore` takes in a log's events (`catchUp`, `follow`) and keeps what they assert as it stands now. An entity is a node, its literal attributes are properties, and a reference is a relationship; a retraction deletes what its target asserted. Events are not stored. It answers queries at everything it has taken in, through `Evaluator`. A `Snapshot` is its content at a position, as JSON lines; `rebuild` restores one and catches up with the log. Tests run against real Neo4j in Docker. Depends on `query`. |

## Layout and build files

A path like `src/events/src/main/java/khora/events/Event.java` reads in four parts:

| Part | What it is |
| --- | --- |
| `src/` | the repository's code folder |
| `events/` | the Gradle module |
| `src/main/java/` | Gradle's place for a module's main code; tests are in `src/test/java/`, test fixtures in `src/testFixtures/java/` |
| `khora/events/` | the Java package, `khora.events` |

The build files are Gradle scripts in the Kotlin DSL (`.kts`); the code itself is all Java.

| File | Says |
| --- | --- |
| `settings.gradle.kts` | which modules exist, and the folder of each |
| `build.gradle.kts` (root) | what every module shares: the Java 25 toolchain, JUnit, Maven Central |
| `src/<module>/build.gradle.kts` | the module's own plugins and dependencies |
| `gradle/libs.versions.toml` | the versions of outside libraries, named as `libs.<name>` |

How modules depend on each other:

- `events` depends on nothing.
- `surfaces`, `log-nats` and `query` depend on `events`, and not on each other. So the kernel runs on any `EventLog`, and only `log-nats` sees NATS.
- `datastore-neo4j` depends on `query`.

The words the module build files use:

- `api(…)`: a dependency whose types appear in this module's public types, so modules using this one see it too.
- `implementation(…)`: a dependency used only inside this module.
- `testFixtures`: code other modules' tests may use, such as `InMemoryEventLog`. `java-test-fixtures` makes it; `testImplementation(testFixtures(project(":events")))` uses it.

To add a module: create `src/<module>/build.gradle.kts`, add two lines to `settings.gradle.kts` as for the others, and depend on `events` with `api` or `implementation`.

Design, decisions and measurements: the runs `blob/Issue #18 Event types ids and clock.md` (`events`, `surfaces`), `blob/Issue #21 Surface contract types.md`, `blob/Issue #19 Log store on NATS.md` (`log-nats`), `blob/Issue #22 Contract tests for surfaces and authorities.md` (the contracts, before gates), `blob/Issue #25 Query as a data structure.md` (`query`) and `blob/Issue #23 Graph datastore on Neo4j.md` (`datastore-neo4j`).

## Prerequisites

- **Docker**, running. The `log-nats` tests start NATS 2.12.15 in containers; the `datastore-neo4j` tests start Neo4j 2026.10.2 community in one container.
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
try (EventLog log = NatsEventLog.open("nats://localhost:4222")) {
    var anna = new By("anna", Optional.empty(), "g3", "kernel");
    var created = Event.create(Optional.empty(), Hlc.system().next(), Optional.empty(), new Emitted(anna,
        Assertion.action(Assertion.entity(),          // /0: a new thing
            Assertion.of(Identity.local(0), Identity.named("name"), new Assertion.Literal(new Json.Str("Kitchen"))))));
    List<Long> positions = log.append(List.of(created));

    Identity project = Identity.of(created.id(), 0);
    List<Event> about = log.about(project);              // the events about the project

    try (Following f = log.follow(1, logged -> apply(logged.event()))) {
        // events from position 1 on, then new ones as they arrive
    }
}
```

Most code goes through the `Kernel` instead, which asks the gates that guard an action, appends and delivers. What the log promises is in the Javadoc of `EventLog`. In short:

- `append` stores all events or none. An exception does not always mean "not stored": after a timeout the batch may be stored whole. Appending the same events again is safe.
- An event is stored once: within NATS's deduplication window (2 minutes) by its id; beyond it, ids are salted content hashes, so different events never share one.
- More than 1000 events in one append are refused.
- `about` reads the whole log, a page at a time: there is no index yet.
- Following may stop on an event it cannot read; the log logs the position.

For tests of code that uses the log, depend on the `events` module's test fixtures and use `InMemoryEventLog`. To check a new `EventLog`, extend `EventLogContract`; to check a new `Datastore`, extend `DatastoreContract` from the `query` module's test fixtures.

## Using the graph datastore from code

```java
try (var store = Neo4jDatastore.open("bolt://localhost:7687", "neo4j", "khora-dev-password");
        Following f = store.follow(log)) {         // takes in the log, then each new event
    Answer open = store.answer(Query.of(Scan.things("t")));
    store.snapshot().write(Path.of("graph.jsonl"));   // its content at a position
}
```

- The datastore owns its database: `clear()` and `restore` delete what it holds.
- It answers only at everything it has taken in. A query at any other position is refused; it neither waits nor reads the past.
- Attribute identities are used as they are, as property keys and relationship types.
- `rebuild(Snapshot.read(file), log)` restores a snapshot and takes in the rest of the log. It refuses a snapshot of another log.

To check a new `Gate` or `Surface`, depend on the `surfaces` module's test fixtures and extend `GateContract` or `SurfaceContract` with your scenarios (`Scenario.named(...).given(...).when(...).then(...)`); each scenario is also held to the contract's general rules. `ProjectsContractTest` and `EchoContractTest` are examples. Some things to know when writing scenarios:

- A rename says what it replaces: a retraction of the statement it read, `TupleSpec.retraction(Ref.tuple(step, 1))`. A step in response to another names it with `.after(step)`.
- A thing's first tuple, `/0`, is the entity itself, so its first attribute is `Ref.tuple(step, 1)`.
- Give `Then.refused` the reason text; without it, a scenario may pass for the wrong reason.
- `SurfaceContract` checks what a surface receives and emits, not what a view shows.

## Run NATS locally

```sh
docker compose up -d        # NATS on localhost:4222, monitoring on localhost:8222; Neo4j on localhost:7687
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

- Start every session with `docker info`. If Docker is not running, ask the person to start Docker Desktop; do not work around it. On Linux the user must be in the `docker` group; a Gradle daemon started before that cannot reach Docker (`./gradlew --stop`).
- Verify with `./gradlew test`. Pass `--rerun-tasks` when you need the tests to run again although nothing changed.
- Keep test output quiet: no `System.out`, and no logging below WARNING in tests.
- Never run the benchmark against port 4222 or without the person's go-ahead: it deletes the stream.
- Do not change the Java version, the NATS image tag (`nats:2.12.15-alpine`) or the stream settings without a Decision: the duplicate handling in `NatsEventLog` depends on NATS 2.12.15 behaviour that `AtomicBatchSpikeTest` pins.
- Repository rules (Decisions, `blob/`, loop runs) are in `CLAUDE.md` at the root.
