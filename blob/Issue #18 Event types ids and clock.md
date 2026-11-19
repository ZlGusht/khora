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
properties__status: "open"
properties__started_at: 2026-11-19
properties__current_phase: "Inception"
properties__issue: 18
properties__author: agent:claude-opus-5-5
---
Issue #18 (plan task K1) of [[Milestone Stage 1 kernel core]]: the four event types, their content-hash ids and the hybrid logical clock, in Java. Opened with [[Simeon Petkov]]. It runs in parallel with [[Issue #21 Surface contract types]].

## Loop Config

- **Domain:** Complicated. ADR-0YB decisions 1, 3 and 5 specify the events, their ids and the clock; the work is translating them into types and choosing the hashing and clock details.
- **Scope of Authority:** the event types, event ids and the clock under `src/`, their tests, and this run's entities in `blob/`. The code lives in its own worktree on branch `issue/18-event-types-ids-and-clock`. Documents in `docs/` change only through Update View from Decisions.
- **Reservoir capacity:** one pull request.

## Vector

- **Attractor:** action, outcome, effect and observation as Java types. An event's id is the hash of its content, including its causes' ids. Recorded time is a hybrid logical clock. An event round-trips; a frontier that lacks an event's cause is refused; an effect whose cause is neither an accepted outcome nor an observation is refused.
- **Repulsor:** not set yet.

## Decisions

- [[K1 runs in parallel with K4]]
- [[Parallel runs keep code in separate worktrees and share one vault]]