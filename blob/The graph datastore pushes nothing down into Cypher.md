---
identity__uuid: ab34d3cc-da16-4155-8a74-6a75e2c15ec1
identity__type: particular
identity__created_at: 2026-11-25
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #23 Graph datastore on Neo4j]]"
properties__type: performance
properties__priority: 3
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Flagged in the Observe of [[Issue #23 Graph datastore on Neo4j]], from the review of the `datastore-neo4j` module.

## Content

`Evaluator` answers over three Cypher lookups; filters, negation and aggregates run in memory, and a `Has` condition asks once per thing and attribute. A scan of an attribute reads every `Thing`, since a property keyed by an attribute's identity has no index.

When: pushdown and planning (P2), or a scan is slow. Indexes per attribute, or a label per attribute, would avoid the full read.