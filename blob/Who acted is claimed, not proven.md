---
identity__uuid: d221c55d-0086-4699-a00b-40e1df91935d
identity__type: particular
identity__created_at: 2026-11-22
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #18 Event types ids and clock]]"
properties__type: security
properties__priority: 2
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Flagged in the Observe of [[Issue #18 Event types ids and clock]], from the review of PR #36.

## Content

An event's `by` names a principal, a grant and a surface as plain text. Nothing authenticates them and nothing signs the event, so whoever writes the log can claim to be anyone. The hash shows tampering only against an id already trusted.

When: Authentication at the API boundary (K11) and signatures for events crossing between khoras.