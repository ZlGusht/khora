---
identity__uuid: 472dd3c2-59a9-4529-b794-91ffdeec6f80
identity__type: particular
identity__created_at: 2026-11-23
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #18 Event types ids and clock]]"
properties__type: performance
properties__priority: 2
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

The log store from the Issue #19 run filed each record under its primary entity, so one entity's history was read straight from NATS (about 96 ms for 1 000 records in a log of a million). The event log files each event under its id, so it can be found by id.

## Content

`EventLog.about` reads the whole log and keeps the events about a thing. On NATS that is a scan of every event, decoded, on each call: far slower than the per-entity history it replaced, and growing with the log.

When: an index of what events are about, kept beside the log (K6, B1), before anything reads histories at scale.