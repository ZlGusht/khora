# ADR-0YA: Entity behavior and effects

> **Status:** Proposed
> **Date:** 09.11.2026
> **Issue:** to be opened
> **Related:** ADR-0XX Query model and propagation; ADR-0XY Entity sources, time and schema; ADR-0XZ Scopes, khoras and rights (amended by this ADR)
> **Number:** to be assigned when added to the repository

## Context

Entities may be functional: they hold logic and cause effects. The sample specs already assume this in many places without naming it:

- Agreements with decision rules.
- Computable rules and checks.
- The learner model, a generative model that predicts.
- Process models and governed actions.
- Automated actions on triggers.
- Dynamic interfaces composed at request time.
- `Propagate` updates defined by a process.

The PRD's self-containment requirement ("any logic outside the firmware is modeled data") asks for the same thing.

The existing ADRs assume a kernel that only reads data, with changes arriving as entries in the action log. Letting entities execute effects inside the kernel would cost an actor runtime, sandboxing, exactly-once execution across deployments and cascade control, none of which the MVP needs.

This ADR therefore separates two things:
- **Logic as data**, which is adopted now.
- **Entities that execute their own effects**, which is staged for later.

The model is fixed now so the later stage does not change it.

## Terms

| Term | Meaning |
| --- | --- |
| **Behavior** | Logic bound to a kind or an entity. An entity itself. |
| **Pure behavior** | A behavior that writes nothing. |
| **Operation** | A behavior with effects, executed by a principal. |
| **Controller** | A principal that watches changes and invokes operations under a standing grant. |
| **Invocation** | The action of running an operation: who, on whose behalf, which version, with what arguments. |
| **Effect** | A change caused by an invocation, linked to it. |
| **Effect class** | Pure, internal (changes khora entities) or external (acts outside khora). |

## Decisions

### Behavior as data

**1. Behavior is a facet, not a kind.** Any entity can have behaviors, whether it is internal, external, computed or an aggregator. Where an entity's data comes from and what it can do are separate questions.

**2. A behavior is an entity.**
- It is bound to a kind (applies to all instances) or to a single entity.
- It is identified by the hash of its content and versioned by log position.
- A read at frontier *f* uses the behaviors in force at *f*, as with the schema (ADR-0XY decision 10).

**3. Every behavior declares its contract.** The declaration covers:
- trigger: call, event or schedule;
- reads and writes, as scopes;
- effect class;
- reversibility;
- determinism.

A behavior without a declaration is opaque: its reads and writes are unknown, so impact on it is "unknown", never "unaffected". Declared writes are enforced; a run that writes outside them is stopped and rolled back.

### Three forms

**4. Pure behaviors run in the kernel.** They cover:
- computed attributes;
- rule checks and invariants;
- kind-specific collapsed representations (ADR-0XZ decision 9);
- `Propagate` updates.

A computed entity is the output of a pure behavior.

**5. Operations are executed by principals.** An operation is declared on a kind and run through the action layer as a governed action. The principal running it can be a person, an agent, a service or a controller. In the MVP, entities do not execute effects themselves.

**6. Reactivity and autonomy come from controllers.**
- A controller is a principal entity. It subscribes to change feeds and invokes operations under a standing grant.
- An entity that acts on its own is, in the model, an entity with a controller.
- Sync of external entities is a controller.
- So is kernel housekeeping wherever possible: materialized lenses and access labels, revocation cascades, orphan detection and decay.

### Execution

**7. Queries call only pure behaviors.** This amends ADR-0XX: a `Propagate` update given as a process must be pure. A non-deterministic pure behavior has its output recorded in the log and replayed; where that isn't possible, the result is marked non-reproducible.

**8. Sources get a write side.** This amends ADR-0XY decision 2. `EntitySource` gains:

```
invoke?(operation, args, ctx): Stream<Effect>, ResultMeta   // capability
```

Sources claim which operations they can execute, the same way they claim kinds and attributes. The planner dispatches invocations by these claims.

**9. An action is an invocation plus the effects it caused.**
- The invocation records the actor, on whose behalf it acted, the grant used, the operation's hash and the arguments.
- Each effect is recorded as a change linked to the invocation that caused it.
- Replay reads the recorded effects; deterministic operations may be re-run to verify them.
- External effects are recorded but cannot be replayed or undone.

This partly settles ADR-0XY's open question on the shape of the action log.

**10. Effect class drives governance.**
- Approval is required for irreversible or external operations, and for operations above an impact threshold.
- Before confirmation, the impact is previewed from the declared writes and the blast radius.
- Reversible operations can be undone from the log.

### Authority

**11. One new right: invoke.** Since behaviors are entities, reading or changing logic is already covered by read and manage on the behavior. Invoke allows calling an operation without reading its logic. This amends ADR-0XZ decision 24.

**12. Authority depends on the form.**
- **Pure behaviors** run with the reader's rights.
- **Operations** run with the intersection of the invoker's rights and the operation's grants (ADR-0XZ decision 35), always. An operation never uses its own authority on the invoker's behalf.
- **Controllers** act on behalf of whoever granted their standing grant.

**13. Controllers stop when their authority ends.**
- A controller stops when its grant chain breaks or its grant is revoked.
- In-flight invocations complete if they are reversible and are aborted otherwise.
- Each controller has one execution home at a time, held as a lease, so its effects never fire twice.
- An orphan has no controller with a live grant, so it cannot act (ADR-0XZ decision 42).

**14. Effects carry the restrictions of what they read.** Data written by an effect can be read only by principals who may read the effect's declared inputs. This extends ADR-0XZ decision 37 from derived results to writes, so restricted data cannot be laundered through an effect.

### Reactivity

**15. Every reaction records its cause.** A controller's invocation records the change that triggered it and the frontier it saw. Reactions to logs that don't know of each other are concurrent and have no defined order.

**16. Cascades have budgets.**
- Every invocation carries a budget (depth, count and cost) inherited from its cause.
- Cycles are detected through cause links.
- A chain that exhausts its budget stops and is flagged.

### Staging

**17. Execution moves inward in stages; the model stays the same.**
1. **MVP:**
   - pure behaviors in the kernel;
   - operations executed by principals;
   - controllers.
2. **Native:**
   - declarative operations executed by khora's own runtime, queries first, then writes;
   - controllers for those operations become an implementation detail.
3. **Self-hosting:** the runtime is itself modeled data, produced and checked by its previous version.

Decisions 2, 3, 9 and 11–16 hold across all stages.

## Caveats

- **Declarations are trusted.** Writes are enforced, but reads cannot be enforced without sandboxing. A behavior that reads more than it declares breaks impact analysis and the restrictions in decision 14.
- **Opaque behavior degrades analysis.** The more logic is opaque, the more often impact, access and replay come back "unknown".
- **Taint is coarse.** Restrictions from declared reads cover everything a behavior could read, not what it actually used, so they over-restrict.
- **Controllers are indirection.** Explaining "why did this happen" depends on tooling over cause links.
- **Pure code still needs limits.** A pure behavior can loop forever or exhaust resources, so it runs under the query's deadline and budget.
- **Logic migrates with kinds.** Changing a kind's behaviors affects every instance, and existing instances may depend on the old version.
- **Leases across deployments** depend on interoperability. **(interop: review)**

## Consequences

**Positive**

- Logic is versioned, logged, rights-governed and replayable, like data.
- Kinds carry operations:
  - domain systems install as content;
  - agents discover their tools from the operations granted in a scope;
  - interfaces generate forms and confirmations from signatures.
- Impact is known from declared writes. Approvals, previews and undo follow from effect classes.
- Data stays live through pure behaviors and controllers, without effects inside the kernel.
- Rules, agreements, process models, computed entities, sync and automation share one construct.
- The kernel stays a data-and-log system in the MVP, and later stages extend it without changing the model.

**Negative**

- Every behavior needs a declaration. Undeclared logic is treated as opaque.
- Controllers add a principal and a lease per active entity.
- The action log records invocations as well as effects, so it grows faster.
- Native execution, and with it full self-containment, waits for a later stage.

## Alternatives considered

- **Passive data with all logic in external executors:** the simplest kernel, but logic is invisible to khora, impact is never exact, and self-containment never happens.
- **Pure behaviors only:** live and analyzable, but nothing can be done inside khora; every action needs separate glue code.
- **Full functional entities now:** entities execute their own effects in the kernel. This needs an actor runtime, sandboxing, exactly-once execution across deployments and cascade control before the MVP, and makes the intractability risk in the PRD worse. Kept as a later stage, without changing the model.
- **Database-style triggers on entities:** hidden reactive logic with no principal, no declared authority and no execution home. Replaced by controllers.

## Open questions

| Question | Lean |
| --- | --- |
| Language for behaviors | Declarative core for queries, transformations and flows; opaque steps in a sandboxed substrate (e.g. WASM) |
| Behaviors on single entities, or only on kinds | Both; per-entity behaviors override or add |
| Kinds from several upper ontologies binding operations | Through the resolution layer (ADR-0XY) |
| Impact threshold for approval | Per scope, set by its holders |
| Versioning a kind's behaviors while instances depend on the old version | Pin per instance until migrated; flag drift |
| Long-running operations | Durable execution on the action log; part of the process engine |
| Controller leases across deployments | Open **(interop: review)** |

## Out of scope (TODO)

- The process engine: long-running workflows, and the native runtime of stage 2.
- Sandboxing technology.
- The interoperability protocol, including controller leases across deployments.

## Done when

- This ADR is accepted.
- ADR-0XX is amended for pure updates (decision 7).
- ADR-0XY is amended for `invoke` and the shape of actions (decisions 8–9).
- ADR-0XZ is amended for the invoke right, authority and effect restrictions (decisions 11–14).
- Contract tests cover:
  - pure behaviors cannot write;
  - writes outside the declaration are stopped and rolled back;
  - operations run with the intersection of rights;
  - revocation stops controllers;
  - cascade budgets stop chains;
  - replay from recorded effects;
  - effect restrictions.
