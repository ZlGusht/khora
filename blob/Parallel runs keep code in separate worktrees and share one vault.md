---
identity__uuid: 4d3251a0-b822-4601-b519-981e4507c926
identity__type: particular
identity__created_at: 2026-11-19
identity__tags: []
relationships__is_a:
  - "[[Decision v1.0.0]]"
relationships__in_context_of:
  - "[[Kit]]"
relationships__originates_in:
  - "[[Issue #18 Event types ids and clock]]"
relationships__decided_by:
  - "[[Simeon Petkov]]"
properties__decided_on: 2026-11-19
properties__status: "active"
properties__kind: "rule"
properties__quote: "code should be separate, but ideally multiple agents would work on the same vault in parallel"
properties__author: agent:claude-opus-5-5
---
## Statement

Runs worked in parallel keep their code in separate git worktrees, one per branch. They share one vault and its one Obsidian: their entities are written there and committed on each run's own branch.

## Rationale

The Obsidian CLI talks to one Obsidian per user, so a second Obsidian for a worktree takes the first one's CLI away. Separate vaults were the alternative; they broke the other session's vault access when tried. Running a fleet of agents on one vault is not solved yet.