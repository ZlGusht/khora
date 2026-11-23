---
identity__uuid: 5348dab1-5281-4bae-901a-d0fe1debee47
identity__type: particular
identity__created_at: 2026-11-23
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

Flagged in the second Observe of [[Issue #18 Event types ids and clock]], from the review of the event kernel.

## Content

Named links (`at`, `based_on`, `source`) are plain strings that no definition declares; nothing checks that `based_on` points at a read.

When: Define each basic action's links with the read, translation and behavior events.