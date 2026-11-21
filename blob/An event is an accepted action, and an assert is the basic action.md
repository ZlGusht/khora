---
identity__uuid: ed30b32c-46f4-4348-8774-11f40f139117
identity__type: particular
identity__created_at: 2026-11-21
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Log]]"
  - "[[Design]]"
relationships__originates_in:
  - "[[Issue #18 Event types ids and clock]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
relationships__about:
  - "[[Events are the basic unit of khora]]"
properties__decided_on: 2026-11-21
properties__status: "active"
properties__kind: "design"
properties__quote: "I would go as far as to claim that an assert is still an action. [...] we don't need decisions either. [...] yes, drop it and go"
properties__author: agent:claude-opus-5-5
---
## Statement

An event records an action khora accepted. Every action has one shape: the id of its definition, and its arguments. Definitions are things created by asserts, so the definition's id also says which vocabulary and version the action belongs to; there is no separate vocabulary field. An assert is the basic vocabulary's action. There are no separate outcome or effect events. Rejected actions are not appended yet; keeping them is to be added later.

## Rationale

An action that states what should be true already holds its result, so an outcome and effects restating it are redundant. A merge, or a surface action translated down, is a further action caused by the first. Acceptance can be re-checked at the action's own frontier, because it names its causes and grant.

Rejections are to be kept ("rejections should be kept, but not necessarily now"), for audit and for telling a principal why a write did not land. A rejection can name the refused action by its id, though the action was never appended.

This replaces the action, outcome, effect and observation shapes of ADR-0YA decision 3 and ADR-0YB decision 3.