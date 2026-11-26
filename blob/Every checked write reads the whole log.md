---
identity__uuid: c93bf8b2-4793-4f35-8226-bf84b2b697e6
identity__type: particular
identity__created_at: 2026-11-23
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #18 Event types ids and clock]]"
properties__type: performance
properties__priority: 3
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Flagged in the second Observe of [[Issue #18 Event types ids and clock]], from the review of the event kernel.

## Content

The projects authority scans the whole log for conflicts on every write based on a read. On NATS, each append also looks every event up by id before publishing, one round trip per event.

When: An index of statements by identity and attribute (K6); batch the lookups.