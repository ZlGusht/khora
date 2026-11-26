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
properties__ended_at: 2026-11-26
properties__issue: 23
properties__author: agent:claude-opus-5-5
properties__pull_requests:
  - "40"
---
Issue #23 (plan task K6) of [[Milestone Stage 1 kernel core]]: the graph datastore, materialized from the events of the log, on Neo4j. It answers K8's `Query` ([[Stage 1's tasks and gate are stated in events]], item 2). Run by an agent in one go, on Simeon Petkov's request, built on the Issue #25 branch (pull request #39). Reopened on 2026-11-26 in review: the first version stored events and statements, and Simeon Petkov decided that the datastore keeps entities ([[The graph datastore materializes entities, not events]]).

The issue speaks of effects; the event kernel keeps none (`Effects are not a notion of their own`). Here "materialized from effects" reads as: materialized from the statements of accepted events.

## Loop Config

- **Domain:** Complicated. The contract fixes the results (ADR-0XY decision 10: materializing changes speed, never results); the work is the graph's shape, reads at any frontier, and snapshots.
- **Scope of Authority:** a new `datastore-neo4j` module under `src/`, `settings.gradle.kts`, `gradle/libs.versions.toml`, `docker-compose.yml`, `src/README.md`, and this run's entities in `blob/`. Nothing in `docs/` or `events`; in `query`, only `DatastoreContract`, for datastores without past reads.
- **Reservoir capacity:** one pull request.

## Vector

- **Attractor:** a `Datastore` on Neo4j that takes in the log's events, keeps the entities, attributes and relationships they assert as they stand now, answers every query at that frontier, passes `DatastoreContract`, and rebuilds from a snapshot to the same state as from the start of the log.
- **Repulsor:** pushdown of operators into Cypher and planning (P2); joins, paths and `Propagate` (stage 2); past and branch reads, reads by recorded or occurred time and waiting reads (*Not staged*); storing events or statements; giving attributes meaning; changes to `events` or `query`; the conflict check (K9); the generic entity view (B2); authentication to Neo4j beyond a user and password.

## Design

From [[The graph datastore materializes entities, not events]]; the rest are the agent's calls, for review in the pull request.

- **Entities.** Each thing is a `Thing` node, its identity as `id`. A thing brought into being by `(,,)` is also an `Entity`. A thing that is only named or referred to stays a plain `Thing`.
- **Attributes.** A literal value is a property of its thing, keyed by the attribute's identity as it is. One value is the property; several are a list. Values of one attribute of one thing are a set.
- **Relationships.** A reference is a relationship from the thing to the thing it names, typed by the attribute's identity, holding the identity of its statement, so the same reference asserted twice is two relationships.
- **Retraction.** It deletes what its target asserted, read from the target's event: the `Entity` label, the value, or the relationship with the target's identity. The thing's other attributes stay, as `Statements.at` keeps them. A retraction of a retraction deletes nothing.
- **Frontier.** A `Progress` node holds the last position taken in, the frontier of everything taken in and the last event's id, written in the same transaction as the page. Answers are at that frontier; a query at any other is refused. `DatastoreContract` says which of its tests need past reads.
- **Answers.** `Evaluator` answers over three Cypher lookups in one read transaction. Attribute identities reach Cypher as parameters, through dynamic property keys and relationship types.
- **Snapshots.** A snapshot is the datastore's content at a position, as JSON lines: each thing with its literal attributes, then each relationship. `rebuild(snapshot, log)` restores it and catches up; it refuses a snapshot whose last event is not the log's at that position.

## Plan

1. Module `datastore-neo4j` (`khora.query.neo4j`), driver 6.3.0, Neo4j 2026.10.2 community in Testcontainers and `docker-compose.yml`.
2. `Neo4jDatastore`: schema, `catchUp`, `follow`, `answer` through `Evaluator`.
3. `Neo4jDatastoreTest` extends `DatastoreContract`; `Neo4jSnapshotDatastoreTest` extends it over a datastore rebuilt from a snapshot.
4. `Snapshot`: write, read, restore, rebuild; a test that a rebuild from a snapshot equals a rebuild from the start of the log.
5. An equivalence test against `EventsDatastore` on generated logs with branches and retractions.
6. `src/README.md` names the module.
7. Reopened: entities instead of events and statements; `DatastoreContract.answersPastFrontiers`; `GraphShapeTest` for what is kept.

## Observe

- **Done:** `Neo4jDatastore` passes `DatastoreContract` built from the start of the log, and again rebuilt from a snapshot and caught up (the issue's first box). A rebuild from a snapshot at position 120 of 300 events gives the same snapshot, the same counts of nodes and relationships, and the same answers as a rebuild from the start (the second box). On three generated logs with branches and retractions, twelve queries give the answers of `EventsDatastore` at four points of the log. `GraphShapeTest` pins what is kept: entities with their attributes and relationships, no events, and retractions as deletes.
- **Not done:** past and branch reads; pushdown into Cypher; the conflict check through the graph (K9).
- **Checked:** `./gradlew build` on Java 25: 78 tests in `events`, 26 in `surfaces`, 32 in `log-nats` (against NATS in Docker), 42 in `query` (one skipped: it is for datastores without past reads) and 71 in `datastore-neo4j` (four skipped: they need past reads; against Neo4j 2026.10.2 community in Docker) pass, with no compiler warnings. khora-validate passes on the run, its Decision, its Letters and its Digest. Nothing in `docs/` or `events` changed.
- **Review:** the agent's own review, of the diff. Cypher text is built only from constants; values and attribute identities are parameters. A clear cut short leaves a datastore that refuses to take events in, rather than one that answers from part of its content.
- **Flagged:** [[The graph datastore pushes nothing down into Cypher]] (performance), [[Restoring a graph snapshot is not one transaction]] (change-later), [[A thing's values of one attribute are a set of one type]] (change-later), [[Whoever can write to the Neo4j database changes every answer]] (security).