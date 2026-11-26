---
identity__uuid: 7a7bb809-5370-47f5-a45a-76dba5d6b9c0
identity__type: particular
identity__created_at: 2026-11-26
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #22 Contract tests for surfaces and authorities]]"
  - "[[Security]]"
properties__type: idea
properties__priority: 3
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Issue #22 asked whether the contract tests cover security, or a separate task does. The plan review had noted that security tests are missing from the testing approach.

## Content

The contract tests leave security out: the kernel checks no access yet ([[Delivery checks no access]]), so there is nothing to test. When grants and scopes reach delivery and the authority (P3 to P5), add rules to the contracts: a surface receives only events its principal may read; an authority rejects an action its caller's grant does not allow; a rejection reveals nothing the caller may not read.

When: a separate issue, with P5 (access labels and enforcement).