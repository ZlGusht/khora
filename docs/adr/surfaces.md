# ADR-0XY: Surfaces

> **Status:** Proposed
> **Date:** 13.11.2026
> **Issue:** #1 DataStore: base and advanced capabilities
> **Related:** ADR-0YA (surfaces are the only way to reach data); ADR-0XX (the logic surfaces run); ADR-0YB (the log surfaces build on)
> **Number:** to be assigned when added to the repository

## Context

Surfaces are the only way to reach data (ADR-0YA decision 10). This ADR fixes the contract every surface implements, what surfaces declare, how reads are pinned in time, and how writes meet.

## Terms

| Term | Meaning |
| --- | --- |
| **Declaration** | What a surface states about itself: capabilities, operations it offers, what it builds on. |
| **Frontier** | One position per origin a read depends on: a recorded time for the khora log; the origin's own position, or khora's observation time, for others. |
| **Condition** | What a write assumes: the frontier it read and the values it depends on. |
| **Merge rule** | How concurrent writes to one attribute or relation combine (ADR-0XX decision 7). |
| **Upcaster** | Maps an effect written under an older definition to the current one. |
| **Adapter** | The surface that connects an external system as an origin. |

## Decisions

**1. Every surface implements one contract.**

```
Surface {
  declares(): Declaration
  read(request, ctx): Result
  write?(operation, condition, ctx): Unconfirmed | Rejected
  changes?(since): Stream<Change>
}
ctx = { principal, grant, budget, cause }
```

- The request states the time it reads at (ADR-0XX decision 2).
- A result carries its completeness, the frontier it saw, and its annotations.
- Reads and writes go through the same contract and context, so both are always attributable. Each call is one action (ADR-0YA decision 2).

**2. Surfaces declare their capabilities:**
- authoritative or not;
- effects: native, observed or none;
- history: full, snapshots or none;
- reads: deterministic, recorded or nondeterministic;
- writes: none, unconditional, conditional or mergeable;
- order.

Results carry what those declarations imply, such as "as observed at 10:42" or "not reproducible".

**3. Datastores are surfaces that also declare what they can execute:** traversal, search, spatial, time series, valid time.
- The runtime pushes the logic of surfaces above them down into them.
- Per operation, a datastore answers exact, inexact or unsupported.
- It never silently drops part of a request.

**4. Every read happens at a frontier.**
- A surface reports the frontier it has reached. A read at a later frontier waits or comes back partial.
- A frontier holds only the origins the read depends on.

**5. A past read sees the past as it was:** old data, old definitions, old kinds. Reading old data with today's meaning is explicit and goes through upcasters.

**6. Effects keep the vocabulary version they were written in.** A definition that changes meaning provides an upcaster from its earlier versions. An effect with no upcaster path is read as it was and marked.

**7. Every write is conditional on what it read.** If the condition fails, the action gets a rejected outcome (ADR-0YB decision 3), unless the attribute declares a merge rule.

**8. Surfaces apply writes locally and commit asynchronously.**
- Unconfirmed writes are a short-lived branch (ADR-0YB decision 8).
- Confirmation or rejection arrives through the surface's changes.
- Work that needs confirmation waits for it.

**9. External systems are connected through adapters.**
- An adapter records the system's changes as observations, with the effects they cause, and turns writes into the system's own operations.
- A write to an external origin is unconfirmed until an observation shows it there.
- Two origins asserting different values for the same thing are both kept, with a contradiction flag.

**10. Materializing a surface changes speed, never results.**
- A surface is kept up to date, cached, or computed on request.
- A materialized surface rebuilds from its latest snapshot at a frontier, not from the start of the log.

## Costs and risks

- Upcasters accumulate: every change in meaning adds one, permanently.
- Until definitions live in the log, they are versioned by the repository. A past read then sees old data and old kinds, but current definitions (ADR-0YC decision 11).
- Asynchronous commits widen the conflict window. Without merge rules, people see more rejections.
- Pushdown fallbacks are correct but can be far slower than native execution.

## Alternatives considered

- **Each store exposes its own query language:** callers would have to know where data lives.
- **Synchronous writes:** people wait on the log.
- **Migrating stored effects when the vocabulary changes:** breaks hashes and history.
- **Last writer wins everywhere:** silent data loss.

## Open questions

| Question | Lean |
| --- | --- |
| Conflict resolution beyond merge rules | Later; needs its own design |
| Does the first version commit synchronously, so that branches for unconfirmed writes can wait? | Open. The write call answers "unconfirmed" either way, so callers do not change later |
| Result batch format | Arrow record batches |

## MVP

Decisions 1–8 and 10, with two or three datastores. Old definitions in past reads (5) wait until definitions live in the log. Adapters (9) start with the first external system a domain needs.

## Done when (issue #1)

Contract tests cover:
- the base contract;
- each declared capability;
- each pushdown fallback, checked against a native implementation;
- conditional writes and conflicts;
- upcasting;
- past reads as they were.
