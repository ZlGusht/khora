---
identity__uuid: f7380529-a1ba-483e-b1f6-5daa3e5d88c4
identity__type: particular
identity__created_at: 2026-11-22
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #18 Event types ids and clock]]"
properties__type: change-later
properties__priority: 3
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Flagged in the Observe of [[Issue #18 Event types ids and clock]], from the review of PR #36.

## Content

The stub's frontier after a write is that write's action alone, with no causes, and none of its actions are in an event graph. It relies on its actions having unique ids, and recognises frontiers only by equality.

When: Replace when surfaces emit events and read at real frontiers (K6, K7).