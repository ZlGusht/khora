---
identity__uuid: 7aaaca1c-bfb5-4874-b6f4-c9cdfc85fe63
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

The contract tests for surfaces and authorities are scenarios, each implementation writing its own, run through the kernel; every scenario is also checked against rules that hold for every surface or every authority.

## Rationale

Alternatives: general rules only, which hold for any implementation but say nothing about what one should do; scenarios only, which check behaviour but leave out what nobody thinks to write. Scenarios are the test format the plan proposed (a recorded sequence of events in, expected results out); the rules come free with every scenario.