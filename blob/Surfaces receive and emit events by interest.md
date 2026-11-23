---
identity__uuid: 5d238f64-bd32-4a7d-8132-c5490fc209f1
identity__type: particular
identity__created_at: 2026-11-23
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Surfaces]]"
  - "[[Design]]"
relationships__originates_in:
  - "[[Issue #18 Event types ids and clock]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
relationships__supersedes:
  - "[[Every surface has one event stream, read from a frontier]]"
properties__decided_on: 2026-11-23
properties__status: "active"
properties__kind: "design"
properties__quote: "let's only use the language of events [...] okay, go with emission and let's see what it boils down to"
properties__author: agent:claude-opus-5-5
---
## Statement

Surfaces speak only the language of events. A surface declares its interests, what events are and what they are about, and receives every accepted event that matches, each after its causes. It emits events of its own in response. There are no streams or connections between surfaces: the log is the one shared place events live, and routing follows from interests.

A request is submitted to the authority for what it is about, which accepts or rejects it. Only an accepted action becomes an event. A receiver's frontier is the set of matching events it has taken in; attaching from a frontier sends it the matching events not in it, then new ones. Delivery can be synchronous or asynchronous; the events are the same either way.

## Rationale

The request-and-response contract of ADR-0XY, with changes as target and value, kept a second model beside events. Emission by interest needs no notion beyond events except interests and which surface is the authority for a thing. The single event stream per surface carried over from ADR-0XY's one `changes` method; nothing argued for exactly one.

Costs: who may receive what rests on grants and scopes checked at delivery; matching interests at scale needs filtering where events are emitted; surfaces that react to their own consequences can loop. Connections could be added later as a restriction on top of interests.