---
identity__uuid: a377a333-f696-45b2-8ab9-5d6b21dc04ee
identity__type: particular
identity__created_at: 2026-11-25
identity__tags: []
relationships__is_a:
  - "[[Process Instance v1.0.0]]"
  - "[[Issue Loop v1.0.0]]"
relationships__in_process:
  - "[[Milestone Stage 1 kernel core]]"
relationships__in_context_of:
  - "[[Surfaces]]"
  - "[[Surface logic]]"
properties__status: closed
properties__started_at: 2026-11-25
properties__current_phase: Process(v)
properties__ended_at: 2026-11-25
properties__issue: 23
properties__author: agent:claude-opus-5-5
---
Issue #23 (plan task K6) of [[Milestone Stage 1 kernel core]]: the graph datastore, materialized from the events of the log, on Neo4j. It answers K8's `Query` ([[Stage 1's tasks and gate are stated in events]], item 2). Run by an agent in one go, on Simeon Petkov's request, built on the Issue #25 branch (pull request #39).

The issue speaks of effects; the event kernel keeps none (`Effects are not a notion of their own`). Here "materialized from effects" reads as: materialized from the statements of accepted events.

## Loop Config

- **Domain:** Complicated. The contract fixes the results (ADR-0XY decision 10: materializing changes speed, never results); the work is the graph's shape, reads at any frontier, and snapshots.
- **Scope of Authority:** a new `datastore-neo4j` module under `src/`, `settings.gradle.kts`, `gradle/libs.versions.toml`, `docker-compose.yml`, `src/README.md`, and this run's entities in `blob/`. Nothing in `docs/`, `events` or `query`.
- **Reservoir capacity:** one pull request.

## Vector

- **Attractor:** a `Datastore` on Neo4j that takes in the log's events, keeps things and statements as a graph, answers every query at any frontier it holds, passes `DatastoreContract`, and rebuilds from a snapshot to the same state as from the start of the log.
- **Repulsor:** pushdown of operators into Cypher and planning (P2); joins, paths and `Propagate` (stage 2); reads by recorded or occurred time and waiting reads (*Not staged*); changes to `events` or `query`; the conflict check (K9); the generic entity view (B2); authentication to Neo4j beyond a user and password.

## Design

The agent's calls, for review in the pull request; no team member agreed them during the run.

- **Graph.** Each accepted event is an `Event` node (id, position, recorded time, causes). Each tuple that brings a thing into being or states an attribute is a `Statement` node, `ABOUT` the `Thing` it is about; a reference value also `REFERS_TO` its `Thing`. A tuple that retracts adds a `Retraction` of its target. Partial tuples other than the empty one are kept only when they retract, as `Statements.at` reads them.
- **What holds.** A statement holds at a frontier when its event is in the frontier and no retraction in the frontier names it, exactly as `Statements.at`.
- **Frontiers.** Without a frontier, a query is answered at everything the datastore has taken in. A frontier is checked against the datastore's own events: one it lacks is refused, and the datastore never reads the log to answer. When the frontier's events are the first n the datastore took in, statements are filtered by position; otherwise by the frontier's events.
- **Answers.** `Evaluator` answers over a `Graph` whose three lookups are Cypher reads, in one read transaction per answer. Order is recorded order, as in `EventsDatastore`.
- **Taking in.** `catchUp(log)` takes in events from the position after the last one taken in, a page per write transaction; `follow(log)` keeps it up to date. The position and heads are written in the same transaction as the events, so a crash leaves a consistent state.
- **Snapshots.** A snapshot is the datastore's content at a position, as JSON lines: its progress, events, statements and retractions. `restore` replaces the content with a snapshot; `rebuild(snapshot, log)` restores and catches up. Things and relationships are rebuilt from statements.
- **Equality.** Two datastores are equal when their snapshots are equal.

## Plan

1. Module `datastore-neo4j` (`khora.query.neo4j`), driver 6.3.0, Neo4j 2026.10.2 community in Testcontainers and `docker-compose.yml`.
2. `Neo4jDatastore`: schema, `catchUp`, `follow`, `answer` through `Evaluator`.
3. `Neo4jDatastoreTest` extends `DatastoreContract`; `Neo4jSnapshotDatastoreTest` extends it over a datastore rebuilt from a snapshot.
4. `Snapshot`: write, read, restore, rebuild; a test that a rebuild from a snapshot equals a rebuild from the start of the log.
5. An equivalence test against `EventsDatastore` on generated logs with concurrent events and retractions, at prefix and concurrent frontiers.
6. `src/README.md` names the module.

## Observe

- **Done:** `Neo4jDatastore` passes `DatastoreContract` built from the start of the log, and again rebuilt from a snapshot and caught up (the issue's first box). A rebuild from a snapshot at position 120 of 300 events gives the same snapshot, the same counts of nodes and relationships, and the same answers as a rebuild from the start (the second box). On three generated logs with branches and retractions, twelve queries give the answers of `EventsDatastore` at everything, at single events, at joined branches and at the empty frontier.
- **Not done:** no operator is pushed down into Cypher; reads wait for nothing; reads by recorded or occurred time; the conflict check through the graph (K9).
- **Checked:** `./gradlew build` on Java 25: 78 tests in `events`, 26 in `surfaces`, 32 in `log-nats` (against NATS in Docker, which #39 could not run), 40 in `query` and 65 in `datastore-neo4j` (against Neo4j 2026.10.2 community in Docker) pass, with no compiler warnings. `docker compose config` accepts the new `neo4j` service. khora-validate passes on the run, its Letters and its Digest. Nothing in `docs/`, `events` or `query` changed.
- **Review:** the agent's own review, of the diff. Cypher text is built only from constants; every value is a parameter. A clear cut short could have left a datastore answering from part of its content; `clear` now deletes the progress first, so it refuses to take events in instead.
- **Flagged:** [[The graph datastore pushes nothing down into Cypher]] (performance), [[Restoring a graph snapshot is not one transaction]] (change-later), [[Whoever can write to the Neo4j database changes every answer]] (security).