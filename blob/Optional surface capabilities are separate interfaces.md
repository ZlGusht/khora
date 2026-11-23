---
identity__uuid: e284be90-7cf5-40e4-8ec2-ce20f2fbaf1d
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
properties__status: superseded
properties__kind: design
properties__quote: Accept all
properties__author: agent:claude-opus-5-5
---
## Statement

Surface holds declares and read. WritableSurface adds write and ChangingSurface adds changes. The contract tests check that a surface's declaration matches the interfaces it implements.

## Rationale

Agent's proposal, accepted. Calling write on a read-only surface is then a compile error, not a runtime refusal. One interface with refusing default methods was the alternative.