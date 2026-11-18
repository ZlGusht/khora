---
identity__uuid: ccec53ed-327a-4e48-88f9-5fe3340e87ca
identity__type: universal
identity__created_at: 2026-11-18
identity__tags: []
aliases:
  - Milestone Loop
relationships__is_a:
  - "[[Loop v2.0.0]]"
relationships__in_context_of:
  - "[[Kit]]"
properties__author: agent:claude-opus-5-5
---
The **Milestone Loop** is one milestone of a [[Roadmap Loop v1.0.0]] run: a goal of weeks, with a gate. Its work is GitHub issues, each run as an [[Issue Loop v1.0.0]] run.

A run is named `Milestone <title>`, with the same title as its GitHub Milestone, is_a both [[Process Instance v1.0.0]] and [[Milestone Loop v1.0.0]], and is in_process its roadmap run. Only team members open one.

```toml
Description = """
A milestone as a loop. The run's body holds its goal, its gate, its Vector, and the issues it takes in. Issues live only in GitHub; designer work is issues too. The run is reflected by a GitHub Milestone, kept in step by `khora-sync` when the run opens and when it closes.
Issue runs report up through their Digests, in_context_of this run. When the milestone closes, it writes its own Digest to its roadmap run.
"""
Subprocesses = """
### Inception
1. A team member opens the run: status open, started_at, in_process the roadmap run, in_context_of the roadmap's Areas.
2. Write the goal and the gate into the body, as the roadmap's Laydown states them.
3. Run `bash .claude/scripts/khora-sync open "<run name>"`. It links the GitHub Milestone with the same title, or creates it, and sets `milestone`.

### Laydown
1. Write the Vector: the Attractor (the gate) and the Repulsor (what this milestone must not take on).
2. List the issues the milestone takes in, by number. Work that has no issue gets one in GitHub, created by a team member; it is never written into the blob.
3. Set the budget, if any.

### Operate
Issue runs do the work. Drop a Letter in_context_of this run for friction that spans issues.

### Observe
1. Read every unresolved Digest in_context_of this run.
2. Check the gate.
3. Run `bash .claude/scripts/khora-sync check "<run name>"` and note what differs between the run and GitHub.

### Process(v)
1. For each Digest read: act on it (adjust the issues taken in, propose issues or Decisions), then set it resolved, or carry it into this run's Digest.
2. When the gate passes, or the milestone is given up: write this run's Digest, in_context_of the roadmap run, set status closed and ended_at.
3. Run `bash .claude/scripts/khora-sync close "<run name>"`. It closes the GitHub Milestone.
"""
ContextAssembly = """
1. The roadmap run (in_process) and this milestone's line in it.
2. The Issue runs in_process this run, and every Digest in_context_of this run, unresolved first.
3. The GitHub Milestone's issues: `gh issue list --milestone "<title>" --state all`.
"""

[Metadata.Properties]
milestone = { type = "number", description = "The number of the GitHub Milestone that reflects this run. Set by khora-sync when the run opens.", optional = true }
```