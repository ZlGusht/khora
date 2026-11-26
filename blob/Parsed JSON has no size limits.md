---
identity__uuid: 5018af25-eba4-47e5-b98d-93e4aacced02
identity__type: particular
identity__created_at: 2026-11-22
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #18 Event types ids and clock]]"
properties__type: security
properties__priority: 3
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Flagged in the Observe of [[Issue #18 Event types ids and clock]], from the review of PR #36.

## Content

Reading an event limits nesting to 1000 levels, but strings, arrays and objects can be any size. Untrusted text can exhaust memory.

When: Size limits when events arrive from outside (K11, K12, other khoras).