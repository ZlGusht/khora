---
identity__uuid: 85638f05-9922-4752-9da2-0cc1470b907c
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

The kernel delivers every event a surface's interests match, with no check of what the surface may see. Reads are events, so a surface interested in reads learns who read what.

When: Access labels checked at delivery, before anything reaches a surface (ADR-0YC decision 4).