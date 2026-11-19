---
identity__uuid: 0a25f940-0b9c-48a6-847d-7e6a1afc3d71
identity__type: particular
identity__created_at: 2026-11-19
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Runtime]]"
relationships__originates_in:
  - "[[Issue #21 Surface contract types]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
properties__decided_on: 2026-11-19
properties__status: "active"
properties__kind: "design"
properties__quote: "language will be java, yes"
properties__author: agent:claude-opus-5-5
---
## Statement

The kernel is implemented in Java. This settles stage 0 task D1 of the implementation plan.

## Rationale

No rationale was stated when it was decided. With a JVM language, the planner lean in ADR-0YC becomes Calcite rather than DataFusion.