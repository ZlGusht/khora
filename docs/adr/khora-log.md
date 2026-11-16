# ADR-0YB: The khora log

> **Status:** Proposed
> **Date:** 13.11.2026
> **Issue:** #4 Action log: storage and versioning model
> **Related:** ADR-0YA (events; everything khora owns lives in its log)
> **Number:** to be assigned when added to the repository

## Context

Everything khora owns lives in its log (ADR-0YA decision 7), and the log holds only events (ADR-0YA decision 1). This ADR fixes what an event stores and how the log scales.

## Terms

| Term | Meaning |
| --- | --- |
| **Log** | An append-only sequence of events in a total order. |
| **Payload** | Content stored once by hash and referenced from events. |
| **Branch** | A log forked from another at a frontier. |

## Decisions

**1. Every event carries two times.**
- *Recorded* is a hybrid logical clock timestamp: close to real time, never going backwards, always after the event's causes. It orders the log.
- *Occurred* is when the event happened, supplied by whoever acted or observed. It may be earlier, for example for delegated asynchronous work or offline edits.
- A past read is addressed by either.

**2. Each deployment has at least one log.** A log is split into partitions by entity or scope only when throughput requires it, and stays one log logically.

**3. An event records what happened and what caused it.**

```
Event { id: hash, log, causes: [EventId],
        occurred, recorded, vocabulary,
        body: Action | Outcome | Effect | Observation }

Action      { actor, on_behalf_of, grant, via, operation }   // a request; via: the surface or behavior it went through
Outcome     { accepted | rejected(reason) }                  // its cause is the action
Effect      = Attr   { entity, attribute, old, new }         // its cause is an outcome or an observation
            | Rel    { relation, from, to, +/− }
            | Member { scope, entity, +/− }
            | Kind   { entity, old, new }
Observation { origin, position, content }                    // what khora saw in an external system
```

- A rejected action keeps its action and its rejected outcome, and has no effects.
- A write to an external origin is accepted when khora sends it. It is confirmed by the observation that later shows it there, which names the action among its causes.
- The kinds of event are open to extension.

**4. Effects are invertible.** The same format serves undo, incremental surfaces and conflict detection.

**5. Events form a causal hash graph.** An event's id is the hash of its content, including its causes. A frontier is valid only if it never includes an event without its causes.

**6. The outcome and effects of one action are appended together, or not at all.** *Suggestion*
- A bulk write is one action. Its effects are stored as one payload, and entities inside it are addressed within the batch.

**7. Old segments move to cold storage and are never compacted away.**

**8. Branches hold work that is not in the base yet:** plans, drafts, agent work, offline edits, unconfirmed writes (ADR-0XY decision 8).
- A read on a branch sees its base up to the fork and the branch's own events.
- Merging appends the branch's actions to the base as new conditional actions, so conflicts surface like any other.

**9. External content stays outside the log.** khora logs what it observed in external origins and what it wrote to them. It keeps snapshots of external content only where a domain needs history the source does not keep.

**10. Events are never deleted; content is shredded.**
- Payloads are encrypted per entity or holder. Deleting the key deletes the content.
- The skeleton (id, causes, and for an action its actor) stays, so causality and audit stay intact.
- Deletion and decay are staged: archive first, then shred.

**11. A read is an action with no effects.** Its result is returned, not stored.

## Costs and risks

- The log's throughput bounds sustained write capacity. Its latency bounds how long writes stay unconfirmed, how wide the conflict window is, and how long work that needs confirmation waits.
- Reads far outnumber writes. If every read is kept like a write, reads bound the log.
- Hot data is stored twice: in the log and in the surfaces that materialize it.
- History cannot be rewritten. Mistakes are corrected only by new actions.
- Recorded times from different deployments are comparable only within clock skew, so consistency across deployments relies on frontiers. **(interop: review)**
- Losing a key also deletes content.

## Alternatives considered

- **One record per action, with its effects and outcome inside:** the request cannot be told from its result, and an outcome that arrives later rewrites the record.
- **The log inside the graph store:** no cursors, no cheap replay.
- **Full state per event:** wasteful.
- **Diffs without old values:** cannot be undone or checked for conflicts.

## Open questions

| Question | Lean |
| --- | --- |
| Log store | Measure Redpanda/Kafka, NATS JetStream, and an embedded log on object storage |
| Which read actions are kept, and for how long | *Suggestion*: all are events; kept durably where a scope requires it (ADR-0XZ decision 19), otherwise in a partition with short retention |
| Does a recorded read keep a hash of its result? | Open |
| Segment and batch payload format | Columnar (Parquet, Iceberg), so surfaces can index segments directly; not required for the MVP |
| Partitioning rule | By entity or scope hash, once needed |
| Key granularity | Per entity, or per holder and scope; decide with security (ADR-0YE) |
| Rebasing long-lived branches | Open |

## MVP

Decisions 1–7, 10 and 11.
- The bulk payload in decision 6 is to be confirmed. It is first needed by the furniture import.
- Branches (8) start with unconfirmed writes only, and only if the first version commits asynchronously (ADR-0XY open questions). Plans, offline edits and agent branches come later.
- Snapshots of external content (9) come with the first domain that needs them.

## Done when (issue #4)

A prototype measures:
- state at a frontier;
- one entity's history;
- recent events in a scope;
- the causes and consequences of an event, and the action behind an effect;
- append latency and throughput, including a 100 000-project batch import (furniture workshop R19).
