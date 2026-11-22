---
identity__uuid: 7d97f055-3b1e-4439-84c0-5229ba66affe
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
properties__status: superseded
properties__kind: design
properties__quote: every assert keeps an id and it helps identify multiple things, entities, their properties, relationships, behaviors
properties__author: agent:claude-opus-5-5
---
## Statement

An assert's id is the identity of what it asserts: an entity, a property of an entity, a relationship between entities, or a behavior bound to an entity. Removing something is an assert about the assert that made it. Retraction has no category of its own.

## Rationale

Every part of an entity then has identity and can be referred to, annotated or reasoned about, with one rule for all of them. The shapes of an assert's arguments are provisional and are settled during execution. The kernel has to recognise a retraction when it builds state, so retraction is a reserved attribute of the basic vocabulary.