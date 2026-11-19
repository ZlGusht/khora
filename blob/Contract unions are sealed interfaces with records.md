---
identity__uuid: c07954da-e992-4d3b-8866-7215326bc576
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
properties__quote: "Accept all"
properties__author: agent:claude-opus-5-5
---
## Statement

A contract type that is one of several shapes carrying data is a sealed interface whose variants are records. A choice without data is an enum.

## Rationale

Agent's proposal, accepted. A switch over a sealed interface fails to compile when a case is missing. Class hierarchies and exceptions were the alternatives; they lose that check.