---
identity__uuid: 06dc8be0-7f40-4d13-a874-1bfbaecaaf8a
identity__type: particular
identity__created_at: 2026-11-20
identity__tags: []
relationships__is_a:
  - "[[Question v1.0.0]]"
relationships__in_context_of:
  - "[[Design]]"
relationships__originates_in:
  - "[[Issue #21 Surface contract types]]"
properties__lean: "Answered: identity comes from events; a lasting thing is identified by the event that created it."
properties__author: agent:claude-opus-5-5
---
Raised by [[Simeon Petkov]] while reviewing the kernel's first Java types. Every definition the kernel writes should obey khora's own language, so each is an entity. The shape and structure of entities are not defined yet.

## The view so far

- An entity carries properties, relationships and behaviors.
- These serve different purposes: structural, functional, descriptional, and others.
- Reifying them lets work be done on them directly: complex relationship queries, analysis of behaviors or learning from them, computations.
- Different kinds of entity can then share the same property, relationship or behavior. This allows emergent behavior and open-ended flexibility.
- So everything is reified and keeps identity.
- Doing that literally is intractable. Virtual entities are where it becomes tractable.
- In code, the question "is this a record or an interface?" then disappears: one rule maps any entity into the language, everywhere.

## To work through

- **Split.** Which parts are reified on their own: properties, relationships, behaviors, and what else? Is the purpose (structural, functional, descriptional) a kind of its own, or a property of the part?
- **Identity and value.** Does every reified part keep identity, including what today looks like a value, such as a frontier or a budget? If two have the same content, are they the same entity?
- **Where behavior comes from.** Is an entity's behavior fixed by its definition, or supplied by whatever realises it? A frontier, for example, behaves differently per origin ([[Frontiers need more than one position per origin]]).
- **Sharing.** When two kinds share a behavior, is it one entity both refer to, or a copy? What happens when it changes version (ADR-0YD decision 5)?
- **Virtual entities.** What is a virtual entity: computed on demand, never stored, or stored only when used? When does one become real, and does its identity survive that?
- **Emergence.** How does a new behavior arise from shared parts, and who or what can recognise it?
- **The kernel's rule.** Which single rule maps an entity into Java, so that every type the kernel defines follows it? Until then, the kernel's types are provisional ([[Kernel types taken from ADRs are provisional]]).