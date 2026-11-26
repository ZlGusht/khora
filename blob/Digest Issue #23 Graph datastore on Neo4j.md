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

The graph datastore exists: `Neo4jDatastore`, in the new `datastore-neo4j` module. It takes in a log's events and keeps what they assert as it stands now: an entity is a node, its literal attributes are properties, and a reference is a relationship ([[The graph datastore materializes entities, not events]]). A retraction deletes what its target asserted. It answers every query at everything it has taken in, through `Evaluator`, with the answers of `EventsDatastore`, and passes `DatastoreContract` both built from the start of the log and rebuilt from a snapshot. A rebuild from a snapshot equals a rebuild from the start of the log. 249 tests pass on Java 25, with NATS and Neo4j in Docker.

Not done: past and branch reads, pushdown into Cypher, and the conflict check (K9).

## Findings

- The first version stored events and statements as nodes, to answer at any frontier. In review, Simeon Petkov decided the datastore keeps entities and answers only now; the log keeps the history.
- A retraction needs no index in the datastore: its target's identity names the event and the tuple, and the tuple says what to delete.
- `Evaluator`'s three lookups were enough; nothing in `events` changed. `DatastoreContract` now says which tests need past reads.

## Frictions

- The issue, the plan's K6 row and ADR-0XY speak of effects, which the event kernel does not keep.
- The first design came from the contract's past-frontier tests rather than from what a datastore is for; the rework took one more round of review.
- `khora-sync open` refuses to run from a worktree, because the vault points at the main checkout. The run's issue comment was posted by hand, with the same marker.

## Considerations

- Review the graph's shape before stage 2 builds traversals on it: entities as nodes, attribute identities as property keys and relationship types, and each relationship holding its statement's identity.
- [[EventsDatastore works every answer out from all events]] can be resolved: the graph datastore answers from materialized state, and `EventsDatastore` stays as the reference.
- Unresolved Letters: [[The graph datastore pushes nothing down into Cypher]], [[Restoring a graph snapshot is not one transaction]], [[A thing's values of one attribute are a set of one type]], [[Whoever can write to the Neo4j database changes every answer]].
- Proposed issue: restate #23, the plan's K6 row and ADR-0XY decision 10 in events and in this Decision, with the rest of `Documents lag K1's Decisions` (#36).