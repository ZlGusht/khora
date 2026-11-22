---
identity__uuid: 9e65403f-cd99-4aa9-88b1-35ee5e3ba86e
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

RFC 8785 writes numbers as JavaScript does, as doubles. Recorded times are above 2^53, so another language's RFC 8785 library would write them differently and compute a different id. The Decision says RFC 8785; the code writes exact integers.

When: Encode large integers as strings, or record the deviation as a Decision, before another implementation or K2's stores compute ids.