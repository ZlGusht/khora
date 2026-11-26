---
identity__uuid: e1f0ba34-40d3-41db-9afd-0bf7a7d86100
identity__type: particular
identity__created_at: 2026-11-22
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Surfaces]]"
relationships__originates_in:
  - "[[Issue #18 Event types ids and clock]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
relationships__about:
  - "[[Every surface has one event stream, read from a frontier]]"
properties__decided_on: 2026-11-22
properties__status: "active"
properties__kind: "design"
properties__quote: "ok for the questions"
properties__author: agent:claude-opus-5-5
---
## Statement

A read names a thing by its identity and a frontier, is recorded as a read event, and returns the statements that hold about the thing at that frontier. A write is an action, together with the read it was based on, which is a named link and among its causes. A write conflicts only with events it did not know of that assert on the same identity and attribute.

## Rationale

Values are derived from events, so reads return statements, not values, and writes carry no expected value. Concurrency comes from causes alone ([[An event's causes are what it knew]]); relevance comes from what the events assert, so writes in unrelated threads neither block reads nor conflict with writes.