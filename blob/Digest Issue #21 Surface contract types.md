---
identity__uuid: f39d3c42-6dd8-432e-ad0d-ac5b117113a4
identity__type: particular
identity__created_at: 2026-11-22
identity__tags: []
relationships__is_a:
  - "[[Digest v1.0.0]]"
relationships__digest_of:
  - "[[Issue #21 Surface contract types]]"
relationships__in_context_of:
  - "[[Milestone Stage 1 kernel core]]"
properties__type: "digest"
properties__priority: 2
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Outcome

K4 is done as provisional types. `src/kernel/` holds the surface contract (`Surface`, `WritableSurface`, `ChangingSurface`, `Actions`), the declaration, the call context, and an in-memory stub surface; 27 tests pass on Java 25. Each read and write is one recorded action.

Not done: the contract is not shaped around events. Writes take a basic set and `changes` yields values. `EventId` and `Frontier` are placeholders that K1's Task 6 replaces.

## Findings

- The run's main result is design, not code: [[Events are the basic unit of khora]], [[Reads are events]] and [[An event's causes are what it knew]]. They answer [[Which parts of an entity are reified and keep identity]] and [[Whether reads are events]].
- The ADRs are not a reliable spec for the kernel. Where ADR-0XY was unclear (the read's time axis and meaning, what a frontier is), the types were cut back rather than guessed.
- A single list of causes cannot carry knowledge, translation and triggering at once; each needs its own named link.

## Frictions

- K1 builds on this branch, while this contract has to change once K1's `Event` exists. Resolved by merge order: K4, then K1, then a follow-up issue.
- Two runs share one vault: the K1 session wrote a second, uncommitted copy of a Decision that originated in this run ("Events are the unit that gives identity and connects surfaces").

## Considerations

- Create the proposed issue "The surface contract speaks events": `write` takes a surface's own event and records the basic events made from it, `changes()` yields events, `Change` and `Operation` go, and the stub keeps events and derives values. After K1 merges.
- An Update View run: this run's Decisions make `docs/adr/surfaces.md`, `docs/adr/runtime-and-executors.md` and `docs/implementation-plan.md` stale, and change ADR-0YA and ADR-0YB.
- The milestone's gate speaks of "an outcome and effects", but K1 no longer has effect events. The gate may need restating.
- Open: [[Frontiers need more than one position per origin]] (K1 covers the khora log only), [[What the basic event vocabulary contains]], [[How overlapping event vocabularies are aligned]], [[How translations between vocabulary versions are kept]].
- K1 should follow [[Causes and reads for K1's events]], especially comparing frontiers by content rather than equality in Task 6.
- Deferred review minors: nulls inside collections don't name their field; four types say only "Provisional."; `Actions` takes the call kind as a string; actions are recorded outside the stub's lock, which matters for K3.