---
identity__uuid: 1b8a4d1d-3659-4d0a-bd18-41065d59e346
identity__type: particular
identity__created_at: 2026-11-19
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Product]]"
  - "[[Log]]"
relationships__originates_in:
  - "[[Issue #18 Event types ids and clock]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
relationships__supersedes:
  - "[[K1 runs in its own Issue run after K4]]"
properties__decided_on: 2026-11-19
properties__status: "active"
properties__kind: "operational"
properties__quote: "while another session works on K4, let's get started with issue K1"
properties__author: agent:claude-opus-5-5
---
## Statement

The Issue #18 run (K1, event types, ids and clock) opens now, in parallel with the open Issue #21 run (K4), on its own branch and with its own pull request. It still replaces the EventId placeholder; whichever pull request merges second rebases onto the other.

## Rationale

The two issues can be worked by two sessions at once. K4 has no code yet, so the overlap is only the EventId type.