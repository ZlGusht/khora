---
identity__uuid: 709209b4-52f8-4f97-ac0c-3f24058ea5be
identity__type: particular
identity__created_at: 2026-11-25
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #23 Graph datastore on Neo4j]]"
properties__type: security
properties__priority: 2
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Flagged in the Observe of [[Issue #23 Graph datastore on Neo4j]], from the review of the `datastore-neo4j` module.

## Content

`Neo4jDatastore` trusts its database whole: it checks neither the content against the log nor a snapshot's rows against their events, beyond the last event's id at the snapshot's position. The development compose file has a default password and no TLS.

When: the datastore runs anywhere but a developer's machine, with the security model (ADR-0YE).