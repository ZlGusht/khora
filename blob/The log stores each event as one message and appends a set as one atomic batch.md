---
identity__uuid: 42f46edd-e3a0-49c5-bc7f-27c0cd0a6961
identity__type: particular
identity__created_at: 2026-11-18
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Log]]"
relationships__originates_in:
  - "[[Issue #19 Log store on NATS]]"
relationships__decided_by:
  - "[[Hristiyan Ganchov]]"
properties__decided_on: 2026-11-18
properties__status: active
properties__kind: design
properties__author: agent:claude-opus-5-5
---
## Statement

Each event is one NATS message under a subject for its primary entity. The records of one append are published as one atomic batch, all or none. If atomic batches from Java prove unworkable, one append becomes one message holding all its records.

## Rationale

The alternative, one message per append, is atomic without extra libraries but gives no per-entity history from the log. It stays the fallback. Atomic batch publish needs NATS 2.12 and Synadia's Orbit.java, and allows at most 1000 messages per batch.