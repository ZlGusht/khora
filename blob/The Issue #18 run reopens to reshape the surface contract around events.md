---
identity__uuid: d260e29c-4260-4a30-bbdd-de999f57aa32
identity__type: particular
identity__created_at: 2026-11-22
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Surfaces]]"
relationships__originates_in:
  - "[[Issue #18 Event types ids and clock]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
relationships__about:
  - "[[Every surface has one event stream, read from a frontier]]"
properties__decided_on: 2026-11-22
properties__status: "active"
properties__kind: "operational"
properties__quote: "ok for the questions"
properties__author: agent:claude-opus-5-5
---
## Statement

The Issue #18 run reopens to reshape the surface contract and the stub around events, on the same branch and pull request (#36). It replaces the proposed follow-up issue "The surface contract speaks events".

## Rationale

Neither #35 nor #36 is merged, so the work stays in one pull request. The code still had parts that do not treat events as the base.