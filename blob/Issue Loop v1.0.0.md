---
identity__uuid: 4d286dbe-a681-41e3-b21b-feea2a72fffc
identity__type: universal
identity__created_at: 2026-11-18
identity__tags: []
aliases:
  - Issue Loop
relationships__is_a:
  - "[[Loop v2.0.0]]"
relationships__in_context_of:
  - "[[Kit]]"
properties__author: agent:claude-opus-5-5
---
The **Issue Loop** plans and carries out one GitHub issue. It is the lowest level of development, below a [[Milestone Loop v1.0.0]] run. A superpowers run is an Issue Loop run.

A run is named `Issue #<number> <short title>`, is_a both [[Process Instance v1.0.0]] and [[Issue Loop v1.0.0]], and is in_process the Milestone run of its issue's milestone. Anyone may open one.

```toml
Description = """
One issue as a loop: planning and execution together. The issue lives in GitHub; the run is based on it and takes it in at Laydown. A run usually ends in one pull request. A large issue spawns child Issue runs, in_process this one, for its sub-issues.
The run's body holds its Loop Config, Vector, plan and what was verified. Friction, findings and ideas go into Letters in_context_of the run, as they happen; they stay after the run closes. Decisions agreed with a team member during the run are Decision entities that originate_in it.
At its close the run writes one Digest to its parent run. `khora-sync` keeps the run and GitHub in step when the run opens, when it closes, and when its pull request merges.
"""
Subprocesses = """
### Inception
1. Open the run: status open, started_at, `issue`, in_process the Milestone run, in_context_of the Areas the issue touches.
2. Write the Loop Config into the body: the domain (Clear, Complicated or Complex), the Scope of Authority (the paths this run may change), and the Reservoir capacity.
3. Create the branch `issue/<number>-<short-title>`, then run `bash .claude/scripts/khora-sync open "<run name>"`. It comments on the issue with the run and the branch.

### Laydown
Superpowers brainstorming runs here.
1. Take in the issue: read it and its comments.
2. Write the Vector: the Attractor (what the issue asks for) and the Repulsor (what the run must not touch or become).
3. Agree design choices with a team member. Each choice is a Decision that originates_in this run. A spec in docs/ is written from those Decisions through Update View, with trace markers.
4. Commit to a first move, and set the budget and the triggers that close the run early.

### Operate
Superpowers planning and plan execution run here.
1. Write the plan into the body, starting from the first move. Work through it on the branch, within the Scope of Authority; tests first for code.
2. Drop a Letter in_context_of the run for each friction, finding or idea worth keeping. Do not stop to write long notes.
3. A single hard impasse is a child run with a narrower scope. A change outside the Scope of Authority means stop and ask.

### Observe
Superpowers verification and review run here.
1. Verify: tests pass; khora-validate passes on every entity written; `khora-stale` shows nothing new caused by this run; every deciding sentence added to docs/ carries a trace marker.
2. Review the diff and record the findings.
3. Write into the body what was done, what was not, and what was checked.

### Process(v)
The superpowers retrospective runs here.
1. Read the run's Letters. Resolve the ones the run dealt with; carry the rest into the Digest.
2. Write the Digest: Outcome, Findings, Frictions, Ideas, Considerations. It is in_context_of the parent run and digest_of this run.
3. Set status closed and ended_at, and validate the run.
4. Open the pull request. Its description says `Closes #<number>`, names the run, and holds the Digest and any proposed issues.
5. Run `bash .claude/scripts/khora-sync close "<run name>"`; it records the pull request. After the merge, run `bash .claude/scripts/khora-sync merged "<run name>"`; it closes the issue if the merge did not.
"""
ContextAssembly = """
1. The issue, with its comments: `gh issue view <number> --comments`.
2. The parent Milestone run, its goal and gate.
3. Active Decisions and open Questions in_context_of the run's Areas, and the documents whose `areas` name them.
4. Earlier Issue runs in the same Areas, most recent first: their Digests.
"""

[Metadata.Properties]
issue = { type = "number", description = "The number of the GitHub issue this run is based on." }
pull_requests = { type = "list", description = "Numbers of the pull requests that carry this run's work. Usually one. Set by khora-sync.", optional = true }
```