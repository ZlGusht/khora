# ADR-0YC: Runtime and executors

> **Status:** Proposed
> **Date:** 13.11.2026
> **Issue:** #9 Narrow the kernel
> **Related:** ADR-0YA (events and executors); ADR-0XX (declarative logic); ADR-0YD (behaviors and processes)
> **Number:** to be assigned when added to the repository

## Context

All logic runs in one runtime, and executors bring initiative (ADR-0YA decisions 11–12). This ADR fixes how the runtime runs logic, how executors, including hubs, behave, and what the kernel is.

## Terms

| Term | Meaning |
| --- | --- |
| **Runtime** | Where all logic runs. |
| **Trigger** | What starts an executor: a call, an event, a time, or the executor itself. |
| **Standing executor** | An executor defined as data that acts until stopped: a reaction, schedule or resident agent. |
| **Budget** | Limits on depth, count and cost, inherited along causes. |
| **Hub** | A live shared instance of a surface. |
| **Kernel** | The code that implements this model: the log, the surface contract, the runtime, and access enforcement. |
| **Base system** | A system that ships with the kernel. |
| **Domain system** | A system built on top of the kernel and its base systems. |

## Decisions

### Runtime

**1. One runtime runs all logic:** surface logic, behaviors and code executors. Declarative logic is compiled and pushed into datastores (ADR-0XX, ADR-0XY decision 3). Opaque logic runs in a sandbox.

**2. Code gets only the capabilities it declares.**
- Deterministic logic gets no clock, no randomness and no network.
- Code reads and writes only through the runtime, so declared reads and writes are enforced, not trusted.

**3. Work runs in scheduling classes, in this priority:**
1. access labels and surface upkeep;
2. interactive reads and writes;
3. slow executor work, such as agents and external calls.

Slow work never delays the first two.

**4. Surfaces advance in dependency order.** A surface advances only to frontiers its inputs have reached. Nothing at a frontier is delivered before the access labels reach it.

**5. Time-driven change is lazy.** Decay and staleness are computed when read. A threshold schedules one timer when it is crossed; nothing ticks per entity.

### Executors

**6. People and outside agents connect through the API and hubs. Code executors run in the runtime.**

**7. A standing executor acts for whoever granted it and stops when that grant is revoked.**
- When it acts, it issues an action under that grant (ADR-0YA decision 5).
- It runs in one place at a time, held as a lease.
- Triggers are delivered at least once and keyed by trigger and time, so handling one twice changes nothing.

**8. Every chain of causes carries a budget.** A chain that exhausts its budget stops and is flagged.

**9. Agents dispatched by the runtime run through an agent runner.** The runner holds no state. It calls khora back only through surfaces, as an agent acting for whoever dispatched it, limited to the scopes it was given.

### Hubs

**10. A hub orders its participants' edits.**
- It shows presence and delivers to each participant only what their rights allow.
- It stays on the surface definition it opened with.
- Its state can be rebuilt from origins.

### Kernel

**11. The kernel is the code in the repository, and is distributed as code.**
- Definitions (kinds, behaviors, surface definitions) are declared in their own files, apart from engine code.
- Later the kernel becomes a firmware plus data: definitions move into the log, and the firmware boots from them (PRD P1 Self-containment).

**12. Base systems ship with the kernel; domain systems are built on top,** as an operating system ships with a shell and a file browser.
- Base systems: principals and grants, scopes, a history view of the log, and a generic entity view.
- Domain systems live in their own packages and reach data only through surfaces.

## Costs and risks

- The sandbox is on the hot path. Each call between sandboxed layers adds overhead; inlining declarative logic keeps most of it out.
- One runtime is one blast radius: a sandbox escape exposes everything.
- At-least-once delivery makes every handler responsible for idempotence.
- Hubs on popular surfaces become hotspots and may need to be split.
- While definitions are code, changing one is a release, and the log does not version them (ADR-0XY).

## Alternatives considered

- **Separate runtimes for surfaces and executors:** two security models, two traces.
- **A process engine as its own component:** processes would bypass the rules everything else follows (ADR-0YD).
- **A workflow product such as Temporal:** a second log beside khora's.

## Open questions

| Question | Lean |
| --- | --- |
| Sandbox | WASM |
| Planner and federation engine | DataFusion if Rust, Calcite if JVM |
| Detecting reactions that keep undoing each other | Flag repeated flips of a value from the same causes |

## MVP

The kernel as code (11) with the base systems (12). Declarative logic only, without the sandbox. Reactions and schedules written in code. Hubs as simple shared rooms. Agents through the API; the agent runner comes later.

## Done when (issue #9)

Tests cover:
- deterministic logic cannot reach the clock or network;
- code cannot read or write outside its declaration;
- revoking a grant stops its standing executor;
- a trigger delivered twice changes nothing;
- budgets stop chains;
- slow executor work does not delay label upkeep;
- a hub delivers to each participant only what they may read.
