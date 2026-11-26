---
identity__uuid: 79db72cb-1d7c-49be-ae3a-46f7e7fe6967
identity__type: particular
identity__created_at: 2026-11-22
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #18 Event types ids and clock]]"
properties__type: performance
properties__priority: 2
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Flagged in the Observe of [[Issue #18 Event types ids and clock]], from the review of PR #36.

## Content

Checking, comparing and combining frontiers walk every event a frontier holds, on every call, with no index. Once conditional writes check frontiers, each write costs time in proportion to the history.

When: An index of ancestry or a summary per frontier (K7, K9).

Update: each submit now walks the history three or four times (the caller's frontier, the authority's conflict check, `Causality`, and for a read the statements), each delivery combines frontiers (so catching up is quadratic), and on NATS every step of a walk is a round trip.