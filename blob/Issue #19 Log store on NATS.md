---
identity__uuid: 2ce2ac61-0b25-4f3d-8cad-10fb30407809
identity__type: particular
identity__created_at: 2026-11-18
identity__tags: []
relationships__is_a:
  - "[[Process Instance v1.0.0]]"
  - "[[Issue Loop v1.0.0]]"
relationships__in_process:
  - "[[Milestone Stage 1 kernel core]]"
relationships__in_context_of:
  - "[[Log]]"
properties__status: open
properties__started_at: 2026-11-18
properties__current_phase: Operate
properties__issue: 19
properties__author: agent:claude-opus-5-5
---
Run for GitHub issue #19 (K2), on branch `issue/19-log-store-nats`. Superpowers brainstorming is the Laydown.

## Loop Config

- **Domain:** Complicated. The candidates are known; the open point is whether the atomic batch works from Java.
- **Scope of Authority:** the Gradle build at the repository root, `src/kernel-core/` (the `LogStore` interface and its test fixtures only) and `src/log-nats/`, `docker-compose.yml`, this run and its Letters and Decisions. Nothing in `docs/adr/` until the log store Decision is accepted.
- **Reservoir capacity:** about two working days.

## Vector

- **Attractor:** a log on NATS JetStream behind a `LogStore` interface, with append latency and throughput measured, and one entity's history read from it.
- **Repulsor:** event types (K1), the append path's accept and reject logic (K3), Neo4j (K6), conditional writes (K9), frontiers, reads as actions.

## Design

### Intent

The kernel needs a log before anything else in stage 1 can be built: K3 appends to it and K6 materializes Neo4j from it. This run gives the kernel a working log on NATS JetStream, behind an interface, and measures it, so that the log store choice (plan D2, ADR-0YB open question) rests on numbers.

### Goals

- A `LogStore` interface in `kernel-core` that K3, K6 and B1 build on.
- `NatsLogStore`, on NATS JetStream, and `InMemoryLogStore`, for tests.
- A set of records appended together is stored all or none.
- One entity's history is read straight from the log.
- Append latency and throughput, and the time to read one entity's history, are measured and written into this run.

### Non-goals

- Event types, ids, hashing and the clock: K1. This run stores events as bytes with a few routing fields.
- Accepting or rejecting actions: K3.
- Neo4j: K6. Conditional writes: K9. Frontiers, reads as actions, cold storage, encryption: not now.
- Comparing several log stores. The team chose NATS; Kafka is not prototyped.

### Anti-goals

- The interface leaks NATS types, so K3 and K6 depend on NATS.
- The build is shaped around this run alone, so the next modules don't fit it.

### Constraints

- Java 25, Gradle (Kotlin DSL) with a Java 25 toolchain, so the build runs the same whatever Java launches Gradle.
- NATS Server 2.12 or later: atomic batch publish needs it.
- Events are never deleted (ADR-0YB decisions 7 and 10).
- Tests run with Docker available; nothing else is installed.

### Approaches

| Approach | Trade-off | Chosen |
| --- | --- | --- |
| **A. One NATS message per event; a set of records is one atomic batch** | One entity's history comes from a subject filter. Depends on atomic batch publish from Java (NATS 2.12, Synadia's Orbit.java), not yet tried; at most 1000 messages per batch. | Yes |
| B. One NATS message per append, holding all its records | Atomic by nature, no extra library. History needs an index, which K6 or B1 would provide. About 1 MB per message by default. | Fallback, if A fails the first step |
| Kafka | No conditional append; heavier to run. Its strengths, tiered storage and a Neo4j connector, are not needed now. | No |

The first step of the plan proves atomic batch publish from Java. If it fails or is unreliable, the run switches to B; only `log-nats` changes.

### Decided

The run's Decisions: [[The kernel's log runs on NATS JetStream]], [[The kernel is written in Java 25 and built with Gradle]], [[The log stores each event as one message and appends a set as one atomic batch]], [[The log confirms a write only after it is on disk]].


**Build layout.**
- One Gradle build at the repository root, Kotlin DSL, with a version catalog in `gradle/libs.versions.toml` and the Gradle wrapper committed.
- Modules under `src/`: `src/kernel-core` and `src/log-nats` now. `store-neo4j` and `cli` come with their own issues.
- `kernel-core` has no dependency on NATS. `log-nats` depends on `kernel-core`.

**The interface** (`kernel-core`):

```java
public interface LogStore extends AutoCloseable {
    /** Appends all records, or none. Returns their positions, in order. */
    List<Long> append(List<LogRecord> records);
    /** Records from a position on, in log order, at most max. */
    List<LogRecord> read(long fromPosition, int max);
    /** Every record about one entity, in log order. */
    List<LogRecord> history(String entityId);
    /** Delivers records from a position on, then new ones as they arrive. */
    Subscription subscribe(long fromPosition, Consumer<LogRecord> handler);
}

public record LogRecord(
    long position,            // assigned by the store; 0 before append
    String eventId,           // from K1; any unique string until then
    String eventType,         // "action" | "outcome" | "effect" | "observation"
    List<String> entityIds,   // entities an effect touches, primary first; empty otherwise
    byte[] body) {}           // the serialized event; the store never reads it
```

- A record's first entity is its **primary entity**: the entity of an `Attr` or `Kind` effect, the `from` of a `Rel`, the `entity` of a `Member`. *Builder's call.*
- `history(entityId)` returns the records whose primary entity it is. The full picture of an entity (relations that point to it, the action behind each effect) is a surface built from the log: B1 and K6, through causes from K1. *Builder's call.*
- A duplicate event id within the stream's deduplication window (2 minutes, the NATS default) is not stored again, and `append` returns the original positions, so a retry is idempotent. Uniqueness beyond the window comes from K1's content-hash ids. How the atomic batch treats a duplicate inside a batch is checked in step 1. *Builder's call.*

**On NATS** (`log-nats`):
- One stream, `KHORA_LOG`, file storage, subjects `khora.log.>`.
- No limits on age, count or size; deletes and purges denied.
- `allow_atomic` on.
- The server runs with `sync: always`: an append is confirmed only once it is on disk ([[The log confirms a write only after it is on disk]]). `docker-compose.yml` and the Testcontainers setup both use it. The setting is selectable per deployment: `docker-compose.yml` picks the server config from `KHORA_NATS_SYNC` (`always`, the default, or `default` for the NATS default sync).
- Subject per record: `khora.log.<eventType>.<entity>`. The entity id is base64url-encoded without padding, since NATS subject tokens cannot contain dots. A record without an entity uses `_`.
- The position is the stream sequence.
- The header `Nats-Msg-Id` carries the event id, so a retried append is deduplicated.
- An append of more than 1000 records is refused with an error. Bulk imports go through a payload (ADR-0YB decision 6), not here.
- `history(entityId)` reads with a subject filter on `khora.log.*.<primary entity>`.

**Tests.**
- JUnit 5. One abstract contract test class, run against both `InMemoryLogStore` and `NatsLogStore`. It is the seed of K5's suite.
- `InMemoryLogStore` and the contract test class are Gradle test fixtures of `kernel-core` (`java-test-fixtures`), so K3 and later modules test against them without depending on NATS. *Builder's call.*
- `NatsLogStore` tests start NATS with Testcontainers.
- Covered: order of positions; all-or-none append, including a batch the server refuses; history of one entity; subscribe from a position and then live; duplicate event id appended once; nothing is lost across a restart of NATS.

**Local run.** `docker-compose.yml` at the root with NATS 2.12 and JetStream on, data in a volume.

**Measurement.**
- Setup: one NATS server, one appender (the kernel is one writer per log), plus a run with 4 concurrent appenders for comparison. Each measurement is taken twice: with `sync: always`, the setting in use, and with the NATS default disk sync, to show what it costs. *Builder's call.*
- A Gradle task in `log-nats` runs a plain benchmark against the local NATS: append latency (p50, p99) for one record and for batches of 1 + 3 and 1 + 50, throughput in records per second, and history read time for an entity with 1 000 and 100 000 records in a log of a million.
- The results go into this run's body as a table. They become the evidence for the log store Decision.

### Left to the builder

- Package names, versions of the libraries, test layout.
- How the benchmark is written, as long as it reports the numbers above.
- How `subscribe` is implemented on JetStream (ordered consumer or otherwise).

### Fallback rule

The run switches to approach B if, by the end of step 1, an atomic batch from Java either cannot be published, or leaves part of a batch stored in the failure test (server refuses or is stopped mid-batch). Under B, `history()` scans the stream with no index, and is measured as such. *Builder's call.*

## Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** a `LogStore` on NATS JetStream behind an interface, with an in-memory twin, a shared contract test, and measured append latency, throughput and history reads.

**Architecture:** one Gradle build; `kernel-core` holds the interface and, as test fixtures, `InMemoryLogStore` and `LogStoreContract`; `log-nats` implements it on one JetStream stream with atomic batch publish (Orbit.java `batch-publish`). A benchmark source set in `log-nats` produces the numbers.

**Tech Stack:** Java 25, Gradle 9.9.1 (Kotlin DSL, wrapper, version catalog), JUnit 5, Testcontainers, jnats, `io.synadia:batch-publish`, NATS Server `nats:2.12-alpine` (2.12.1 or later, for de-duplication inside batches).

**Spec:** the Design section above.

### Global Constraints

- Java toolchain 25 in every module, whatever Java runs Gradle.
- `kernel-core` has no dependency on NATS or Orbit.
- Stream `KHORA_LOG`, subjects `khora.log.>`, file storage, no limits on age, count or size, deletes and purges denied, atomic publish allowed.
- The server syncs every write: `sync_interval: always` in the `jetstream` block, in `docker-compose.yml` and in tests.
- Subject per record: `khora.log.<eventType>.<base64url, no padding, of the primary entity>`, or `_` when there is no entity.
- Positions start at 1 and are the stream sequence.
- `Nats-Msg-Id` = event id. An append of more than 1000 records is refused, and nothing is stored.
- Package root: `khora.kernel` (builder's call). Commits as Isiooo, local only; nothing is pushed.

### Review Focus

1. Entity ids with `.`, `*`, `>`, spaces or Cyrillic letters: history still finds them, and no other entity's records. Test in Task 3.
2. A record whose body exceeds the server's max payload (1 MB default): append throws `LogStoreException`, and nothing from that append is stored. Test in Task 3.
3. NATS unreachable: append throws `LogStoreException` within 5 seconds instead of hanging. Test in Task 4.
4. An empty list: `append(List.of())` returns an empty list and stores nothing. Test in Task 1 (contract).
5. The same event id twice in one batch: stored once; both entries of the returned list name the same position. Test in Task 1 (contract).

---

### Task 1: Build, interface, in-memory store and contract test

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradlew`, `gradlew.bat`, `gradle/wrapper/*` (from `gradle wrapper --gradle-version 9.9.1`), add `.gradle/` and `build/` to `.gitignore`
- Create: `src/kernel-core/build.gradle.kts` (plugins `java-library`, `java-test-fixtures`)
- Create: `src/kernel-core/src/main/java/khora/kernel/log/{LogStore,LogRecord,Subscription,LogStoreException}.java`
- Create: `src/kernel-core/src/testFixtures/java/khora/kernel/log/{InMemoryLogStore,LogStoreContract}.java`
- Test: `src/kernel-core/src/test/java/khora/kernel/log/InMemoryLogStoreTest.java`

**Interfaces:**
- Produces:
  - `interface LogStore extends AutoCloseable` with `List<Long> append(List<LogRecord>)`, `List<LogRecord> read(long fromPosition, int max)`, `List<LogRecord> history(String entityId)`, `Subscription subscribe(long fromPosition, Consumer<LogRecord> handler)`, `void close()`.
  - `record LogRecord(long position, String eventId, String eventType, List<String> entityIds, byte[] body)`. The canonical constructor rejects an `eventType` other than `action`, `outcome`, `effect`, `observation`, and a null or blank `eventId`. Methods: `Optional<String> primaryEntity()`, `LogRecord withPosition(long)`, static `LogRecord unappended(String eventId, String eventType, List<String> entityIds, byte[] body)` (position 0). `equals` and `hashCode` compare `body` by content.
  - `interface Subscription extends AutoCloseable { void close(); }`.
  - `class LogStoreException extends RuntimeException`.
  - `abstract class LogStoreContract` with `protected abstract LogStore newStore()`, run by every implementation.

- [ ] **Step 1:** Create the root build: settings include `:kernel-core` with `projectDir = file("src/kernel-core")`; root build sets the Java 25 toolchain and `useJUnitPlatform()` for all subprojects; the catalog holds JUnit and, for later tasks, jnats, batch-publish and Testcontainers at their latest stable versions. Run `./gradlew help`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 2: Write the contract tests** in `LogStoreContract` (each a `@Test` method):
  - `appendAssignsIncreasingPositions`: append e1, e2, e3, then e4: `[1, 2, 3]` then `[4]`.
  - `readReturnsRecordsFromAPosition`: after e1..e5, `read(3, 10)` returns e3, e4, e5 with positions 3, 4, 5; `read(3, 2)` returns e3, e4.
  - `historyReturnsRecordsByPrimaryEntity`: records with entities `[a]`, `[b]`, `[a, b]`, `[]`; `history("a")` is the first and third; `history("b")` is the second only; `history("x")` is empty.
  - `appendOfMoreThan1000RecordsIsRefused`: 1001 records throw `LogStoreException`; `read(1, 2000)` is empty.
  - `emptyAppendStoresNothing`: `append(List.of())` returns `List.of()`; `read(1, 10)` is empty.
  - `duplicateEventIdIsStoredOnce`: append e1 → `[1]`; append e1 again → `[1]`; read shows one record.
  - `duplicateEventIdInsideOneBatchIsStoredOnce`: append `[e1, e1]` → `[1, 1]`; read shows one record.
  - `subscribeDeliversExistingThenNewRecords`: after e1, e2, `subscribe(2, …)` receives e2; append e3; it receives e3 within 5 seconds; after `close()`, append e4 is not received.
- [ ] **Step 3:** `InMemoryLogStoreTest extends LogStoreContract`, returning `new InMemoryLogStore()`. Run `./gradlew :kernel-core:test`. Expected: FAIL, the classes do not exist.
- [ ] **Step 4:** Implement the four main types and `InMemoryLogStore` (a synchronized list; duplicate ids kept in a map from event id to position).
- [ ] **Step 5:** Run `./gradlew :kernel-core:test`. Expected: all contract tests PASS.
- [ ] **Step 6: Commit** `Add the Gradle build and the LogStore interface with an in-memory store`.

### Task 2: Atomic batch spike against NATS

**Files:**
- Create: `src/log-nats/build.gradle.kts` (depends on `:kernel-core`, test fixtures of `:kernel-core` for tests, jnats, batch-publish, Testcontainers); include `:log-nats` in settings
- Create: `nats/nats-server-always-sync.conf` (named `nats-server.conf` until Task 5 renamed it; JetStream on, `store_dir: /data`, `sync_interval: always`); `docker-compose.yml` with service `nats` (`nats:2.12-alpine`, `-c` with that config, ports 4222 and 8222, a named volume on `/data`)
- Create: `src/log-nats/src/test/java/khora/kernel/log/nats/NatsTestServer.java`: a Testcontainers container of the same image and config, with `url()` and `stop()`/`start()` that keep the data directory
- Test: `src/log-nats/src/test/java/khora/kernel/log/nats/AtomicBatchSpikeTest.java`

**Interfaces:**
- Produces: `NatsTestServer` (used by Tasks 3 and 4), `nats/nats-server-always-sync.conf` (used by the benchmark).

- [ ] **Step 1: Write the spike tests**, each creating a fresh stream with the Global Constraints settings:
  - `batchOfFourIsStoredTogether`: an atomic batch of 4 messages on two subjects; the stream holds 4 messages with consecutive sequences.
  - `refusedBatchStoresNothing`: a batch whose third message exceeds the max payload; publishing fails; the stream holds 0 messages.
  - `duplicateMsgIdInsideBatch`: a batch with the same `Nats-Msg-Id` twice; record what is stored and what the ack says, and assert it.
  - `streamRefusesDelete`: deleting a message fails.
- [ ] **Step 2:** Run `./gradlew :log-nats:test --tests '*AtomicBatchSpikeTest'`. Expected: the first run fails only where a library call or setting is wrong; fix the setup, not the assertions.
- [ ] **Step 3:** Write the outcome into this run, under a new heading `## Spike result`: the library versions, what each test showed, and whether the Fallback rule applies. **Stop and report to Hristiyan Ganchov** before Task 3. If the Fallback rule applies, Tasks 3 to 5 use approach B.
- [ ] **Step 4: Commit** `Prove atomic batch publish to JetStream from Java`.

### Task 3: NatsLogStore: append, read, history

**Files:**
- Create: `src/log-nats/src/main/java/khora/kernel/log/nats/{NatsLogStore,Subjects}.java`
- Test: `src/log-nats/src/test/java/khora/kernel/log/nats/{NatsLogStoreTest,SubjectsTest}.java`

**Interfaces:**
- Consumes: `LogStore`, `LogRecord`, `LogStoreException`, `LogStoreContract` (Task 1); `NatsTestServer` (Task 2).
- Produces: `static NatsLogStore open(String natsUrl)` (creates or checks the stream; throws `LogStoreException` if an existing `KHORA_LOG` has other settings); `Subjects.forRecord(LogRecord): String`; `Subjects.historyFilter(String entityId): String`.

- [ ] **Step 1: Write failing tests:**
  - `SubjectsTest`: `forRecord` of an effect on `p:kitchen.ivanovi` is `khora.log.effect.` + base64url of the id; an action with no entity is `khora.log.action._`; ids `a.b`, `a*`, `a>`, `a b`, `кухня` all give one valid subject token.
  - `NatsLogStoreTest extends LogStoreContract`, `newStore()` opens on a fresh `NatsTestServer`. Plus `historyFindsEntitiesWithSpecialCharacters` (the ids above, one record each; each history has exactly its record) and `oversizedBodyStoresNothing` (two records, the second 2 MB: `LogStoreException`; `read(1, 10)` is empty).
- [ ] **Step 2:** Run `./gradlew :log-nats:test`. Expected: FAIL, `NatsLogStore` does not exist.
- [ ] **Step 3:** Implement `Subjects`, then `NatsLogStore.open`, `append` (refuse over 1000 before publishing; one atomic batch; positions from the acks; a duplicate's position from the ack's sequence), `read` and `history` (`Subjects.historyFilter` on `khora.log.*.<entity>`). Leave `subscribe` throwing `UnsupportedOperationException` and mark the subscribe contract test `@Disabled("Task 4")` in `NatsLogStoreTest` only.
- [ ] **Step 4:** Run `./gradlew :log-nats:test`. Expected: PASS, except the disabled test.
- [ ] **Step 5: Commit** `Add NatsLogStore: append, read and history`.

### Task 4: Subscribe, failures and restart

**Files:**
- Modify: `src/log-nats/src/main/java/khora/kernel/log/nats/NatsLogStore.java`
- Test: `src/log-nats/src/test/java/khora/kernel/log/nats/NatsLogStoreTest.java`

**Interfaces:**
- Consumes: Task 3.
- Produces: a complete `NatsLogStore`.

- [ ] **Step 1: Write failing tests:** remove the `@Disabled`; add
  - `recordsSurviveARestart`: append e1..e3, stop and start the server keeping its data, open a new store: `read(1, 10)` returns e1..e3 at positions 1..3.
  - `unreachableServerFailsFast`: open against a server, stop it, `append` of one record throws `LogStoreException` within 5 seconds.
  - `handlerExceptionDoesNotStopTheSubscription`: a handler that throws on e1 still receives e2.
- [ ] **Step 2:** Run `./gradlew :log-nats:test`. Expected: the new tests FAIL.
- [ ] **Step 3:** Implement `subscribe` (builder's choice of consumer) and the timeouts (connection and publish timeouts of at most 5 seconds).
- [ ] **Step 4:** Run `./gradlew test`. Expected: every test in both modules PASS.
- [ ] **Step 5: Commit** `Complete NatsLogStore: subscribe, timeouts and restart`.

### Task 5: Benchmark and numbers

**Files:**
- Create: `src/log-nats/src/bench/java/khora/kernel/log/nats/LogBench.java`; a `bench` source set and a `bench` `JavaExec` task in `src/log-nats/build.gradle.kts`, taking `--url`
- Create: `nats/nats-server-default-sync.conf` (as the other, without `sync_interval`); a second compose service `nats-default-sync` on port 4223 with its own volume

**Interfaces:**
- Consumes: `NatsLogStore.open`, `LogRecord.unappended`.

- [ ] **Step 1:** Implement `LogBench`. Each scenario starts from an empty stream, warms up for 1000 appends, then reports:
  - append latency p50 and p99, over 10 000 appends each, for batches of 1, 1 + 3 and 1 + 50 records, with one appender, and for batches of 1 + 3 with 4 concurrent appenders;
  - throughput in records per second for batches of 1 + 50 with one appender;
  - history read time (median of 20 reads) for an entity with 1 000 and one with 100 000 records, in a log of 1 000 000 records with 200-byte bodies.
  Output: one Markdown table per server.
- [ ] **Step 2:** Run `docker compose up -d`, then `./gradlew :log-nats:bench --args='--url nats://localhost:4222'` and `--args='--url nats://localhost:4223'`. Expected: two tables, no errors.
- [ ] **Step 3:** Write both tables into this run under `## Measurements`, with the machine (`sysctl -n machdep.cpu.brand_string`, memory, disk) and the versions.
- [ ] **Step 4: Commit** `Measure the NATS log store`.

## Spike result

Task 2, commit `8c8773e`. NATS Server 2.12.15 (`nats:2.12-alpine`), jnats 2.27.0, `io.synadia:batch-publish` 0.4.0. The server reports `sync_always: true`.

| Test | What NATS did |
| --- | --- |
| A batch of 4 on two subjects | Stored together at sequences 1 to 4. |
| A batch refused by the client (an oversized message) | jnats refuses it before anything reaches the server. 0 stored. |
| A batch refused by the server (message 3 over a 1024-byte limit on that test's stream) | 0 stored. The error arrives only at commit, as 10176 "batch is incomplete", not the real reason. |
| The same `Nats-Msg-Id` twice in one batch | The server refuses the whole batch (10201). 0 stored. |
| Delete a message | Refused. |

**The Fallback rule does not apply.** The run stays on approach A, confirmed by [[Hristiyan Ganchov]].

Consequences for Task 3:
- `NatsLogStore.append` removes duplicate event ids inside one batch before publishing, since the server refuses the whole batch otherwise (ledger ruling R1).
- A server-side refusal is reported as "batch is incomplete"; `LogStoreException` should say which append failed, not why the server refused it.

## Measurements

Task 5, commit `2047c80`. `./gradlew :log-nats:bench` against two local NATS servers from `docker-compose.yml`, one with `sync: always` (port 4222) and one with the NATS default sync (port 4223). Each scenario starts from an empty stream. 200-byte bodies. Latency is measured per append, after a warm-up of 1000 appends.

**Setup.** Apple M5 (10 cores), 24 GB, Apple SSD, macOS 27.0.1. Docker Desktop with 10 CPUs and 7.65 GiB. NATS Server 2.12.15, jnats 2.27.0, `io.synadia:batch-publish` 0.4.0, Java 25.

| Scenario | `sync: always` | NATS default sync |
| --- | --- | --- |
| Append 1 record | p50 1.00 ms, p99 1.79 ms | p50 0.58 ms, p99 0.76 ms |
| Append 1 + 3 records | p50 2.27 ms, p99 3.79 ms | p50 0.84 ms, p99 1.10 ms |
| Append 1 + 50 records | p50 17.9 ms, p99 23.6 ms | p50 1.23 ms, p99 1.99 ms |
| Append 1 + 3, 4 concurrent appenders | p50 7.53 ms, p99 11.0 ms; 2 130 records/s | p50 2.03 ms, p99 2.99 ms; 8 004 records/s |
| Throughput, batches of 1 + 50, one appender | 2 813 records/s | 39 994 records/s |
| Fill a log of 1 000 000 records, batches of 1 000 | 5 min 30 s (3 028 records/s) | 10.1 s (99 111 records/s) |
| History of an entity with 1 000 records, log of 1 000 000 | median 95.8 ms | median 97.2 ms |
| History of an entity with 100 000 records, log of 1 000 000 | median 309 ms | median 297 ms |

**What the numbers say.**
- A typical write, an outcome with three effects, is confirmed in about 2 ms with `sync: always`. Synchronous commit is not held back by the log.
- `sync: always` costs about 3 times at small batches and about 14 times at 1 + 50: the server appears to sync each message of an atomic batch separately. Bulk imports are where it hurts: a million records take 5.5 minutes instead of 10 seconds. This is why the sync is selectable ([[The log confirms a write only after it is on disk]]).
- Concurrent appenders share the cost: four of them reach about 25 % more records per second than one, with higher latency each.
- Reading one entity's history does not depend on the sync setting, but grows with the size of the log: 1 000 records take about 20 ms in a log of 200 000 and about 96 ms in a log of 1 000 000. A large log needs an index for history; that is K6 and B1.

**How far to trust them.**
- NATS ran inside Docker Desktop's Linux VM. Its disk sync goes to a virtual disk on APFS, so absolute `sync: always` numbers on a Linux server with a real disk may differ. The comparison between the two settings holds.
- One machine, one run per scenario.
- Latency is closed-loop: each appender sends the next append when the last one is confirmed. A stall counts as one slow sample.
- Throughput and fill times include building the records, so with the NATS default sync they are understated by a few per cent. This does not change the comparison.
- That the server syncs each message of an atomic batch separately is inferred from the numbers, not confirmed on the server.