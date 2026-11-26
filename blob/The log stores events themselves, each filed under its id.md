---
identity__uuid: ee9d7436-346c-4917-94b3-bb6dd611205f
identity__type: particular
identity__created_at: 2026-11-23
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Log]]"
relationships__originates_in:
  - "[[Issue #18 Event types ids and clock]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
relationships__supersedes:
  - "[[The log stores each event as one message and appends a set as one atomic batch]]"
properties__decided_on: 2026-11-23
properties__status: "active"
properties__kind: "design"
properties__quote: "do we need log entries? can we keep naming consistent with the event language?"
properties__author: agent:claude-opus-5-5
---
## Statement

The log stores events themselves, not records about them. Each event is one message, filed under its own id, with its canonical JSON as the data and its id as the message id. A set of events is appended as one atomic batch, all or none, and an event already stored is not stored again.

## Rationale

The record carried nothing the event does not, and its four types (action, outcome, effect, observation) no longer match the event model. Filing by id lets the log find any event by id, which checking causes and links needs. Finding the events about a thing now reads the whole log, until an index of what events are about is kept beside it.