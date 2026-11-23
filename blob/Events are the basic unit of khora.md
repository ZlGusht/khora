---
identity__uuid: 60476182-216c-48d4-a6f4-f0f961829930
identity__type: particular
identity__created_at: 2026-11-21
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Design]]"
relationships__originates_in:
  - "[[Issue #21 Surface contract types]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
relationships__answers:
  - "[[Which parts of an entity are reified and keep identity]]"
properties__decided_on: 2026-11-21
properties__status: "active"
properties__kind: "design"
properties__quote: "data is fluid and it can be identified by the traces it leaves - events. Events are the basic unit that acts as the glue between different surfaces."
properties__author: agent:claude-opus-5-5
---
## Statement

Data is identified by the traces it leaves: events. Events are the basic unit of khora and the glue between surfaces.

1. Everything that happens is an event: creating an entity, adding an attribute or a relationship to it, binding a behavior to it, and defining a kind, an attribute or a behavior. Each carries identity, so anything can be reified when it is needed.
2. A lasting thing is identified by the event that created it. Later events about it refer to that event. An entity is its creation event and every event that refers to it.
3. Surfaces are connected by event streams. A surface takes in the events of what it builds on and emits events of its own. Coherence across the surface graph comes from every surface reading at a frontier its inputs have reached.
4. Surfaces may define their own events. A surface's own event is translated down into the language its origins store and manipulate.
5. A surface's own event is recorded, and the events it translates into are recorded as caused by it.
6. There is one small, fixed basic vocabulary: the language origins store and manipulate. Everything beyond it is open. Its contents are decided in K1 (#18).

## Rationale

The question was how to reify everything sustainably, scalably and flexibly, as the PRD asks, not whether it can be done: a graph store with everything reified can do it, but does not answer how.

- Identity through events means no one decides in advance what deserves identity. Everything that happened already has it, and nothing is stored twice to be reified. Datomic (facts carry the transaction that asserted them) and CRDTs such as Automerge (an object's id is the id of the operation that created it) work this way.
- Event streams between surfaces are how incremental dataflow systems (Differential Dataflow, DBSP, Materialize) keep derived views coherent. What makes them coherent is the frontier each one publishes, not the streams alone. This gives K1's frontiers their purpose: the set of events a surface has taken in, closed under causes.
- Surfaces with their own vocabularies keep surfaces free to work with data in their own shapes and terms, while origins store one language. Recording the surface's event as the cause keeps its meaning next to the basic changes, so "why did this change" is answerable in the surface's terms.

Costs:
- The hard problems move to two places: frontiers (coherence) and translations (meaning).
- At the PRD's scale, streams must be filtered by scope at their source.
- Reactive surfaces can form loops.
- Events are kept forever, so translations between versions of a vocabulary accumulate.

Open: [[What the basic event vocabulary contains]], [[Whether reads are events]], [[How overlapping event vocabularies are aligned]], [[How translations between vocabulary versions are kept]].

This replaces parts of ADR-0YA and ADR-0YB, whose entity and effect shapes are not event-first and which have no events of a surface's own. The documents change through Update View.