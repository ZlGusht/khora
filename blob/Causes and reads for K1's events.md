---
identity__uuid: a0b18c5e-bcef-437d-b5ce-ad5e01323bdc
identity__type: particular
identity__created_at: 2026-11-22
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #18 Event types ids and clock]]"
relationships__relates_to:
  - "[[Issue #21 Surface contract types]]"
  - "[[An event's causes are what it knew]]"
  - "[[Reads are events]]"
properties__type: task
properties__priority: 2
properties__resolved: true
properties__author: agent:claude-opus-5-5
---
## Context

The Issue #21 run (K4) reviewed PR #36 and agreed two Decisions with [[Simeon Petkov]] that change how K1 describes events: [[An event's causes are what it knew]] and [[Reads are events]]. K4 merges first, as provisional; reshaping the surface contract around events is a follow-up issue after K1.

## Content

- **Causes:** `Event`'s Javadoc says causes are "the heads of what it read". Under the Decision they are the events it happened after and knew of. What a read read, a translation's source, and a behavior's trigger are named links in the body, each pointing into the causal past.
- **Reads:** a read is an accepted action with a basic read definition. `Basic` has only `assert` so far.
- **Task 6, frontier equality:** the plan has the stub surface compare frontiers by equality, but `Frontier`'s constructor does not reduce to heads. `{a, b}` where `a` caused `b` and `{b}` are the same set but unequal, so a valid write would be rejected as stale. Compare with `FrontierCheck.compare`, or reduce in the constructor.
- **Identity:** a bare `sha256:…` is both a well-known name and an event id; nothing says which.

Resolved in the Process(v) of [[Issue #18 Event types ids and clock]]: causes now mean what an event knew, in `Event`'s Javadoc; `Basic.READ` exists, though a read's arguments and named links do not ([[Reads, translations and reactions have no named links yet]]); the stub keeps frontier equality by ruling, flagged in [[The stub surface's frontiers are not real frontiers]]; the identity ambiguity is [[Named identities and event ids share one form]].