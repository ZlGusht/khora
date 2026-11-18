---
identity__uuid: 9e910707-416b-4c81-8eda-c14d2fd10243
identity__type: universal
identity__created_at: 2026-11-18
identity__tags: []
aliases:
  - Roadmap Loop
relationships__is_a:
  - "[[Loop v2.0.0]]"
relationships__in_context_of:
  - "[[Kit]]"
properties__author: agent:claude-opus-5-5
---
The **Roadmap Loop** is the long-term level of development: the direction of a product over several milestones. Below it run [[Milestone Loop v1.0.0]] runs, and below those [[Issue Loop v1.0.0]] runs.

A run is named `Roadmap <topic>`, is_a both [[Process Instance v1.0.0]] and [[Roadmap Loop v1.0.0]], and is in_context_of the Areas it covers. Only team members open one. It has no parent run and no GitHub counterpart: its artifacts are in the repository.

```toml
Description = """
The roadmap as a loop. The run's body holds its horizon, its Vector and its milestones in order, each with a goal and a gate (the condition to pass before the next one starts). Milestones become Milestone runs, in_process this run, when a team member opens them.
The roadmap's views are written from this run: documents through Update View (for khora v1, the Stages section of docs/implementation-plan.md), and the Roadmap canvas.
Milestone runs report up through their Digests, in_context_of this run. When a roadmap run closes, it writes its own Digest. What that Digest resolves into, above the roadmap, is not yet defined; until it is, the Digest is in_context_of the roadmap's Areas.
"""
Subprocesses = """
### Inception
1. A team member opens the run: status open, started_at, in_context_of the Areas it covers.
2. Write the Loop Config into the body: the horizon (what the roadmap covers), the Scope of Authority (what the roadmap may change: the milestones, their order, goals and gates), and the Areas.

### Laydown
1. Write the Vector: the Attractor (where the horizon ends) and the Repulsor (what the roadmap must not become).
2. List the milestones in order, each with its goal and gate. Record what each was taken from (a document, a Decision).
3. Ask a team member to open the first Milestone run that is not open yet.

### Operate
Milestone runs do the work. The roadmap changes only when a Digest or a fact calls for it.

### Observe
1. Read every unresolved Digest in_context_of this run.
2. For each milestone, check its gate against what its run reports.
3. Run `khora-stale` and note the roadmap's views that are stale.

### Process(v)
1. For each Digest read: act on it (propose a change of milestones, order or gate to the team; propose Decisions), then set it resolved. A change of milestones is made by a team member.
2. Bring the views up to date through Update View and Render Canvas.
3. When the horizon is reached or given up: write this run's Digest, set status closed and ended_at.
"""
ContextAssembly = """
1. The Milestone runs in_process this run, and every Digest in_context_of this run, unresolved first.
2. Active Decisions and open Questions in_context_of the run's Areas.
3. The documents whose `areas` name those Areas, for the roadmap's views.
"""
```