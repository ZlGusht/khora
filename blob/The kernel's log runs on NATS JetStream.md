---
identity__uuid: d3e59254-d5e8-4218-ac75-6ba9375d1923
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

The khora log is stored in NATS JetStream, version 2.12 or later. This settles the log store question in ADR-0YB and plan task D2.

## Rationale

Alternatives: Kafka or Redpanda, and an embedded log on object storage. NATS checks the expected position on append, which conditional writes will need; gives one entity's history through subject filters; runs as one light binary; and is Apache 2.0. Kafka has no conditional append and is heavier to run; its strengths, tiered storage and a Neo4j connector, are not needed now. Redpanda's tiered storage needs a paid licence and its BSL licence is a risk for hosted khora. An embedded log is too much to build for the MVP. Kafka was named in an earlier call; this choice replaces it for now.