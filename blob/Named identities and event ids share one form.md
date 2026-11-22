---
identity__uuid: 543646fa-6e0c-4a0f-8266-5d456ce99f20
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

A well-known name's identity is `sha256:` of the name, and an event id is `sha256:` of the event. Nothing in the string says which, so a reference could be read as either.

When: Give named identities a form of their own, such as a distinct prefix, before definitions are entities.