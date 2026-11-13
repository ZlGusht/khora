# ADR-0YB: The khora log

> **Status:** Proposed
> **Date:** 12.11.2026
> **Issue:** #4 Action log: storage and versioning model
> **Related:** ADR-0YA (everything khora owns lives in its log)
> **Number:** to be assigned when added to the repository

## Context

Everything khora owns lives in its log (ADR-0YA decision 3). This ADR fixes what the log stores and how it scales.

## Terms

| Term | Meaning |
| --- | --- |
| **Log** | An append-only sequence of actions in a total order. |
| **Payload** | Content stored once by hash and referenced from actions. |
| **Branch** | A log forked from another at a frontier. |

## Decisions

**1. Every action carries two times.**
- *Recorded* is a hybrid logical clock timestamp: close to real time, never going backwards, always after the action's causes. It orders the log.
- *Occurred* is when the action happened, supplied by whoever acted. It may be earlier, for example for delegated asynchronous work or offline edits.
- A past read is addressed by either.

**2. Each deployment has at least one log.** A log is split into partitions by entity or scope only when throughput requires it, and stays one log logically.

**3. An action records who, why, what and how it ended.**

```
Action { id: hash, log, causes: [ActionId],
         occurred, recorded, vocabulary,
         actor, on_behalf_of, grant, via,         // via: the surface or behavior written through
         outcome: applied | rejected(reason) | pending,
         effects: [Effect] }
Effect = Attr   { entity, attribute, old, new }
       | Rel    { relation, from, to, +/− }
       | Member { scope, entity, +/− }
       | Kind   { entity, old, new }
```

- Rejected attempts are actions too.
- `pending` marks a write to an external origin that has not been observed there yet.
- The effect vocabulary is open to extension.

**4. Effects are invertible.** The same format serves undo, incremental surfaces and conflict detection.

**5. Actions form a causal hash graph.** An action's id is the hash of its content, including its causes. A frontier is valid only if it never includes an action without its causes.

**6. A bulk write is one action.** Its effects are stored as one payload, and entities inside it are addressed within the batch.

**7. Old segments move to cold storage and are never compacted away.**

**8. Branches hold work that is not in the base yet:** plans, drafts, agent work, offline edits, unconfirmed writes (ADR-0XY decision 8).
- A read on a branch sees its base up to the fork and the branch's own actions.
- Merging appends the branch's actions to the base as new conditional actions, so conflicts surface like any other.

**9. External content stays outside the log.** khora logs what it observed in external origins and what it wrote to them. It keeps snapshots of external content only where a domain needs history the source does not keep.

**10. Actions are never deleted; content is shredded.**
- Payloads are encrypted per entity or holder. Deleting the key deletes the content.
- The skeleton (id, causes, actor) stays, so causality and audit stay intact.
- Deletion and decay are staged: archive first, then shred.

## Costs and risks

- The log's throughput bounds sustained write capacity. Its latency bounds how long writes stay unconfirmed, how wide the conflict window is, and how long work that needs confirmation waits.
- Hot data is stored twice: in the log and in the surfaces that materialize it.
- History cannot be rewritten. Mistakes are corrected only by new actions.
- Recorded times from different deployments are comparable only within clock skew, so consistency across deployments relies on frontiers. **(interop: review)**
- Losing a key also deletes content.

## Alternatives considered

- **The log inside the graph store:** no cursors, no cheap replay.
- **Full state per action:** wasteful.
- **Diffs without old values:** cannot be undone or checked for conflicts.

## Open questions

| Question | Lean |
| --- | --- |
| Log store | Measure Redpanda/Kafka, NATS JetStream, and an embedded log on object storage |
| Segment and batch payload format | Columnar (Parquet, Iceberg), so surfaces can index segments directly; not required for the MVP |
| Partitioning rule | By entity or scope hash, once needed |
| Key granularity | Per entity, or per holder and scope; decide with security (ADR-0YE) |
| Rebasing long-lived branches | Open |

## MVP

Decisions 1–7 and 10. Branches (8) start with unconfirmed writes only; plans, offline edits and agent branches come later. Snapshots of external content (9) come with the first domain that needs them.

## Done when (issue #4)

A prototype measures:
- state at a frontier;
- one entity's history;
- recent actions in a scope;
- the causes and consequences of an action;
- append latency and throughput, including a 100 000-project batch import (furniture workshop R19).
