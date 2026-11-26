---
identity__uuid: 82958f30-9f1a-4791-9831-b86913480ef0
identity__type: particular
identity__created_at: 2026-11-22
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #18 Event types ids and clock]]"
properties__type: change-later
properties__priority: 2
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Flagged in the Observe of [[Issue #18 Event types ids and clock]], from the review of PR #36.

## Content

Under [[An event's causes are what it knew]], what a read read, a translation's source and a behavior's trigger are named links in the body, each pointing into the causal past. `Basic.READ` is only a definition id; no named links exist and nothing checks they point into the causal past.

When: Define the basic read, translation and behavior events with their links (K10 and the surface reshaping).

Update: accepted actions now carry named links, checked to be in the causal past before an event is appended (`Causality`). A read links `at` the frontier it read, a write `based_on` its read, and what a surface emits links its `source`. A behavior's trigger has no link yet, and no link names are defined per kind of action.