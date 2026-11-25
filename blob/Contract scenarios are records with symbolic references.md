---
identity__uuid: 9cfc6c6f-5181-481d-9f05-40fd66588722
identity__type: particular
identity__created_at: 2026-11-25
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Surfaces]]"
relationships__originates_in:
  - "[[Issue #22 Contract tests for surfaces and authorities]]"
relationships__decided_by:
  - "[[Hristiyan Ganchov]]"
properties__decided_on: 2026-11-25
properties__status: "active"
properties__kind: "design"
properties__author: agent:claude-opus-5-5
---
## Statement

A contract scenario is plain data: records naming its steps, with symbolic references to earlier steps and their tuples, resolved when it runs. A small Java builder writes them. JSON scenario files can come later without changing the format.

## Rationale

Event ids, salts and recorded times change from run to run, so a scenario names steps instead. Alternatives: a builder over lambdas, quickest but code that can never become data; JSON files now, logic as data but a loader and a symbolic syntax before anything is tested.