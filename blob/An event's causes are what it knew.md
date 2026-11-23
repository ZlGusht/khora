---
identity__uuid: eee7b4e4-edc0-4a3a-aca0-48ba2deb2da4
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
properties__decided_on: 2026-11-22
properties__status: "active"
properties__kind: "design"
properties__author: agent:claude-opus-5-5
---
## Statement

An event's causes are the events it happened after and knew of, and nothing else. Every other relation between events is a named link in the event's body.

1. Causes mean knowledge only. Frontiers, ordering, conflict checks and replication use only this relation.
2. What a read read and at which frontier, which event a translation was made from, and which event triggered a behavior are named links in the body. Each kind of event defines its own.
3. Every event a named link points to is in the event's causal past. Nothing can read, translate or react to an event it did not know of.
4. A surface's event translated down is named as the source in the body of each basic event made from it, and is among their causes. This is how point 5 of [[Events are the basic unit of khora]] is recorded.

## Rationale

Causes were taking on several meanings at once: K1 defined them as what an event read, and [[Events are the basic unit of khora]] used "caused by" for translation. With both in one unlabelled list, a translation and a later action by someone who saw the same event look alike, so "what did this surface event become" cannot be answered.

- Lamport's happened-before, vector clocks and Git parents keep the causal relation to knowledge alone. W3C PROV keeps "used", "was derived from" and "was informed by" as separate relations.
- Rule 3 keeps frontiers sound: a frontier holds everything a named link inside it refers to.

Costs:
- Whoever records an event must know what the actor knew, usually the frontier the surface showed them, not only what they read.
- Each kind of event defines its named links, so the basic read, translation and behavior events need them before they are used.