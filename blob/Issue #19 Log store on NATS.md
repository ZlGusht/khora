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
- The server runs with `sync: always`: an append is confirmed only once it is on disk ([[The log confirms a write only after it is on disk]]). `docker-compose.yml` and the Testcontainers setup both use it.
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