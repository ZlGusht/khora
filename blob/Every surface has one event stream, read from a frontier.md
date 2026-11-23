---
identity__uuid: 364cc87c-917f-4f49-960e-2523216a209f
identity__type: particular
identity__created_at: 2026-11-22
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Design]]"
  - "[[Surfaces]]"
relationships__originates_in:
  - "[[Issue #21 Surface contract types]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
relationships__supersedes:
  - "[[Surface changes are read through a blocking ChangeFeed]]"
  - "[[Surface changes take no context for now]]"
  - "[[Optional surface capabilities are separate interfaces]]"
properties__decided_on: 2026-11-22
properties__status: superseded
properties__kind: design
properties__author: agent:claude-opus-5-5
---
## Statement

Surfaces communicate through event streams, so every surface has one: the events it takes in and emits, read from a frontier. Changes are not a separate notion.

1. The stream is part of every surface's contract, beside declaring and reading. Taking writes stays an optional capability.
2. Reading a stream from a frontier yields every event not in that frontier, each after its causes, then new events as they happen. It is read through a blocking feed with close, on virtual threads.
3. Following a stream is a read: it carries a context and is recorded ([[Reads are events]]).
4. A write is confirmed when its event appears in the stream.

## Rationale

[[Events are the basic unit of khora]] connects surfaces by event streams, and every surface takes in events and emits its own. A surface's stream is therefore its change feed, and a change reported as a target and a value loses the identity, causes and meaning the events carry.

- The blocking feed of [[Surface changes are read through a blocking ChangeFeed]] is kept as the way a stream is read.
- [[Surface changes take no context for now]] is contradicted by [[Reads are events]].
- [[Optional surface capabilities are separate interfaces]] made changes optional; under event streams no surface is without one.

The code follows when the contract is reshaped around K1's events, in the issue "The surface contract speaks events". Until then the types that go are marked "Out with events" in their Javadoc.