---
identity__uuid: 4e485a2a-305e-4692-bd09-e48e65f6d230
identity__type: particular
identity__created_at: 2026-11-19
identity__tags: []
relationships__is_a:
  - "[[Digest v1.0.0]]"
relationships__digest_of:
  - "[[Issue #19 Log store on NATS]]"
relationships__in_context_of:
  - "[[Milestone Stage 1 kernel core]]"
properties__type: "digest"
properties__priority: 2
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Outcome

The kernel has a log: `LogStore` in `kernel-core`, implemented on NATS JetStream in `log-nats`, with an in-memory twin and a contract test both pass. Appends are all or none; one entity's history and a subscription come straight from the log. Measured on one machine: a write of an outcome with three effects is confirmed in about 2 ms with `sync: always`. Not done: the #19 measurements that need K1, K6 and K7 (frontier, scope, causes), and any candidate other than NATS.

## Findings

- Atomic batch publish works from Java (NATS 2.12, Orbit.java). A refused batch stores nothing.
- NATS refuses a whole batch that repeats an event id; the store removes duplicates first. Duplicates across appends are caught only within the 2-minute window; beyond it, uniqueness relies on K1's content-hash ids.
- `sync: always` costs about 3 times on small batches and about 14 times on large ones; a million-record import takes 5.5 minutes instead of 10 seconds. Hence the selectable sync.
- History read from the log grows with the log's size (about 96 ms for 1 000 records in a log of a million). A large log needs an index: K6 and B1.
- An exception from `append` does not mean "not stored": the batch may be stored whole. Retrying with the same ids is safe within the window. K3 must handle this.

## Frictions

- The Mac slept during a test run; tests against Docker stalled for hours. Long runs need `caffeinate -i`.
- NATS trims spaces at the ends of header values, so event ids may not start or end with a space.

## Considerations

- #19's "Done when" asks for every candidate and for measurements that depend on K1, K6 and K7. Narrow it, or move those measurements to the issues they depend on.
- The run's Decisions (NATS, Java 25 and Gradle, atomic batches, sync) replace Kafka from an earlier call; they are accepted only when the pull request is merged.
- Decisions from the 18.11 call are not recorded yet: reads are not logged, undo is an inverse effect, CLI first, Neo4j as the first datastore, conditional writes deferred. Some contradict ADR-0YB decision 11 and ADR-0XY decision 7.
- The CLI has no issue and no owner.
- `CLAUDE.md` still says `src/` holds no code.
- K6 needs a signal when a subscription stops on an unreadable record.