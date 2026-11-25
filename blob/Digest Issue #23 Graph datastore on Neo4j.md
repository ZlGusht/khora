---
identity__uuid: 4b32d090-e3cd-4dbd-bf3d-7622f405741e
identity__type: particular
identity__created_at: 2026-11-25
identity__tags: []
relationships__is_a:
  - "[[Digest v1.0.0]]"
relationships__digest_of:
  - "[[Issue #23 Graph datastore on Neo4j]]"
relationships__in_context_of:
  - "[[Milestone Stage 1 kernel core]]"
properties__type: "digest"
properties__priority: 2
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Outcome

The graph datastore exists: `Neo4jDatastore`, in the new `datastore-neo4j` module. It takes in a log's events and keeps events, things, statements and retractions as a Neo4j graph. It answers every query at any frontier it holds, through `Evaluator`, with the answers of `EventsDatastore`. It passes `DatastoreContract` both when built from the start of the log and when rebuilt from a snapshot. A rebuild from a snapshot equals a rebuild from the start of the log. 241 tests pass on Java 25, with NATS and Neo4j in Docker.

Not done: pushdown into Cypher, waiting reads, reads by recorded or occurred time, and the conflict check (K9).

## Findings

- The datastore needs only the log's positions and each event's causes to answer at any frontier. When a frontier's events are the first n taken in, filtering by position is enough; this covers every read in stage 1 that is not on a branch.
- `Evaluator`'s three lookups were enough. Nothing in `query` or `events` changed.
- The contract tests could not tell a datastore that took in the whole log from one rebuilt from a snapshot. Both pass the same 27 tests.

## Frictions

- The issue, the plan's K6 row and ADR-0XY speak of effects, which the event kernel does not keep. The run read "materialized from effects" as "materialized from the statements of accepted events".
- The design choices are the agent's, made in one go on request. They are listed in the run's Design section, for review in the pull request.
- `khora-sync open` refuses to run from a worktree, because the vault points at the main checkout. The run's issue comment was posted by hand, with the same marker.

## Considerations

- Review the graph's shape before stage 2 builds traversals on it: `Statement` nodes `ABOUT` a `Thing`, references `REFERS_TO` a `Thing`, and retractions kept as their own nodes.
- [[EventsDatastore works every answer out from all events]] can be resolved: the graph datastore answers from materialized state, and `EventsDatastore` stays as the reference.
- Unresolved Letters: [[The graph datastore pushes nothing down into Cypher]], [[Restoring a graph snapshot is not one transaction]], [[Whoever can write to the Neo4j database changes every answer]].
- Proposed issue: restate #23, the plan's K6 row and ADR-0XY decision 10 in events, with the rest of `Documents lag K1's Decisions` (#36).