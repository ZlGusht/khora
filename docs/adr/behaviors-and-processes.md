# ADR-0YD: Behaviors and processes

> **Status:** Proposed
> **Date:** 12.11.2026
> **Issue:** #2 Make process execution core capability; #7 Process model
> **Related:** ADR-0YA (behaviors are the logic of entities); ADR-0XX (operations); ADR-0YC (the runtime that runs them)
> **Number:** to be assigned when added to the repository

## Context

Entities can have their own logic and cause effects (ADR-0YA decision 4). Processes are that logic composed over time. This ADR fixes how behaviors are bound, declared, enforced and versioned, and how processes compose them.

## Terms

| Term | Meaning |
| --- | --- |
| **Rule** | A behavior that constrains what an entity may be. |
| **Process** | A behavior composed of actions and other processes. |
| **Instance** | A running process: an entity whose state is built from its actions. |

## Decisions

### Behaviors

**1. A behavior is bound to a kind or to one entity.** Kind behaviors apply to all instances of the kind. Behaviors bound to `entity` apply to everything. Entity-bound behaviors add to or override their kind's.

**2. Behaviors come in four levels:**
- derived: computed from the entity, such as remaining budget;
- invoked: an operation others call, such as `building.check(rules)`;
- reactive: runs when something changes;
- autonomous: acts on its own initiative.

Derived and invoked behaviors run inside surfaces; reactive and autonomous ones run as executors (ADR-0YA decision 7).

**3. Every behavior declares** its trigger, what it reads and writes (as scopes), its effect class (pure, internal, external), its reversibility and its determinism. An undeclared behavior is opaque: impact on it is "unknown", never "unaffected".

**4. Rules are checked at commit, whichever surface a write came through.**

**5. A new version of a behavior applies from its time on.** Entities that violate a new rule are flagged, not rewritten. Reads at an earlier frontier use the version in force then.

### Processes

**6. A process is a behavior composed of actions and other processes,** in sequence, in parallel, by choice, or waiting for an action. Parts with overlapping writes do not run in parallel.

**7. A process with no effects and no nondeterminism is pure** and can be used in surface logic, including as a `Propagate` update.

**8. A process instance is an entity** whose state is built from its actions in the log.
- It stays on the version it started with until it is migrated explicitly.
- A wait holds no resources; the matching action resumes the instance.

**9. An instance acts for its starter and stops when the starter's authority ends.** Reversible work in progress completes; irreversible work is aborted. Each part gets a share of the instance's budget.

**10. Replay and compensation.**
- Deterministic parts may re-run.
- Nondeterministic parts (agents, external calls, clocks) return their recorded output.
- Failures across origins are compensated. The default compensation comes from the invertible effects.

### Example

```
process QuoteFromHistory(project)
  similar = SimilarProjects(project, 10)                        pure
  totals  = KroikaPricing.price(project)                        external, read-only, deterministic
  note    = agent "Explain the price against the similar projects" (similar, totals)
  quote   = Quote.create(project, totals, note)                 internal, reversible
```

`similar` and `totals` run in parallel. If the instance restarts after `note`, it resumes at `quote` with the recorded note.

## Costs and risks

- Opaque behaviors degrade analysis: impact, access and replay come back "unknown" more often.
- Changing a kind's behavior affects every instance at once.
- Agent parts are recorded, not reproducible.
- Processes made only of agent tasks run, but their impact stays unknown until their parts are declared.

## Alternatives considered

- **Processes as surfaces:** any entity can already be shown, approved and watched through a surface; a process needs nothing extra.
- **Processes only as lists of steps:** composition of actions and processes covers steps and more.
- **Agents following prose process descriptions:** needs an LLM for every process, and runs are not reproducible.

## Open questions

| Question | Lean |
| --- | --- |
| Behavior language | Declarative core (ADR-0XX); opaque parts in the sandbox |
| Process definition syntax | With #7 |
| Kinds from several upper ontologies binding behaviors | Through a resolution layer |

## MVP

Derived and invoked behaviors, rules, and processes with internal and external actions, child processes and waits. Agent parts and parallel composition come later.

## Done when (issues #2, #7)

Tests cover:
- a rule holds whichever surface a write comes through;
- a new rule version flags violating entities without rewriting them;
- a pure process runs inside surface logic;
- an instance resumes after a restart without repeating work;
- parts with overlapping writes do not run in parallel;
- an agent part is replayed, not re-run;
- a wait holds no resources;
- an instance stops when its starter's authority ends.
