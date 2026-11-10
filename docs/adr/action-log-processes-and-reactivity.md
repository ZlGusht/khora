# ADR-0YB: Action log, processes and reactivity

> **Status:** Provisional (core ideas recorded; not yet reviewed or measured)
> **Date:** 10.11.2026
> **Issue:** #4 Action log: storage and versioning model
> **Related:** ADR-0XX Query model and propagation; ADR-0XY Entity sources, time and schema; ADR-0XZ Scopes, khoras and rights; ADR-0YA Entity behavior and effects
> **Number:** to be assigned when added to the repository

## Context

The earlier ADRs lean on the action log without defining it:
- ADR-0XY uses its positions as khora's clock.
- ADR-0XZ makes snapshots frontiers over several logs.
- ADR-0YA records actions as invocations plus their effects, and controllers react to changes.

The sample specs add two more demands. Processes such as review rounds, learning loops and development tasks run for weeks and must survive restarts and be replayable. And data must feel alive (PRD P0 Interaction): views, alerts and automation react to change instead of being refreshed.

This ADR makes the log the system's engine, not only its history. Processes run on it, and reactivity is built on it.

## Terms

| Term | Meaning |
| --- | --- |
| **Action** | One entry in a log: an invocation, its outcome and its effects (ADR-0YA). |
| **Log** | An append-only sequence of actions with a total order. A log is an entity. |
| **Projection** | Anything computed by folding the log: datastores, indexes, access labels, lenses, views, process state. |
| **Frontier** | One position per log (ADR-0XZ). |
| **Subscription** | A `Query` plus a cursor, delivering changes to its result. |
| **Process instance** | A running process. Its state is a fold over its own actions. |

## Decisions

### The log

**1. The log is the source of truth for internal entities. Everything else is a projection.**
- Each projection reports the frontier it has applied. This generalizes ADR-0XY decision 7.
- Projections are materialized where reads must be fast and computed on demand otherwise.
- A snapshot is a projection checkpointed at a frontier.

**2. An action records intent, outcome and effects.**

```
Action {
  id:         hash(content)
  log, pos
  causes:     [ActionId]
  invocation: { actor, on_behalf_of, grant, operation, args }   // ADR-0YA decision 9
  process?:   { instance, step, version }
  trace
  outcome:    applied | rejected(reason) | pending
  effects:    [Delta]
}
Delta = Attr { entity, attribute, old, new }
      | Rel { relation, from, to, +/− }
      | Member { scope, entity, +/− }
```

- Rejected attempts are logged too, so provenance shows what was tried as well as what was done.
- Deltas are invertible. One format then serves streams, incremental views, undo and conflict detection: a write whose `old` no longer matches is a conflict.

**3. Actions form a causal hash graph.**
- An action's id is the hash of its content, and its causes are ids of earlier actions.
- Causality is part of identity, and tampering with history is detectable.
- A frontier is valid only if it is causally closed: it never includes an effect without its causes.

**4. A log is the unit of total order and replication.**
- Each deployment has at least one log, so deployments write without coordinating.
- Within a deployment, the log is partitioned by entity or scope only when throughput requires it.
- Per-process or per-scope streams are filtered views, not separate logs.

**5. External content stays outside the log.** Sync and write-back are actions about it.
- Write-back goes through an outbox: an intent action, the connector's attempt, then a confirmation action.
- Large payloads are stored once, addressed by hash, and referenced from actions.

### Processes

**6. A process instance is a durable execution on the log.**
- Each step appends its start (with inputs), then its completion (with outputs and effects) or its failure.
- A step's effects commit atomically as one action.
- An instance's state is restored by folding its actions.

**7. Replay depends on determinism, which every step declares (ADR-0YA decision 3).**
- Deterministic steps may be re-run to verify them.
- Nondeterministic steps return their recorded outputs. These include agent calls, external calls, clock reads and reads outside the instance's pinned frontier.

**8. Waiting is an action, not a thread.** Timers, human steps, approvals and waits for sync suspend the instance. A matching action resumes it, and a waiting instance holds no resources.

**9. Instances keep the version they started with.**
- An instance stays pinned to its process definition's version. Migrating it is an explicit action.
- Failure across steps is handled by compensation (sagas). The default compensation is derived from invertible deltas.

### Reactivity

**10. Subscriptions are the basis of reactivity.**
- A subscription is a `Query` plus a cursor. The cursor is an entity, so subscriptions survive restarts.
- Delivery is at-least-once. Handlers are idempotent by action id.
- Controllers and triggers (ADR-0YA) are built on subscriptions. Cause recording and cascade budgets follow ADR-0YA decisions 15–16.
- Views for people are subscriptions by default, not snapshots.

**11. Live views are maintained incrementally, as far as their algebra allows.** This amends ADR-0XX decision 6: each algebra also declares how it can be maintained.

| Algebra | Insertions | Deletions |
| --- | --- | --- |
| Rings, e.g. (+, ×) | Incremental | Incremental (add the inverse) |
| Semirings without inverses, e.g. (or, and), (min, +) | Incremental | Counting derivations, or re-derivation |
| Non-monotone operators | Re-evaluate the affected stratum | Same |

**12. Time-driven change is lazy.**
- Decay and staleness are computed at read time from age.
- Reacting to a threshold schedules a timer action at the moment it is crossed. Nothing ticks per entity.

**13. Projections advance in dependency order.**
- A projection advances only to frontiers its inputs have reached.
- A stream delivers an action at frontier *f* only after the access labels have reached *f*, so a revocation is never overtaken by a read.

### Retention

**14. Actions are never deleted; content is shredded.**
- Payloads are encrypted per entity or holder, and deleting the key deletes the content.
- The action skeleton (id, causes, actor) survives, so causality and audit stay intact.
- Decay is staged: archive first, then shred.

## Caveats

- **Projections cost.** Every materialized projection must keep up with the log; the log is cheap, its readers are not.
- **Deletions are expensive in reachability.** Blast radius, network tracing and grant chains are reachability. Without counting, a deletion means re-deriving.
- **Replay is only as good as declarations.** A step wrongly declared deterministic diverges on replay.
- **At-least-once delivery shifts work to handlers.** Every handler must be idempotent.
- **Cross-deployment frontiers lag.** Causally closed frontiers wait for the slowest log they depend on. **(interop: review)**
- **Shredding needs key management.** Losing a key is also deletion.

## Consequences

**Positive**
- One mechanism feeds storage, views, access labels, processes and automation.
- Every state, process and decision is replayable and traceable to its causes.
- Long-running processes cost nothing while waiting and survive restarts.
- Data is live by default.

**Negative**
- The log is on every write path; its latency bounds the system's.
- Process steps must declare their determinism.
- Key management becomes part of the kernel.

## Alternatives considered

- **Actions as entities in the graph store:** one query model, but a graph store is a poor log, with no cursors or cheap replay. Actions are exposed as entities through the source contract (ADR-0XY) whatever stores them.
- **Log beside the datastores, both written directly:** two sources of truth to keep consistent.
- **Full before/after state per action:** wasteful. Plain diffs cannot be undone.
- **Process state in a separate workflow engine:** a second log beside khora's, with its own replay and provenance.
- **Polling instead of subscriptions:** stale views, wasted load, no causal links between change and reaction.

## Open questions

| Question | Lean |
| --- | --- |
| Log store | Log-native (Redpanda/Kafka, NATS JetStream) or an embedded log on object storage; decide by measurement |
| Partitioning rule within a deployment | By entity or scope hash, once needed |
| Which projections are materialized first | Entity history, scope index, access labels |
| Granularity of encryption keys | Per entity, or per holder and scope; decide with security |
| Process definition language and step contract | With the process engine |

## Out of scope (TODO)

- The process engine itself, beyond its contract with the log.
- Snapshots (separate issue).
- The interoperability protocol, including log replication across deployments.

## Done when (issue #4)

- This ADR is accepted, and ADR-0XY decision 7 and ADR-0XX decision 6 are amended.
- These are measured on a prototype:
  - state as of a frontier;
  - an entity's history;
  - recent actions in a scope;
  - the causes and consequences of an action;
  - rebuilding a process instance;
  - append-to-subscriber latency;
  - keeping access labels current after a revocation.
