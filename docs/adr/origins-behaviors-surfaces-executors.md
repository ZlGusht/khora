# ADR-0YA: Origins, behaviors, surfaces and executors

> **Status:** Proposed
> **Date:** 12.11.2026
> **Issue:** —
> **Related:** every other ADR builds on this one
> **Number:** to be assigned when added to the repository

## Context

The earlier ADRs grew a read path, a write path, a log, a behavior runtime, controllers and a process engine. Each covered the gap the previous one left, and they overlapped. This ADR replaces them with one model that the other ADRs build on.

## Terms

| Term | Meaning |
| --- | --- |
| **Action** | One atomic write: who did it, on whose behalf, why, and the effects it caused. |
| **Effect** | One change to an entity, inside an action. |
| **Entity** | An identity that effects refer to. |
| **Kind** | What an entity is. Defaults to `entity`. |
| **Origin** | An authoritative source of effects: the khora log or an external system. |
| **Behavior** | Logic bound to a kind or to one entity. |
| **Surface** | A shape over origins and other surfaces, with its own logic, for reading and writing. |
| **Executor** | Anything with initiative: a person, agent, program, reaction or schedule. |

## Decisions

**1. State changes only through effects.** An entity is whatever its effects say. It has no storage of its own.

**2. An entity may declare a kind; otherwise its kind is `entity`.**
- Every kind is a kind of `entity`.
- A kind is declared by the creation effect or by any later effect. A read at a past frontier sees the kind in force then.
- Surfaces may classify an entity further, but never replace its declared kind.

**3. Everything khora owns lives in its log.** Every other origin is an external system, authoritative for its own effects. khora records only what it observed there and what it did.

**4. Behaviors are the logic of entities.** They are defined as data and specified in ADR-0YD.

**5. Rules about data are behaviors, never surface logic.** A rule then holds everywhere the entity appears.

**6. Surfaces are the only way to reach data.**
- A surface reads origins and other surfaces, applies the behaviors of the entities it shows, and translates the writes it accepts into effects.
- A surface with nothing beneath it is an origin.

**7. Surfaces have no initiative; executors do.** Derived and invoked behaviors run inside surfaces. Reactive and autonomous behaviors run as executors.

**8. Executors keep no state.** Whatever must survive is written through a surface.

**9. Anything a write depends on is reproducible or recorded.** Nondeterministic logic is allowed, but its output is recorded before a write uses it.

## Costs and risks

- Some declarations can't be checked by the runtime: algebra properties, reversibility, and the guarantees of external systems. Those are trusted (ADR-0YC decision 2 lists what is enforced).
- Effects are kept forever, so the vocabulary needs versioning and upcasting (ADR-0XY).
- Everything khora owns passes through one log, so the log's throughput bounds sustained writes (ADR-0YB).

## Alternatives considered

- **Datastores as the source of truth:** two truths to keep consistent.
- **Datastores as origins for high-volume data:** data khora owns would live outside its log.
- **Logic only in surfaces:** rules drift between surfaces.
- **Separate read and write paths:** authority and provenance are implemented twice.

## Open questions

| Question | Lean |
| --- | --- |
| How definitions (vocabulary, behaviors, surfaces, standing executors, origins) are found and booted: a named catalog surface, or ordinary surfaces over definition effects | Open |

## MVP

All of it: this ADR defines the model, not features.
