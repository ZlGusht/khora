---
identity__uuid: db1dad07-3cf0-4f64-81b8-2ab457aee617
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
relationships__supersedes:
  - "[[An event is an accepted action, and an assert is the basic action]]"
properties__decided_on: 2026-11-22
properties__status: "active"
properties__kind: "design"
properties__quote: "I feel you have it the other way around, an event is not an accepted action, an accepted action is an event. [...] you can apply the event envelope as well"
properties__author: agent:claude-opus-5-5
---
## Statement

An event is an envelope: its id, causes, recorded and occurred times, salt, and a body. An accepted action is one kind of body, carrying who acted and the action. Other kinds of event are added as further kinds of body. Every action has one shape: the id of its definition, and its arguments. An assert is the basic vocabulary's action. There are no separate outcome or effect events. Rejected actions are not appended yet; keeping them is to be added later.

## Rationale

Event is the broader category. Who acted belongs to an action, not to every event, so it moves into the accepted action's body. Adding a kind of body later is a change the compiler flags wherever bodies are handled. The rest carries over from the superseded Decision.