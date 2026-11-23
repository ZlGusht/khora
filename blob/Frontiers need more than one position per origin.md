---
identity__uuid: 7e06c9aa-bf49-42e8-b832-850cbf839d19
identity__type: particular
identity__created_at: 2026-11-19
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #21 Surface contract types]]"
relationships__relates_to:
  - "[[Issue #18 Event types ids and clock]]"
  - "[[Surfaces]]"
properties__type: thought
properties__priority: 2
properties__resolved: false
properties__author: agent:claude-opus-5-5
---
## Context

Issue #21 first wrote `Frontier` as a map from origin name to a whole number, with `reaches` and `advance`. Most of the review findings were about that meaning. In the review with [[Simeon Petkov]], frontiers were found to need far more than that, and to belong to K1 (#18). K4 now keeps `Frontier` as a placeholder that only names a frontier: a surface recognises only frontiers it reported itself, and compares them by equality.

What the ADRs already say:
- A frontier is one position per origin a read depends on (ADR-0XY, Terms; decision 4).
- The khora log is a causal hash graph; a frontier is valid only if it never includes an event without its causes (ADR-0XB decision 5).
- A surface advances only to frontiers its inputs have reached, and nothing at a frontier is delivered before the access labels reach it (ADR-0YC decision 4).
- Unconfirmed writes are a short-lived branch (ADR-0YB decision 8).

## Content

Cases a frontier has to handle, for K1:

1. **The khora log is not a line.** Concurrent events have no order between them. A position in the log is a set of heads in a graph, not a number. Comparing two frontiers can answer "neither", and a frontier has to be checked for causal closure.
2. **Each external origin has its own kind of position.** One log sequence number (Postgres), one offset per partition (Kafka), a commit in a graph (Git), or only khora's observation time (an API without positions), comparable only within clock skew. Positions differ in type, and comparison differs per origin.
3. **Layered surfaces.** A surface built on an adapter and on a view over the log reports its frontier in terms of the origins at the bottom, not the layers between. Each layer combines its inputs' frontiers, cannot pass the slowest input or the access labels, and keeps only the origins a read actually touched, which can depend on the data.
4. **Writes conditioned across origins.** A write to the log conditioned on a read that also depended on an external origin cannot check that origin's part in the same step. Either the condition is split, with a weaker guarantee, or a surface declares it conditions only on its own origin.
5. **Branches.** A read inside an unconfirmed write's branch has a frontier that includes events not on the main log. Confirmation is when the branch's frontier becomes reachable from the main one.
6. **Other deployments.** Another khora's log is an origin with its own graph and clock (interop: review).

So a frontier needs: positions of different types per origin; comparison that can say "neither"; combining; restricting to the origins a read touched; a validity check; and possibly a branch. These are behaviors, and they differ per origin.

This ties to the open question of what entities reify. Entities carry properties, relationships and behaviors; if those are reified, with identity, different kinds can share them. A frontier would then be an entity whose behavior comes from the origins it describes, rather than a choice between a Java record and an interface. Virtual entities are where reifying everything stays tractable. That needs its own development; frontiers are one of its first concrete cases.