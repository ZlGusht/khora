---
identity__uuid: 1e4c8344-9370-4e43-aea3-fd2608c8edd2
identity__type: particular
identity__created_at: 2026-11-19
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Surfaces]]"
relationships__originates_in:
  - "[[Issue #21 Surface contract types]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
properties__decided_on: 2026-11-19
properties__status: "active"
properties__kind: "design"
properties__quote: "Accept all"
properties__author: agent:claude-opus-5-5
---
## Statement

changes(since) returns a ChangeFeed with a blocking next() and close(), read on virtual threads. java.util.concurrent.Flow can be adapted to it later.

## Rationale

Agent's proposal, accepted. java.util.stream.Stream is for finite collections, not a live feed. Flow.Publisher was the alternative: built in, but verbose and hard to get right without a library.