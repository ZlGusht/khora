---
identity__uuid: 9f190228-84e3-4e5d-9b36-bcb725e82c31
identity__type: particular
identity__created_at: 2026-11-23
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #18 Event types ids and clock]]"
properties__type: security
properties__priority: 2
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Flagged in the second Observe of [[Issue #18 Event types ids and clock]], from the review of the event kernel.

## Content

An event's hash shows it was not changed, but not who made it. A client that can publish to `khora.events.>` can add any well-formed event, and the log keeps it.

When: NATS permissions for the stream, and signed events when events cross between khoras.