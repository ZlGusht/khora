---
identity__uuid: 342cdaf6-ac02-4692-8766-36ecdfc4cde1
identity__type: particular
identity__created_at: 2026-11-18
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Kit]]"
relationships__originates_in:
  - "[[Call 2026-11-18 Loop levels for development]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
properties__decided_on: 2026-11-18
properties__status: "active"
properties__kind: "rule"
properties__quote: "sync runs on opening a run, closing it and merging."
properties__author: agent:claude-opus-5-5
---
## Statement

Sync between the blob and GitHub runs at three points: when a run opens, when it closes, and when its work merges.

## Rationale

Alternative discussed: sync at every phase boundary. Declined.