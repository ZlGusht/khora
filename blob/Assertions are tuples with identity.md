---
identity__uuid: f20df0c5-527d-4c42-8228-ca417cafe580
identity__type: particular
identity__created_at: 2026-11-22
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
  - "[[An assert's id is the identity of what it asserts]]"
properties__decided_on: 2026-11-22
properties__status: "active"
properties__kind: "design"
properties__quote: "assertions should be tuples and that they should be given identity as well. This means that an assertion is (identity, attribute label, attribute value) [...] entity = (,,) would be the way to instantiate entities."
properties__author: agent:claude-opus-5-5
---
## Statement

An assertion is a tuple (identity, attribute, value), and the assertion has identity of its own. The empty tuple `(,,)` instantiates an entity: the entity's identity is the assertion's. A full tuple's identity is the identity of that attribute of that thing, so anything can be asserted about it, including its retraction.

## Rationale

One standard syntax gives everything structure while leaving it open to interpretation: entities, properties, relationships and their properties, bindings, retractions and definitions all share it, so surfaces can treat them the same. It follows Datomic's facts and RDF statements with identity. Costs: rebuilding a whole entity means gathering many tuples, so wide reads need materialized surfaces; whether a new value replaces or adds depends on the attribute, which needs a definition; and values must say whether they are literals or references.