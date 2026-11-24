---
identity__uuid: ffb29da4-91f3-4ae6-936b-6ba254c78481
identity__type: particular
identity__created_at: 2026-11-24
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #25 Query as a data structure]]"
properties__type: finding
properties__priority: 3
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Flagged in the Observe of [[Issue #25 Query as a data structure]], from the review of the `query` module.

## Content

Retracting the empty tuple that brought a thing into being drops it from `Scan` of things, but its other statements still hold, so a scan of an attribute still finds it. `Statements.about` behaves the same way. The `DatastoreContract` pins this (`statementsAboutARetractedThingStillShow`) so every datastore agrees.

When: decide whether retracting a creation retracts what is said about the thing, before the generic entity view (B2) lets people delete things.