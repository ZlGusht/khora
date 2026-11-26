---
identity__uuid: 7b8c1c6f-b090-480e-9a7d-aa68e6e20dba
identity__type: particular
identity__created_at: 2026-11-23
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #18 Event types ids and clock]]"
properties__type: performance
properties__priority: 4
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Flagged in the second Observe of [[Issue #18 Event types ids and clock]], from the review of the event kernel.

## Content

The kernel keeps, per attached surface, the ids of every event it received and emitted, so they are delivered once. The sets only grow.

When: Track a frontier per surface instead, once frontier comparison is cheap.