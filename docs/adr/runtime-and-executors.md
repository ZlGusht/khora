# ADR-0YC: Runtime and executors

> **Status:** Proposed
> **Date:** 12.11.2026
> **Issue:** #9 Narrow the kernel
> **Related:** ADR-0YA (executors); ADR-0XX (declarative logic); ADR-0YD (behaviors and processes)
> **Number:** to be assigned when added to the repository

## Context

All logic runs in one runtime, and executors bring initiative (ADR-0YA decisions 7–8). This ADR fixes how the runtime runs logic and how executors, including hubs, behave.

## Terms

| Term | Meaning |
| --- | --- |
| **Runtime** | Where all logic runs. |
| **Trigger** | What starts an executor: a call, a change, a time, or the executor itself. |
| **Standing executor** | An executor defined as data that acts until stopped: a reaction, schedule or resident agent. |
| **Budget** | Limits on depth, count and cost, inherited along causes. |
| **Hub** | A live shared instance of a surface. |

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
- It runs in one place at a time, held as a lease.
- Triggers are delivered at least once and keyed by trigger and time, so handling one twice changes nothing.

**8. Every chain of causes carries a budget.** A chain that exhausts its budget stops and is flagged.

**9. Agents dispatched by the runtime run through an agent runner.** The runner holds no state. It calls khora back only through surfaces, as an agent acting for whoever dispatched it, limited to the scopes it was given.

### Hubs

**10. A hub orders its participants' edits.**
- It shows presence and delivers to each participant only what their rights allow.
- It stays on the surface definition it opened with.
- Its state can be rebuilt from origins.

## Costs and risks

- The sandbox is on the hot path. Each call between sandboxed layers adds overhead; inlining declarative logic keeps most of it out.
- One runtime is one blast radius: a sandbox escape exposes everything.
- At-least-once delivery makes every handler responsible for idempotence.
- Hubs on popular surfaces become hotspots and may need to be split.

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

Declarative logic only, without the sandbox. Reactions and schedules written in code. Hubs as simple shared rooms. Agents through the API; the agent runner comes later.

## Done when (issue #9)

Tests cover:
- deterministic logic cannot reach the clock or network;
- code cannot read or write outside its declaration;
- revoking a grant stops its standing executor;
- a trigger delivered twice changes nothing;
- budgets stop chains;
- slow executor work does not delay label upkeep;
- a hub delivers to each participant only what they may read.
