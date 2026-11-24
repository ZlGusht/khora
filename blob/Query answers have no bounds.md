---
identity__uuid: e671915f-e3c8-4709-b88e-f75bce3a5916
identity__type: particular
identity__created_at: 2026-11-24
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #25 Query as a data structure]]"
properties__type: security
properties__priority: 2
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Flagged in the Observe of [[Issue #25 Query as a data structure]], from the review of the `query` module.

## Content

A query is answered in full: no limit on time, rows read or work done. A `Not` or a `Filter` over a scan of everything costs the whole frontier, and nested ones multiply. A stored query is read from the log, so anyone who may write a statement can store one that a reader later runs.

When: before queries come from outside the kernel (K12, the API), or before stored queries run without a person starting them.