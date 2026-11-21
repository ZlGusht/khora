---
identity__uuid: 95814ada-ba3d-4186-ace1-b23518007943
identity__type: particular
identity__created_at: 2026-11-19
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Surfaces]]"
relationships__originates_in:
  - "[[Issue #21 Surface contract types]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
properties__decided_on: 2026-11-19
properties__status: "active"
properties__kind: "design"
properties__quote: "changes can be left out without context for now"
properties__author: agent:claude-opus-5-5
---
## Statement

changes(since) takes no context, as in ADR-0XY decision 1. This is provisional.

## Rationale

Without a context, a change feed has no principal, so access to it cannot be checked. Adding a context later changes the signature of ChangingSurface.