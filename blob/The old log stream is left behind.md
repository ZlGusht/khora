---
identity__uuid: d47d9f60-e436-4a38-8e1e-a0e470a61882
identity__type: particular
identity__created_at: 2026-11-23
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #18 Event types ids and clock]]"
properties__type: change-later
properties__priority: 4
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Flagged in the second Observe of [[Issue #18 Event types ids and clock]], from the review of the event kernel.

## Content

The NATS log used the stream `KHORA_LOG`; the event log uses `KHORA_EVENTS`. A server that held the old stream keeps it, unread and with no warning.

When: Nothing is deployed yet; delete old streams by hand, or migrate if anything is deployed first.