---
identity__uuid: 6b861e51-8fb1-4230-bd6e-4fcbb31ea051
identity__type: particular
identity__created_at: 2026-11-24
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #25 Query as a data structure]]"
properties__type: performance
properties__priority: 3
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Flagged in the Observe of [[Issue #25 Query as a data structure]], from the review of the `query` module.

## Content

`EventsDatastore` reads every event of the frontier for each answer, and reads the whole log to find its heads when a query names no frontier. Each operator also asks its input for its columns when it is built, which walks the body below it: building a body n operators deep takes n² steps. Bodies read from JSON are at most about 500 deep, which keeps this small.

When: the graph datastore (K6) answers from materialized state through the same `Evaluator`; `EventsDatastore` stays as the reference it is checked against.