---
identity__uuid: 810e00df-b69f-40e6-984a-e57d3d2fbd6b
identity__type: particular
identity__created_at: 2026-11-22
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #18 Event types ids and clock]]"
properties__type: performance
properties__priority: 4
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Flagged in the Observe of [[Issue #18 Event types ids and clock]], from the review of PR #36.

## Content

Creating an event computes its canonical JSON and hash twice; JSON objects copy their members whenever they are built; ids are checked with regexes compiled on each call.

When: Tune once the log store (K2) shows where time goes.