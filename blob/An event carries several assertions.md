---
identity__uuid: 60da60ce-79bd-40f7-ad32-3d9b31a43f5d
identity__type: particular
identity__created_at: 2026-11-22
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Log]]"
  - "[[Design]]"
relationships__originates_in:
  - "[[Issue #18 Event types ids and clock]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
relationships__about:
  - "[[Assertions are tuples with identity]]"
properties__decided_on: 2026-11-22
properties__status: "active"
properties__kind: "design"
properties__quote: "let an event carry several tuples"
properties__author: agent:claude-opus-5-5
---
## Statement

An assert action carries one or more assertions. Each assertion's identity is its event's id and its index in the event, written `<event id>/<index>`. The assertions of one event are appended together or not at all.

## Rationale

One tuple per event multiplied the events and their overhead, and left no way to write several tuples atomically, which K3 needs. The tuple model is unchanged.