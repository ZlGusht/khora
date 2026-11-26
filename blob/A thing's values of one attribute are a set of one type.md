---
identity__uuid: bfe6bd23-6a79-4584-855c-32f96b0ba5ce
identity__type: particular
identity__created_at: 2026-11-26
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #23 Graph datastore on Neo4j]]"
properties__type: change-later
properties__priority: 3
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Flagged in the Observe of [[Issue #23 Graph datastore on Neo4j]], from the review of the `datastore-neo4j` module.

## Content

A literal value is kept once per thing and attribute. Asserting a value the thing already has adds nothing, and retracting one of two statements of the same value removes it, where `EventsDatastore` keeps the other. A property holds integers or strings, not both: Neo4j lists hold one type, so an assert that mixes them fails the page it is in, and the datastore stops taking events in there.

When: kinds give attributes a type and a cardinality, or the same literal is asserted twice on purpose. A relationship per value, or a property per kind of value, would lift either limit.