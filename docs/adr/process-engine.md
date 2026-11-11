# ADR-0YC: Process engine

> **Status:** Draft (for review; not yet discussed)
> **Date:** 10.11.2026
> **Issue:** #2 Make process execution core capability
> **Related:** ADR-0YA Entity behavior and effects; ADR-0YB Action log, processes and reactivity; ADR-0XX Query model and propagation; ADR-0XY Entity sources, time and schema; ADR-0XZ Scopes, khoras and rights; #7 Process model; #9 Narrow the kernel
> **Number:** to be assigned when added to the repository

## Context

Every ADR so far defers the process engine:
- ADR-0YA leaves long-running operations to "durable execution on the action log; part of the process engine".
- ADR-0YB fixes the engine's contract with the log (decisions 6–9) and leaves "the process engine itself" and "process definition language and step contract" open.
- ADR-0XX defers running `Propagate` as a long-lived process, and who recomputes materialized results.
- ADR-0XZ counts a process among principals and runs it under least privilege (decision 35).

Processes are the core primitive next to entities (meeting, 06.11). The starter pack describes them as data (`Process(n) v1.0.0`, `Process Instance v1.0.0`), but only an agent following `Execute Process v1.0.0` can run one. A process with nothing intelligent in it still needs an LLM, and two runs of it can differ.

This ADR defines what a process is in the ADR-0YA model and how the engine advances it. How a run is stored, resumed, pinned and compensated is ADR-0YB's; this ADR uses it as is.

## Terms

| Term | Meaning |
| --- | --- |
| **Process** | A composite operation: a behavior (ADR-0YA) whose body is a set of steps. An entity. |
| **Step** | One unit of a process: a pure behavior, an operation, a subprocess, an agent task or a wait. |
| **Process instance** | A running process (ADR-0YB). A principal while it runs. |
| **Engine** | The kernel component that advances instances: picks the next step, dispatches it, records the outcome. |
| **Agent runner** | A stateless service that performs agent steps with an LLM and calls khora back as an agent principal. |
| **Binding** | How a step's input is taken from the process's inputs or from earlier steps' outputs. |

## Decisions

### What a process is

**1. A process is a composite operation.**
- It is a behavior (ADR-0YA decision 2): an entity, bound to a kind or to one entity, identified by content hash.
- It declares a contract like any behavior (ADR-0YA decision 3). Its reads and writes cover those of its steps; its effect class is the strongest of its steps'.
- A process whose steps are all pure is a pure behavior, so it may be used inside queries (ADR-0YA decision 7), including as a `Propagate` update.

**2. A step is one of five kinds.** No new forms; each maps onto ADR-0YA and ADR-0YB:

| Kind | Runs | Performed by |
| --- | --- | --- |
| `pure` | A pure behavior or a `Query` (ADR-0XX decision 2) | The kernel |
| `operation` | An operation, dispatched to the source or runtime that claims it (ADR-0YA decision 8) | The instance, through the action layer |
| `process` | Another process, as a child instance | The engine |
| `agent` | A task that needs judgment, written in prose | An agent principal through an agent runner |
| `wait` | A timer, an approval, a human step or a sync (ADR-0YB decision 8) | Nobody; a matching action resumes it |

A step written as plain prose, with no kind, is an `agent` step, so the starter pack's processes keep running.

**3. Steps pass data by binding.** Each step names its inputs and outputs. An input binds to a process input or to an earlier step's output. Large outputs are stored once and referenced by hash (ADR-0YB decision 5). Order follows the bindings; steps with no dependency between them may run in parallel.

### How the engine runs it

**4. The engine is a kernel component; the instance is the principal.**
- The engine sits beside the controller host and the action layer, and keeps no state of its own: an instance's state is the fold of its actions (ADR-0YB decision 6).
- The instance acts with the intersection of its invoker's rights and the process's grants (ADR-0XZ decision 35; ADR-0YA decision 12). Starting it needs `invoke` on the process (ADR-0YA decision 11).
- Every `operation` step goes through the action layer: rights, approval, declared writes and budget are checked there, not in the engine.

**5. One loop advances every instance.** For an instance with work to do, the engine:
1. folds the instance's actions to find the steps whose inputs are ready;
2. appends each step's start with its inputs (ADR-0YB decision 6);
3. dispatches the step by its kind (decision 2);
4. appends the completion with outputs and effects, or the failure;
5. on a `wait`, appends the wait and stops; the matching action resumes the instance.

Crash and restart are the same case: the next pass of the loop resumes from the log.

**6. Agent steps call out; agents call back.**
- The engine sends the agent runner the step, its bound inputs, the context assembled from the process's recipe, the operations the step may invoke, and a budget.
- The runner calls khora only through MCP, as an agent principal acting for the instance, limited to the step's declared scopes. It holds no state and reaches no store.
- The step is nondeterministic, so its output is recorded and replayed, never re-run (ADR-0YB decision 7).

**7. Budgets are split, not shared.** An instance inherits its budget from its cause (ADR-0YA decision 16) and gives each step a share. A step that exhausts its share fails; the instance then fails or compensates (ADR-0YB decision 9).

### What it is for

**8. Primitives stay kernel operations; every computation can still be a process.** `search`, `query`, `read` and `create` are implemented by the kernel, not interpreted from process entities, so recursion bottoms out. Each is reachable as a one-step process through the same run interface, with the same log entries. Queries are already data (ADR-0XX decision 2); running them natively is ADR-0YA stage 2.

**9. Long-lived `Propagate` runs as a process.** A simulation or an iterative computation that outlives a query is a process whose step evaluates `Propagate` for a bounded number of iterations, checkpoints its state as an output, and repeats until the stop condition. It gets durability and budgets from the engine.

**10. Recomputing materialized results is a controller starting a process.** A materialized result (ADR-0XX) declares its inputs. A controller subscribed to them (ADR-0YB decision 10) starts the recompute process when they change. The result reports the frontier it was computed at.

**11. One run interface, three doors.**

| Operation | CLI | REST | MCP |
| --- | --- | --- | --- |
| Start | `khora run <process> --in k=v` | `POST /api/v1/processes/{id}/runs` | `run_process` |
| Status, steps, outputs | `khora run show <run>` | `GET /api/v1/runs/{run}` | `get_run` |
| Approve a waiting step | `khora run approve <run> <step>` | `POST /api/v1/runs/{run}/steps/{step}/approve` | `approve_step` |
| Cancel | `khora run cancel <run>` | `POST /api/v1/runs/{run}/cancel` | `cancel_run` |

Cancelling appends an action; running steps finish or are aborted by their reversibility (ADR-0YA decision 13).

### Example

A quote in the furniture workshop system. Kroika, a separate design app, prices projects; khora calls it through an operation its connector claims, and does not copy its code.

```
process QuoteFromHistory                         // composite operation
  in:     project
  reads:  project, past projects in scope
  writes: Quote                                  internal  reversible
  steps:
    similar = pure       SimilarProjects(project, limit 10)
    totals  = operation  KroikaPricing.price(project)        external, read-only, deterministic
    note    = agent      "Explain the price against the similar projects"
                         in: similar, totals   may invoke: search, read
    quote   = operation  Quote.create(project, totals, note)
  out:    quote
```

`similar` and `totals` have no dependency and run in parallel. If the Kroika connector is unreachable, `totals` fails and nothing is written. If the kernel restarts after `note`, the loop resumes at `quote` with the recorded note.

## Caveats

- **The engine depends on declarations.** A step declared deterministic that is not diverges on replay (ADR-0YB caveats).
- **Agent steps are the weak point.** Their output is recorded, not reproducible; their reads are trusted, not enforced (ADR-0YA caveats).
- **Parallel steps need disjoint writes.** Two parallel steps that declare overlapping writes run one after the other.
- **Prose processes stay opaque.** A process made only of `agent` steps runs, but its impact and access are "unknown" until its steps are declared.

## Consequences

**Positive**
- A process with no `agent` steps runs without an LLM and gives the same result every time.
- One construct covers workflows, simulations, recomputation and the starter pack's processes.
- Processes get rights, approvals, budgets, durability and replay from the existing ADRs, with no mechanism of their own.

**Negative**
- Every step must declare its contract; prose steps lose analysis until they do.
- The agent runner is a second service to run and secure.
- The engine is on the path of every long-running write.

## Alternatives considered

- **Executor outside the kernel, in Python:** close to the LLM SDKs, but it would need store access or a call per step, and the kernel could not run its own processes.
- **No engine; agents follow `Execute Process`:** works today, but needs an LLM for every process and is not reproducible.
- **Processes as controllers only:** fits reactions, not "run this now with these inputs". Controllers start processes instead (decision 10).
- **A workflow product (Temporal and similar):** rejected by ADR-0YB for the same reason: a second log beside khora's.

## Open questions

| Question | Lean |
| --- | --- |
| Is a process a composite operation (decision 1)? | Yes |
| "Every computation is a process": is decision 8 what was meant, or should `search` and `query` themselves be process entities? | Decision 8; native processes in ADR-0YA stage 2 |
| Does the engine count as part of the kernel that #9 narrows? | Yes, as a component beside the controller host (as on the architecture canvas) |
| How much of decision 5 is in the MVP? | `pure`, `operation`, `process`, `wait`; `agent` steps and parallel steps after |
| Step syntax, inputs and outputs, unstructured tasks | With #7 |

## Out of scope (TODO)

- The process definition format and its syntax (#7).
- Sandboxing for opaque steps (ADR-0YA).
- Running processes across deployments **(interop: review)**.

## Done when (issue #2)

- This ADR is accepted.
- The plan's sections 04, 08 and 12 move the executor from `khora-ai` to the kernel, and `khora-ai` keeps an agent runner.
- Contract tests cover:
  - a process with only pure steps runs inside a query;
  - an `operation` step is checked by the action layer like any other invocation;
  - an instance resumes after a restart without repeating a step;
  - parallel steps with overlapping declared writes run in sequence;
  - an `agent` step's output is replayed, not re-run;
  - a `wait` step holds no resources until the matching action arrives.
