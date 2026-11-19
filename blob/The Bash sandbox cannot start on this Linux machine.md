---
identity__uuid: 1a0744df-3a4e-4456-b7e5-fe0d5775f2c9
identity__type: particular
identity__created_at: 2026-11-19
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #21 Surface contract types]]"
relationships__relates_to:
  - "[[Kit]]"
properties__author: agent:claude-opus-5-5
properties__priority: 2
properties__resolved: false
properties__type: thought
---
## Context

Every sandboxed Bash command in the Issue #21 run failed before running: `apply-seccomp: write /proc/self/setgroups ... Permission denied`. All commands ran unsandboxed.

## Content

Nested user namespaces are restricted on this machine. Agents here run without the sandbox until that is changed; `/sandbox` manages it.