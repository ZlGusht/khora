# ADR-0YA: Origins, behaviors, surfaces and executors

> **Status:** Proposed
> **Date:** 13.11.2026
> **Issue:** —
> **Related:** every other ADR builds on this one
> **Number:** to be assigned when added to the repository

## Context

The earlier ADRs grew a read path, a write path, a log, a behavior runtime, controllers and a process engine. Each covered the gap the previous one left, and they overlapped. This ADR replaces them with one model that the other ADRs build on.

A first draft of this ADR made the action the unit of the log, with its effects inside it. That fused a request with its result, and forced an actor onto things nobody requested, such as observations and timers. The unit is now the event.

## Terms

| Term | Meaning |
| --- | --- |
| **Event** | One thing that happened, recorded in a log with its causes. |
| **Action** | An event in which an executor requests a read or a write. |
| **Outcome** | The event that accepts or rejects an action. |
| **Result** | What an accepted action brings about: its effects and, for a read, what is returned. |
| **Effect** | An event that changes one entity. |
| **Observation** | An event recording what khora saw in an external system. |
| **Entity** | An identity that effects refer to. |
| **Kind** | What an entity is. Defaults to `entity`. |
| **Origin** | An authoritative source of effects: the khora log or an external system. |
| **Behavior** | Logic bound to a kind or to one entity. |
| **Surface** | A shape over origins and other surfaces, with its own logic, for reading and writing. |
| **Executor** | Anything with initiative: a person, agent, program, reaction or schedule. |

## Decisions

### Events

**1. Everything that happens is recorded as an event.** A log holds only events, linked by cause. An event is never changed.

**2. An action is an event: a request by an executor.**
- A read is an action, like a write.
- One call by an executor is one action.

**3. A request, its acceptance and its result are separate events.**
- Every action gets an outcome: accepted or rejected.
- The effects of an accepted action are events caused by that outcome.
- An accepted action may have no effects. A read has none.

**4. State changes only through effects.**
- An entity is whatever its effects say. It has no storage of its own.
- Every effect has exactly one cause: the outcome of an accepted action, or an observation.

**5. Only actions need authority.**
- An outcome or an effect is valid by its cause.
- An observation carries its source, not authority.
- A reaction or schedule acts by issuing actions under its grant (ADR-0YC decision 7).

### Entities and logic

**6. An entity may declare a kind; otherwise its kind is `entity`.**
- Every kind is a kind of `entity`.
- A kind is declared by the creation effect or by any later effect. A read at a past frontier sees the kind in force then.
- Surfaces may classify an entity further, but never replace its declared kind.

**7. Everything khora owns lives in its log.** Every other origin is an external system, authoritative for its own effects. khora records only what it observed there and what it did.

**8. Behaviors are the logic of entities.** They are defined as data and specified in ADR-0YD.

**9. Rules about data are behaviors, never surface logic.** A rule then holds everywhere the entity appears.

**10. Surfaces are the only way to reach data.**
- A surface reads origins and other surfaces, applies the behaviors of the entities it shows, and translates the writes it accepts into effects.
- A surface with nothing beneath it is an origin.

**11. Surfaces have no initiative; executors do.** Derived and invoked behaviors run inside surfaces. Reactive and autonomous behaviors run as executors.

**12. Executors keep no state.** Whatever must survive is written through a surface.

**13. Anything a write depends on is reproducible or recorded.** Nondeterministic logic is allowed, but its output is recorded before a write uses it.

**14. Recording an event is not itself an event.** *Suggestion*
- Appending to the log is what the kernel does. It is not requested through a surface.
- Reads the runtime makes to keep surfaces current belong to the event that caused them. They are not new actions.

## Costs and risks

- Events outnumber requests several times: every action brings an outcome, and reads are actions. Volume and retention need a policy (ADR-0YB).
- "Who did this" is answered by walking causes back to an action. That walk needs an index.
- Some declarations can't be checked by the runtime: algebra properties, reversibility, and the guarantees of external systems. Those are trusted (ADR-0YC decision 2 lists what is enforced).
- Events are kept forever, so the vocabulary needs versioning and upcasting (ADR-0XY).
- Everything khora owns passes through one log, so the log's throughput bounds sustained writes (ADR-0YB).

## Alternatives considered

- **Actions as the unit of the log, with effects inside:** fuses a request with its result; observations and timers need a made-up actor.
- **Authority on every event:** effects and observations are not requests.
- **Datastores as the source of truth:** two truths to keep consistent.
- **Datastores as origins for high-volume data:** data khora owns would live outside its log.
- **Logic only in surfaces:** rules drift between surfaces.
- **Separate read and write paths:** authority and provenance are implemented twice.

## Open questions

| Question | Lean |
| --- | --- |
| Do events carry domain kinds, such as "meal eaten", beyond the four kinds here? | The action's operation carries the meaning; open |
| How definitions (vocabulary, behaviors, surfaces, standing executors, origins) are found and booted from the log | Later, with self-containment; the kernel ships as code first (ADR-0YC decision 11) |

## MVP

All of it: this ADR defines the model, not features.
