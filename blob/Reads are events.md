---
identity__uuid: 8bd5e6d9-b5db-4d22-b857-dcbaf2fb3d31
identity__type: particular
identity__created_at: 2026-11-22
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Design]]"
relationships__originates_in:
  - "[[Issue #21 Surface contract types]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
relationships__answers:
  - "[[Whether reads are events]]"
properties__decided_on: 2026-11-22
properties__status: "active"
properties__kind: "design"
properties__quote: "reads are events indeed, as they are actions done by someone."
properties__author: agent:claude-opus-5-5
---
## Statement

A read is an action someone takes, so it is an event like any other: an accepted action whose definition is the basic read. It is recorded whether or not anything follows from it.

## Rationale

- [[Events are the basic unit of khora]] identifies data by the traces it leaves. Who read what, at which frontier, is such a trace, and access, audit and conditional writes depend on it.
- A write follows from the read it was based on, so the read is what its conditions refer to.

Costs:
- Reads can outnumber writes many times over, so the log grows much faster. Storing it is K2's concern.
- At the PRD's scale, which reads are kept in full may have to depend on scope.