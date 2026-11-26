---
identity__uuid: 23081f68-03af-4092-9034-0ee522380710
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

The salt stops anyone guessing content from an id, but it is stored in clear inside the event. Until the salt is kept and shredded with the content's key (ADR-0YB decision 10), anyone holding an event can still test guesses against its id.

When: Keep the salt with the content's key once encryption and shredding are built.