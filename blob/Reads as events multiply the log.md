---
identity__uuid: 95f80d4b-aa73-4098-a516-1ec0b7a6cbbb
identity__type: particular
identity__created_at: 2026-11-22
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #18 Event types ids and clock]]"
properties__type: performance
properties__priority: 3
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Flagged in the Observe of [[Issue #18 Event types ids and clock]], from the review of PR #36.

## Content

[[Reads are events]] records every read, and reads outnumber writes many times over. The in-memory event graph keeps everything.

When: K2's store and the retention of reads by scope.