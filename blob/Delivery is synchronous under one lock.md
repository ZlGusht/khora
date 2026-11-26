---
identity__uuid: 74a56a50-13d6-4cf5-b7c8-bb1277534536
identity__type: particular
identity__created_at: 2026-11-23
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #18 Event types ids and clock]]"
properties__type: change-later
properties__priority: 3
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Flagged in the second Observe of [[Issue #18 Event types ids and clock]], from the review of the event kernel.

## Content

The kernel appends and delivers inside one call, under one lock, including surface callbacks and log I/O. If an append's outcome is unknown, the kernel neither updates its heads nor delivers; an action a surface emits that the authority rejects is dropped with only a log line.

When: Asynchronous delivery, and a way to recover a pending append, before surfaces run remotely.