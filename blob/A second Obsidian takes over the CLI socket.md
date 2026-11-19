---
identity__uuid: da755583-c14d-44a9-8cef-82ede9d9321f
identity__type: particular
identity__created_at: 2026-11-19
identity__tags: []
relationships__is_a:
  - "[[Letter v1.0.0]]"
relationships__in_context_of:
  - "[[Issue #18 Event types ids and clock]]"
relationships__relates_to:
  - "[[Parallel runs keep code in separate worktrees and share one vault]]"
  - "[[Kit]]"
properties__author: agent:claude-opus-5-5
properties__priority: 3
properties__resolved: false
properties__type: thought
---
## Context

The Issue #18 run opened while the Issue #21 run was open, each in its own session. A second headless Obsidian on a git worktree replaced the first one's CLI socket and cut the other session off from its vault until it was restarted.

## Content

Several agents cannot yet work on one vault safely, or on several vaults on one machine:

- The Obsidian CLI connects through one socket per user, `$XDG_RUNTIME_DIR/.obsidian-cli.sock`. The newest Obsidian takes it over, whatever its config home.
- The khora plugin's queue answers every request in its `in/` folder on each command, so two Obsidians sharing a temp folder would answer each other's queries.
- One vault means one working tree, on one branch. Entities written for one run sit untracked in another run's checkout until committed, and must be kept out of its commits by hand.

This matters if the team runs a fleet of agents.