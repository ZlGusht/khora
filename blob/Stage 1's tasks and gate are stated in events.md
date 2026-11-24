---
identity__uuid: a501b1da-bb78-48bc-ac0a-6cf1bcc5d8a2
identity__type: particular
identity__created_at: 2026-11-24
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Product]]"
relationships__originates_in: "[[Milestone Stage 1 kernel core]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
relationships__about:
  - "[[Milestone Stage 1 kernel core]]"
properties__decided_on: 2026-11-24
properties__status: active
properties__kind: design
properties__quote: "rework k9, k5 [...] k8 is fine as is, it was rather part of a search functionality that would probably be based on datastoresl [...] ok about the rest"
properties__author: agent:claude-opus-5-5
---
## Statement

Stage 1's tasks and gate are stated in events.

1. K5 is contract tests for surfaces and authorities: events in; out, the actions a surface emits, the verdicts an authority gives, and the statements read at a frontier.
2. K8's `Query` is the search a planner hands to datastores; the graph datastore (K6) answers it first.
3. K9 is the conflict check in a general authority, through the graph datastore. A write is confirmed by its event; there is no separate outcome.
4. B1 shows who acted for each event, from the log. Recent events in a scope wait for scopes (P4).
5. B2 creates, changes and views things.
6. The gate to stage 2: an accepted action is an event, statements are read at a frontier, the history view shows who did what, and the contract tests pass.

## Rationale

The tasks and the gate spoke of outcomes, effects, declared capabilities and change streams, none of which the event kernel keeps (`An accepted action is one kind of event`, `Effects are not a notion of their own`, `Surfaces receive and emit events by interest`). #36 already does conditional writes in its stub authority. K6 stays as it is.