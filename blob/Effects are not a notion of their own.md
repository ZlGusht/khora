---
identity__uuid: d98b23c8-4ec0-42fa-ac61-d8ce18472196
identity__type: particular
identity__created_at: 2026-11-23
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Surfaces]]"
  - "[[Design]]"
relationships__originates_in:
  - "[[Issue #18 Event types ids and clock]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
relationships__about:
  - "[[An accepted action is one kind of event]]"
properties__decided_on: 2026-11-23
properties__status: "active"
properties__kind: "design"
properties__quote: "I feel effects is also outdated now, no? effects are accepted asserts?"
properties__author: agent:claude-opus-5-5
---
## Statement

Effects are not a notion of their own. An accepted assert is the change; nothing separate records what it brought about, and surfaces declare no effects.

## Rationale

ADR-0YA and ADR-0YB made effects separate events caused by an outcome. With an accepted action as the event that holds its tuples, an effect would restate it.