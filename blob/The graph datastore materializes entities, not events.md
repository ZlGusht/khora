---
identity__uuid: 732accbe-14e2-4af5-91a8-b33327762e80
identity__type: particular
identity__created_at: 2026-11-26
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Surfaces]]"
relationships__originates_in: "[[Issue #23 Graph datastore on Neo4j]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
relationships__about:
  - "[[Issue #23 Graph datastore on Neo4j]]"
properties__decided_on: 2026-11-26
properties__status: active
properties__kind: design
properties__quote: "the datastore interprets the assertions as entities with attributes and relationships, not storing the events [...] If I create a new entity, I want to store the entity, not the event. [...] past and branch reads are far into the future of funcionality, we don't need them now. Retraction can work as simple delete now, you can get the address from the target event. Attribute ids are for the log now, the datastore doesn't care, it only materializes what was asserted."
properties__author: agent:claude-opus-5-5
---
## Statement

The graph datastore materializes entities, not events.

1. It keeps what the events assert as it stands now. An entity is a node, its literal attributes are properties, and a reference is a relationship. Events and statements are not stored.
2. It answers only at everything it has taken in. Past and branch reads are not needed now.
3. A retraction deletes what its target asserted. The target is found through its event.
4. Attribute identities are used as they are. The datastore gives them no meaning; it materializes what was asserted.

## Rationale

The first version kept statements and events as nodes, so that it could answer at any frontier. That stored the log's shape a second time. A datastore is a materialized view of the log's current state; the log keeps the history.