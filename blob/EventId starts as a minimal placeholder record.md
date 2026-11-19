---
identity__uuid: bbacbd74-7d78-430e-a8e3-55d6ab1a3108
identity__type: particular
identity__created_at: 2026-11-19
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Surfaces]]"
  - "[[Log]]"
relationships__originates_in:
  - "[[Issue #21 Surface contract types]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
properties__decided_on: 2026-11-19
properties__status: "active"
properties__kind: "design"
properties__quote: "we can use a minimal placeholder record for eventid"
properties__author: agent:claude-opus-5-5
---
## Statement

The surface contract (K4, #21) uses a minimal placeholder record for EventId, so the context's cause can be typed before K1 (#18) defines event ids.

## Rationale

The same people take on K1 as well, so the placeholder is replaced there. The alternative was to wait for K1 before K4.