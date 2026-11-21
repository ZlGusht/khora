---
identity__uuid: 484082cc-f957-4535-b88c-c9c6f3aeb42b
identity__type: particular
identity__created_at: 2026-11-19
identity__tags: []
relationships__is_a:
  - "[[Process Instance v1.0.0]]"
  - "[[Issue Loop v1.0.0]]"
relationships__in_process:
  - "[[Milestone Stage 1 kernel core]]"
relationships__in_context_of:
  - "[[Log]]"
  - "[[Runtime]]"
properties__status: open
properties__started_at: 2026-11-19
properties__current_phase: Laydown
properties__issue: 18
properties__author: agent:claude-opus-5-5
---
Issue #18 (plan task K1) of [[Milestone Stage 1 kernel core]]: the four event types, their content-hash ids and the hybrid logical clock, in Java. Opened with [[Simeon Petkov]]. It runs in parallel with [[Issue #21 Surface contract types]].

## Loop Config

- **Domain:** Complicated. ADR-0YB decisions 1, 3 and 5 specify the events, their ids and the clock; the work is translating them into types and choosing the hashing and clock details.
- **Scope of Authority:** the event types, event ids and the clock under `src/`, their tests, and this run's entities in `blob/`. The code lives in its own worktree on branch `issue/18-event-types-ids-and-clock`. Documents in `docs/` change only through Update View from Decisions.
- **Reservoir capacity:** one pull request.

## Vector

- **Attractor:** the event as an accepted action, its salted content-hash id, the hybrid logical clock, and frontiers as sets of events closed under causes, in Java. An event round-trips; a frontier that lacks an event's cause is refused.
- **Repulsor:** frontiers over external origins, layered surfaces, writes conditioned across origins, branches and other deployments (the other cases in [[Frontiers need more than one position per origin]]); authority checks; encryption and shredding; keeping rejections; changes to K4's contract beyond replacing its `EventId` and `Frontier` placeholders.

## Decisions

- [[K1 runs in parallel with K4]]
- [[Parallel runs keep code in separate worktrees and share one vault]]
- [[An event is an accepted action, and an assert is the basic action]]
- [[An assert's id is the identity of what it asserts]]
- [[Event ids are salted hashes of canonical JSON]]

Taken in from the Issue #21 run: [[Events are the basic unit of khora]] (a frontier is the set of events a surface has taken in, closed under causes; K1 decides the basic vocabulary).

## Design

Types are provisional ([[Kernel types taken from ADRs are provisional]]); unions are sealed interfaces with records ([[Contract unions are sealed interfaces with records]]).

- **Event:** `id, causes, recorded, occurred?, by { principal, on_behalf_of?, grant, via }, salt, action`. `causes` are the heads of what the action read.
- **Action:** `{ definition, args }`. `definition` is the id of the action's definition, which says its vocabulary and version. The basic vocabulary has one action, `assert`, with a fixed well-known id the kernel ships; a reserved `retracted` attribute marks retraction. Other definitions are opaque to the kernel for now.
- **Assert shapes (provisional):** an entity; a property of an entity; a relationship between entities, with its relator; a behavior bound to an entity. The assert's id is the identity of the asserted thing.
- **Id:** `sha256:<hex>` of the event's canonical JSON (RFC 8785) without the id itself. Values are strings, integers and ids.
- **Clock:** a hybrid logical clock: wall milliseconds and a counter packed into one long. `now()` never goes backwards; `after(causes)` is later than every cause.
- **Frontier:** a set of head event ids, standing for the heads and all their causes. Operations: compare (before, after, equal, concurrent), combine (union, reduced to heads), and a closure check against an event graph that refuses a frontier whose events or causes are unknown. Replaces K4's `Frontier` placeholder; the event id replaces K4's `EventId` placeholder.
- **Event graph:** an in-memory store of events by id, used by the closure check and the tests. Appending an event whose causes are absent, or whose recorded time is not after its causes, is refused.
- **Round-trip:** an event written to canonical JSON and read back is equal, and its id recomputes to the same value.

Left to execution: the argument shapes of asserts, who defines a non-basic action (the principal's kind, the target's kind, or neither), and whether observations need anything beyond an assert by an adapter with a source.

Builds on the Issue #21 branch: this branch is rebased onto it, so the placeholders can be replaced.