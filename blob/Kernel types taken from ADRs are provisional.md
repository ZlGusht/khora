---
identity__uuid: cacbb2f7-dfdc-4fff-b3cf-0644361f81a3
identity__type: particular
identity__created_at: 2026-11-19
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Product]]"
relationships__originates_in:
  - "[[Issue #21 Surface contract types]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
properties__decided_on: 2026-11-19
properties__status: "active"
properties__kind: "rule"
properties__quote: "keep in mind the adr is not ideal so we should be careful about how much we trust it"
properties__author: agent:claude-opus-5-5
---
## Statement

The ADRs are a starting point, not a specification. A kernel type taken from an ADR is kept minimal and marked provisional in the code. Where an ADR looks wrong or incomplete, the choice is agreed as a Decision before it is coded.

## Rationale

The ADRs are not considered reliable enough to be copied into code as they stand.