---
identity__uuid: f2baed66-f83f-42b0-919a-eda173f66b2a
identity__type: particular
identity__created_at: 2026-11-18
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Log]]"
relationships__originates_in:
  - "[[Issue #19 Log store on NATS]]"
relationships__decided_by:
  - "[[Hristiyan Ganchov]]"
properties__decided_on: 2026-11-18
properties__status: active
properties__kind: design
properties__author: agent:claude-opus-5-5
---
## Statement

By default NATS runs with `sync: always`: an append is confirmed only once it is written to disk. For now the setting is selectable per deployment, so the faster NATS default can be chosen where losing the last seconds of writes on a crash is acceptable.

## Rationale

The log is the only source of truth and the first version commits synchronously, so a write a person has seen confirmed must survive a crash or power loss. The NATS default syncs about every two minutes and is faster. The benchmark measures both, so the cost is known.