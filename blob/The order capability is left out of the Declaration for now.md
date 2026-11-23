---
identity__uuid: 41072706-43d5-4d77-9ccc-cf2958af8bdf
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
properties__quote: "let's leave order aside for now and mark it as provisional"
properties__author: agent:claude-opus-5-5
---
## Statement

Declaration has no order capability yet, although ADR-0XY decision 2 lists one. The Declaration type says so in the code, as provisional.

## Rationale

The ADR names order but does not say what it means or which values it takes. One reading: whether a surface's changes come in a total order, per entity, or in none.